from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: str
    service: str


class ModelInfoResponse(BaseModel):
    model_name: str
    model_version: str
    initialization_status: str
    device_type: str
    cuda_available: bool
    supported_categories: list[str]
    output_resolution: str


class ModelStatusResponse(BaseModel):
    weights_available: bool
    initialized: bool
    inference_available: bool
    gpu_available: bool
    device_type: str
    initialization_status: str
    error: str | None = None
