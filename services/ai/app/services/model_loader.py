import logging
import os
from pathlib import Path
from threading import Lock

from app.core.config import Settings

logger = logging.getLogger("designer_costume_ai.model")


class ModelLoader:
    REQUIRED_WEIGHTS = (
        Path("model.safetensors"),
        Path("dwpose/yolox_l.onnx"),
        Path("dwpose/dw-ll_ucoco_384.onnx"),
    )

    def __init__(self, settings: Settings):
        self.settings = settings
        self.pipeline = None
        self.device_type = "unavailable"
        self.initialization_status = "disabled" if settings.model_use_mode == "disabled" else "not_initialized"
        self.error: str | None = (
            "Inference is disabled by configuration."
            if settings.model_use_mode == "disabled"
            else None
        )
        self._lock = Lock()

    @property
    def weights_available(self) -> bool:
        return all((self.settings.model_weights_dir / relative).is_file()
                   for relative in self.REQUIRED_WEIGHTS)

    def cuda_available(self) -> bool:
        try:
            os.environ.setdefault("HF_HOME", str(self.settings.huggingface_home))
            import torch

            return bool(torch.cuda.is_available())
        except ImportError:
            return False

    def initialize(self) -> None:
        with self._lock:
            if self.pipeline is not None:
                return
            self.initialization_status = "loading"
            self.error = None
            if self.settings.model_use_mode != "research":
                self.initialization_status = "disabled"
                self.error = "Inference is disabled. MODEL_USE_MODE=research is required for evaluation."
                return
            if not self.settings.model_license_acknowledged:
                self.initialization_status = "error"
                self.error = (
                    "Research-only license acknowledgement is required; commercial use is not authorized."
                )
                return
            if not self.weights_available:
                self.initialization_status = "error"
                self.error = "Required model weights are not installed."
                return

            try:
                import torch

                if self.settings.model_device == "cuda" and not torch.cuda.is_available():
                    raise RuntimeError("CUDA was requested but is unavailable.")
                selected_device = self.settings.model_device
                if selected_device == "auto":
                    selected_device = "cuda" if torch.cuda.is_available() else "cpu"
                from fashn_vton import TryOnPipeline

                self.pipeline = TryOnPipeline(
                    weights_dir=str(self.settings.model_weights_dir),
                    device=None if selected_device == "auto" else selected_device,
                )
                self.device_type = str(self.pipeline.device.type)
                self.initialization_status = "ready"
                logger.info("FASHN VTON v%s initialized on %s", self.settings.model_version, self.device_type)
            except ImportError:
                self.pipeline = None
                self.initialization_status = "error"
                self.error = "Model dependencies are not installed."
                logger.exception("FASHN VTON dependencies are unavailable")
            except Exception as exception:
                self.pipeline = None
                self.initialization_status = "error"
                self.error = self._public_initialization_error(exception)
                logger.exception("FASHN VTON initialization failed")

    def _public_initialization_error(self, exception: Exception) -> str:
        try:
            import torch

            if isinstance(exception, torch.cuda.OutOfMemoryError):
                return "GPU memory is insufficient to initialize the model."
        except ImportError:
            pass
        if isinstance(exception, FileNotFoundError):
            return "Required model or parser weights are unavailable."
        if isinstance(exception, RuntimeError) and "CUDA was requested" in str(exception):
            return "CUDA was requested but is unavailable."
        return "Model initialization failed; check server logs and installed model dependencies."

    def info(self) -> dict:
        return {
            "model_name": self.settings.model_name,
            "model_version": self.settings.model_version,
            "initialization_status": self.initialization_status,
            "device_type": self.device_type,
            "cuda_available": self.cuda_available(),
            "supported_categories": list(self.settings.supported_categories),
            "output_resolution": f"{self.settings.output_width}x{self.settings.output_height}",
        }

    def status(self) -> dict:
        cuda = self.cuda_available()
        initialized = self.pipeline is not None
        return {
            "weights_available": self.weights_available,
            "initialized": initialized,
            "inference_available": initialized and self.settings.model_use_mode == "research",
            "gpu_available": cuda,
            "device_type": self.device_type,
            "initialization_status": self.initialization_status,
            "error": self.error,
        }

    def run(self, **kwargs):
        if self.pipeline is None:
            raise RuntimeError(self.error or "Model is not initialized.")
        return self.pipeline(**kwargs)

    def release_cuda_memory(self) -> None:
        try:
            import torch

            if torch.cuda.is_available():
                torch.cuda.empty_cache()
        except ImportError:
            return

    def reset_cuda_peak_memory(self) -> None:
        try:
            import torch

            if self.device_type == "cuda" and torch.cuda.is_available():
                torch.cuda.reset_peak_memory_stats()
        except ImportError:
            return

    def cuda_peak_memory_mib(self) -> float | None:
        try:
            import torch

            if self.device_type == "cuda" and torch.cuda.is_available():
                return torch.cuda.max_memory_allocated() / (1024 * 1024)
        except ImportError:
            return None
        return None
