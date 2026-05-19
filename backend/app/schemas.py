from pydantic import BaseModel


class AnalysisResult(BaseModel):
    label: str  # "AI_GENERATED" | "REAL" | "UNCERTAIN"
    confidence: float | None = None  # 0.0 - 1.0
    explanation: str


class AnalyzeResponse(BaseModel):
    status: str  # "success" | "error"
    result: AnalysisResult | None = None
    error: str | None = None


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    device: str
    vllm_enabled: bool
