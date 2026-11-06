import asyncio
import logging
import secrets
from contextlib import asynccontextmanager
from time import perf_counter

from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, Request, UploadFile
from fastapi.responses import Response
from starlette.concurrency import run_in_threadpool

from app.core.config import Settings, get_settings
from app.schemas.try_on import HealthResponse, ModelInfoResponse, ModelStatusResponse
from app.services.image_validation import ImageValidationError, load_validated_image, save_upload
from app.services.model_loader import ModelLoader
from app.services.try_on_service import ModelInferenceError, TryOnService

logger = logging.getLogger("designer_costume_ai")


def create_app(settings: Settings | None = None, model_loader: ModelLoader | None = None) -> FastAPI:
    config = settings or get_settings()
    loader = model_loader or ModelLoader(config)
    inference = TryOnService(loader, config)
    semaphore = asyncio.Semaphore(config.model_max_concurrent_jobs)

    @asynccontextmanager
    async def lifespan(_: FastAPI):
        if config.model_use_mode == "research":
            try:
                await run_in_threadpool(loader.initialize)
            except Exception:
                logger.exception("FASHN VTON research-mode initialization failed")
        yield

    application = FastAPI(
        title="Virtual Try-On AI Service",
        description="Internal FASHN VTON v1.5 research/evaluation service.",
        version=config.app_version,
        lifespan=lifespan,
    )
    application.state.settings = config
    application.state.model_loader = loader
    application.state.try_on_service = inference
    application.state.inference_semaphore = semaphore

    async def require_internal_token(
        request: Request,
        authorization: str | None = Header(default=None),
    ):
        expected = request.app.state.settings.api_token
        if not expected:
            raise HTTPException(status_code=503, detail="Internal AI API is not configured")
        if not authorization or not authorization.startswith("Bearer "):
            raise HTTPException(status_code=401, detail="Internal bearer token required")
        supplied = authorization.removeprefix("Bearer ").strip()
        if not secrets.compare_digest(supplied, expected):
            raise HTTPException(status_code=401, detail="Invalid internal bearer token")

    @application.exception_handler(ImageValidationError)
    async def image_validation_error_handler(_: Request, exception: ImageValidationError):
        return Response(
            content='{"detail":"' + exception.public_message.replace('"', '\\"') + '"}',
            status_code=exception.status_code,
            media_type="application/json",
        )

    @application.exception_handler(ModelInferenceError)
    async def inference_error_handler(_: Request, exception: ModelInferenceError):
        logger.error("Try-on inference failed: %s", exception.code)
        return Response(
            content='{"detail":"' + exception.public_message.replace('"', '\\"') + '"}',
            status_code=exception.status_code,
            media_type="application/json",
        )

    @application.exception_handler(Exception)
    async def unexpected_error_handler(_: Request, exception: Exception):
        logger.exception("Unexpected AI service error")
        return Response(
            content='{"detail":"AI service request failed"}',
            status_code=500,
            media_type="application/json",
        )

    @application.get("/health", response_model=HealthResponse)
    async def get_health():
        return HealthResponse(
            status="UP",
            service="Virtual Try-On AI Service",
        )

    @application.get(
        "/api/v1/model/info",
        response_model=ModelInfoResponse,
        dependencies=[Depends(require_internal_token)],
    )
    async def model_info():
        return loader.info()

    @application.get(
        "/api/v1/model/status",
        response_model=ModelStatusResponse,
        dependencies=[Depends(require_internal_token)],
    )
    async def model_status():
        return loader.status()

    @application.post(
        "/api/v1/try-on",
        dependencies=[Depends(require_internal_token)],
        responses={
            200: {"content": {"image/png": {}}},
            413: {"description": "Image exceeds configured file size"},
            422: {"description": "Invalid image or category"},
            503: {"description": "Model is unavailable"},
            507: {"description": "GPU memory exhausted"},
        },
    )
    async def try_on(
        person_image: UploadFile = File(...),
        garment_image: UploadFile = File(...),
        category: str = Form(...),
        garment_photo_type: str = Form(default="model"),
        num_timesteps: int = Form(default=config.model_num_timesteps, ge=1, le=50),
        seed: int = Form(default=config.model_seed, ge=0, le=2_147_483_647),
    ):
        if category not in config.supported_categories:
            raise HTTPException(
                status_code=422,
                detail="Unsupported category. Use tops, bottoms, or one-pieces.",
            )
        if garment_photo_type not in {"model", "flat-lay"}:
            raise HTTPException(
                status_code=422,
                detail="Unsupported garment_photo_type. Use model or flat-lay.",
            )

        async with semaphore:
            with inference.temporary_directory() as temporary_directory:
                person = None
                garment = None
                try:
                    person_path = await save_upload(
                        person_image, temporary_directory / "person", config.max_upload_bytes
                    )
                    garment_path = await save_upload(
                        garment_image, temporary_directory / "garment", config.max_upload_bytes
                    )
                    person = load_validated_image(person_path, config.max_image_dimension)
                    garment = load_validated_image(garment_path, config.max_image_dimension)
                    started = perf_counter()
                    result = await run_in_threadpool(
                        inference.run,
                        person,
                        garment,
                        category,
                        garment_photo_type,
                        num_timesteps,
                        config.model_guidance_scale,
                        seed,
                    )
                    duration = perf_counter() - started
                    try:
                        output = await run_in_threadpool(inference.encode_png, result.image)
                    finally:
                        result.image.close()
                    headers = {"X-Inference-Duration-Seconds": f"{duration:.3f}"}
                    if result.cuda_peak_memory_mib is not None:
                        headers["X-CUDA-Peak-Memory-MiB"] = f"{result.cuda_peak_memory_mib:.1f}"
                    return Response(content=output, media_type="image/png", headers=headers)
                except ImageValidationError as exception:
                    raise HTTPException(
                        status_code=exception.status_code, detail=exception.public_message
                    ) from exception
                finally:
                    if person is not None:
                        person.close()
                    if garment is not None:
                        garment.close()
                    await person_image.close()
                    await garment_image.close()

    return application


app = create_app()
