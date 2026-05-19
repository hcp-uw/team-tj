import io
import logging
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image

from app.config import MAX_IMAGE_SIZE_MB, HOST, PORT
from app.schemas import AnalyzeResponse, AnalysisResult, HealthResponse
from app.model_handler import ModelHandler

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

app = FastAPI(title="VerifAI", description="AI-generated image detection API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

model_handler = ModelHandler()

SUPPORTED_FORMATS = {"JPEG", "PNG", "WEBP", "BMP", "GIF", "TIFF"}


@app.on_event("startup")
def startup():
    model_handler.load()


@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze_image(file: UploadFile = File(...)):
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File must be an image")

    contents = await file.read()
    if len(contents) > MAX_IMAGE_SIZE_MB * 1024 * 1024:
        raise HTTPException(
            status_code=413,
            detail=f"Image exceeds {MAX_IMAGE_SIZE_MB} MB limit",
        )

    try:
        image = Image.open(io.BytesIO(contents))
        fmt = image.format
        if fmt and fmt.upper() not in SUPPORTED_FORMATS:
            raise HTTPException(
                status_code=400,
                detail=f"Unsupported format: {fmt}. Supported: {', '.join(sorted(SUPPORTED_FORMATS))}",
            )
        image = image.convert("RGB")
    except Exception as e:
        if isinstance(e, HTTPException):
            raise
        raise HTTPException(status_code=400, detail=f"Invalid or corrupt image: {e}")

    if not model_handler.is_loaded:
        raise HTTPException(status_code=503, detail="Model not available")

    try:
        result = model_handler.infer(image)
    except Exception as e:
        logger.exception("Inference failed")
        raise HTTPException(status_code=500, detail=f"Inference error: {e}")

    return AnalyzeResponse(status="success", result=result)


@app.get("/health", response_model=HealthResponse)
def health():
    return HealthResponse(
        status="ok",
        model_loaded=model_handler.is_loaded,
        device="vllm" if model_handler._check_vllm_health() else "cuda",
        vllm_enabled=model_handler._check_vllm_health(),
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=HOST, port=PORT, reload=False)
