import os

MODEL_PATH = os.environ.get("FAKEVLM_MODEL_PATH", "./FakeVLM/fakeVLM_model")
DEVICE = os.environ.get("FAKEVLM_DEVICE", "cuda")
USE_4BIT = os.environ.get("FAKEVLM_USE_4BIT", "true").lower() == "true"
USE_VLLM = os.environ.get("FAKEVLM_USE_VLLM", "false").lower() == "true"
VLLM_HOST = os.environ.get("FAKEVLM_VLLM_HOST", "http://localhost:8000")

MAX_IMAGE_SIZE_MB = int(os.environ.get("MAX_IMAGE_SIZE_MB", "10"))
MAX_NEW_TOKENS = int(os.environ.get("MAX_NEW_TOKENS", "256"))
INFERENCE_TIMEOUT = int(os.environ.get("INFERENCE_TIMEOUT", "60"))

HOST = os.environ.get("HOST", "0.0.0.0")
PORT = int(os.environ.get("PORT", "8080"))
