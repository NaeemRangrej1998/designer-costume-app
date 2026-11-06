import os
from dataclasses import dataclass
from pathlib import Path


def _positive_int(name: str, default: int) -> int:
    try:
        value = int(os.getenv(name, str(default)))
    except ValueError as exception:
        raise ValueError(f"{name} must be an integer") from exception
    if value <= 0:
        raise ValueError(f"{name} must be greater than zero")
    return value


def _positive_float(name: str, default: float) -> float:
    try:
        value = float(os.getenv(name, str(default)))
    except ValueError as exception:
        raise ValueError(f"{name} must be a number") from exception
    if value <= 0:
        raise ValueError(f"{name} must be greater than zero")
    return value


@dataclass(frozen=True)
class Settings:
    app_version: str
    api_token: str | None
    model_name: str
    model_version: str
    model_weights_dir: Path
    model_device: str
    model_num_timesteps: int
    model_guidance_scale: float
    model_seed: int
    model_max_concurrent_jobs: int
    model_use_mode: str
    model_license_acknowledged: bool
    max_upload_bytes: int
    max_image_dimension: int
    temp_root: Path | None
    huggingface_home: Path
    supported_categories: tuple[str, ...] = ("tops", "bottoms", "one-pieces")
    output_width: int = 576
    output_height: int = 864


def get_settings() -> Settings:
    use_mode = os.getenv("MODEL_USE_MODE", "disabled").strip().lower()
    if use_mode not in {"disabled", "research"}:
        raise ValueError("MODEL_USE_MODE must be disabled or research")
    device = os.getenv("MODEL_DEVICE", "auto").strip().lower()
    if device not in {"auto", "cpu", "cuda"}:
        raise ValueError("MODEL_DEVICE must be auto, cpu, or cuda")

    weights_dir = Path(os.getenv("MODEL_WEIGHTS_DIR", "./weights")).expanduser().resolve()
    huggingface_home = Path(
        os.getenv("HF_HOME", str(Path.home() / ".cache" / "huggingface"))
    ).expanduser().resolve()
    temp_value = os.getenv("AI_TEMP_DIR", "").strip()
    temp_root = Path(temp_value).expanduser().resolve() if temp_value else None
    seed = int(os.getenv("MODEL_SEED", "42"))
    if seed < 0 or seed > 2_147_483_647:
        raise ValueError("MODEL_SEED must be between 0 and 2147483647")

    return Settings(
        app_version=os.getenv("AI_APP_VERSION", "0.2.0"),
        api_token=os.getenv("AI_SERVICE_API_TOKEN") or None,
        model_name=os.getenv("MODEL_NAME", "fashn-vton-1.5"),
        model_version=os.getenv("MODEL_VERSION", "1.5.0"),
        model_weights_dir=weights_dir,
        model_device=device,
        model_num_timesteps=_positive_int("MODEL_NUM_TIMESTEPS", 30),
        model_guidance_scale=_positive_float("MODEL_GUIDANCE_SCALE", 1.5),
        model_seed=seed,
        model_max_concurrent_jobs=_positive_int("MODEL_MAX_CONCURRENT_JOBS", 1),
        model_use_mode=use_mode,
        model_license_acknowledged=os.getenv(
            "MODEL_LICENSE_ACKNOWLEDGED", "false"
        ).strip().lower() in {"1", "true", "yes"},
        max_upload_bytes=_positive_int("AI_MAX_UPLOAD_BYTES", 10 * 1024 * 1024),
        max_image_dimension=_positive_int("AI_MAX_IMAGE_DIMENSION", 4096),
        temp_root=temp_root,
        huggingface_home=huggingface_home,
    )
