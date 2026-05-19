import re
import json
import logging
import io
import torch
from PIL import Image
from app.config import (
    MODEL_PATH, DEVICE, USE_4BIT, USE_VLLM, VLLM_HOST,
    MAX_NEW_TOKENS, INFERENCE_TIMEOUT,
)
from app.schemas import AnalysisResult

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = (
    "You are an expert AI-generated image detector. Analyze the given image carefully. "
    "Determine whether it is AI-generated or a real photograph.\n"
    "Respond ONLY in this exact JSON format, with no other text:\n"
    '{"label": "<AI_GENERATED or REAL>", "confidence": <0-100 integer>, '
    '"explanation": "<one sentence describing specific visual artifacts or '
    'natural features that support your conclusion>"}'
)


class ModelHandler:
    def __init__(self):
        self._model = None
        self._processor = None
        self._loaded = False

    @property
    def is_loaded(self) -> bool:
        if USE_VLLM:
            return self._check_vllm_health()
        return self._loaded

    def load(self):
        if USE_VLLM:
            logger.info("vLLM mode enabled — skipping local model load")
            return

        logger.info("Loading FakeVLM model from %s (4-bit=%s)...", MODEL_PATH, USE_4BIT)
        from transformers import LlavaForConditionalGeneration, AutoProcessor

        load_kwargs = {
            "torch_dtype": torch.float16,
            "device_map": "auto",
        }
        if USE_4BIT:
            load_kwargs["load_in_4bit"] = True

        self._model = LlavaForConditionalGeneration.from_pretrained(
            MODEL_PATH, **load_kwargs
        )
        self._processor = AutoProcessor.from_pretrained(MODEL_PATH)
        self._loaded = True
        logger.info("FakeVLM model loaded successfully on %s", DEVICE)

    def infer(self, image: Image.Image) -> AnalysisResult:
        if USE_VLLM:
            return self._infer_vllm(image)
        return self._infer_local(image)

    def _infer_local(self, image: Image.Image) -> AnalysisResult:
        prompt = f"<image>\n{SYSTEM_PROMPT}"
        inputs = self._processor(
            text=prompt, images=image, return_tensors="pt"
        ).to(DEVICE)

        with torch.no_grad():
            output = self._model.generate(
                **inputs,
                max_new_tokens=MAX_NEW_TOKENS,
                do_sample=False,
            )

        raw_text = self._processor.decode(output[0], skip_special_tokens=True)
        # Strip the prompt echo — keep only the generated part
        if "<image>" in raw_text:
            raw_text = raw_text.split("<image>")[-1].strip()
        # The prompt itself may appear; remove it
        clean_prompt = SYSTEM_PROMPT.strip()
        if clean_prompt in raw_text:
            raw_text = raw_text.split(clean_prompt)[-1].strip()

        logger.info("Raw model output: %s", raw_text)
        return _parse_response(raw_text)

    def _infer_vllm(self, image: Image.Image) -> AnalysisResult:
        import base64
        import requests

        buf = io.BytesIO()
        image.save(buf, format="PNG")
        img_b64 = base64.b64encode(buf.getvalue()).decode()

        payload = {
            "model": MODEL_PATH,
            "prompt": f"<image>\n{SYSTEM_PROMPT}",
            "max_tokens": MAX_NEW_TOKENS,
            "temperature": 0.0,
            "images": [img_b64],
        }

        resp = requests.post(
            f"{VLLM_HOST}/v1/completions",
            json=payload,
            timeout=INFERENCE_TIMEOUT,
        )
        resp.raise_for_status()
        data = resp.json()
        raw_text = data["choices"][0]["text"]
        logger.info("vLLM output: %s", raw_text)
        return _parse_response(raw_text)

    def _check_vllm_health(self) -> bool:
        try:
            import requests
            r = requests.get(f"{VLLM_HOST}/health", timeout=5)
            return r.status_code == 200
        except Exception:
            return False


def _extract_json_objects(text: str) -> list[str]:
    """Extract all top-level JSON objects with balanced braces."""
    results = []
    depth = 0
    start = -1
    for i, ch in enumerate(text):
        if ch == "{":
            if depth == 0:
                start = i
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0 and start >= 0:
                results.append(text[start : i + 1])
    return results


def _parse_response(raw_text: str) -> AnalysisResult:
    # Strategy 1: Extract JSON objects with balanced braces (handles braces in explanations)
    for candidate in _extract_json_objects(raw_text):
        try:
            data = json.loads(candidate)
            if "label" in data:
                label = _normalize_label(data.get("label", ""))
                confidence = _normalize_confidence(data.get("confidence"))
                explanation = data.get("explanation", raw_text)
                return AnalysisResult(
                    label=label, confidence=confidence, explanation=explanation
                )
        except (json.JSONDecodeError, KeyError):
            continue

    # Strategy 2: Regex heuristic extraction
    label = "UNCERTAIN"
    if re.search(r"\bAI[_\s-]?GENERATED\b", raw_text, re.IGNORECASE):
        label = "AI_GENERATED"
    elif re.search(r"\bREAL\b", raw_text, re.IGNORECASE):
        label = "REAL"
    elif re.search(r"\bfake\b", raw_text, re.IGNORECASE):
        label = "AI_GENERATED"
    elif re.search(r"\bauthentic\b|\bgenuine\b", raw_text, re.IGNORECASE):
        label = "REAL"

    confidence = None
    conf_match = re.search(r'confidence["\s:]+(\d+)', raw_text, re.IGNORECASE)
    if conf_match:
        confidence = int(conf_match.group(1)) / 100.0

    explanation = raw_text.strip()
    return AnalysisResult(label=label, confidence=confidence, explanation=explanation)


def _normalize_label(label_str: str) -> str:
    s = label_str.strip().upper()
    if "AI" in s or "GENERATED" in s or "FAKE" in s:
        return "AI_GENERATED"
    if "REAL" in s or "AUTHENTIC" in s or "GENUINE" in s:
        return "REAL"
    return "UNCERTAIN"


def _normalize_confidence(val) -> float | None:
    if val is None:
        return None
    try:
        c = float(val)
        # If it looks like 0-100 scale, normalize to 0-1
        if c > 1:
            c = c / 100.0
        return round(max(0.0, min(1.0, c)), 4)
    except (ValueError, TypeError):
        return None
