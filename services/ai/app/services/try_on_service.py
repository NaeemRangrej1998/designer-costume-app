import io
import logging
import tempfile
from contextlib import contextmanager
from pathlib import Path
from time import perf_counter

from PIL import Image

from app.core.config import Settings
from app.services.model_loader import ModelLoader

logger = logging.getLogger("designer_costume_ai.inference")


class ModelInferenceError(Exception):
    def __init__(self, public_message: str, code: str, status_code: int = 503):
        super().__init__(public_message)
        self.public_message = public_message
        self.code = code
        self.status_code = status_code


class TryOnService:
    def __init__(self, loader: ModelLoader, settings: Settings):
        self.loader = loader
        self.settings = settings

    @contextmanager
    def temporary_directory(self):
        if self.settings.temp_root is not None:
            self.settings.temp_root.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(
            prefix="designer-vton-",
            dir=str(self.settings.temp_root) if self.settings.temp_root else None,
        ) as directory:
            yield Path(directory)

    def run(
        self,
        person_image: Image.Image,
        garment_image: Image.Image,
        category: str,
        garment_photo_type: str,
        num_timesteps: int,
        guidance_scale: float,
        seed: int,
    ):
        if not self.loader.status()["inference_available"]:
            raise ModelInferenceError(
                self.loader.error or "The model is not initialized.",
                "model_unavailable",
            )
        if category not in self.settings.supported_categories:
            raise ModelInferenceError("Unsupported garment category.", "invalid_category", 422)

        self.loader.reset_cuda_peak_memory()
        started = perf_counter()
        try:
            output = self.loader.run(
                person_image=person_image,
                garment_image=garment_image,
                category=category,
                garment_photo_type=garment_photo_type,
                num_timesteps=num_timesteps,
                guidance_scale=guidance_scale,
                seed=seed,
            )
            if not output.images or not isinstance(output.images[0], Image.Image):
                raise ModelInferenceError("The model did not return a valid image.", "invalid_model_output")
            result = output.images[0].convert("RGB")
            duration = perf_counter() - started
            peak_memory = self.loader.cuda_peak_memory_mib()
            logger.info(
                "Inference completed in %.3f seconds (device=%s, cuda_peak_memory_mib=%s)",
                duration,
                self.loader.device_type,
                f"{peak_memory:.1f}" if peak_memory is not None else "unavailable",
            )
            return InferenceResult(result, duration, peak_memory)
        except ModelInferenceError:
            raise
        except Exception as exception:
            if self._is_cuda_oom(exception):
                self.loader.release_cuda_memory()
                raise ModelInferenceError(
                    "GPU memory is insufficient for this inference. Reduce input dimensions or use a larger GPU.",
                    "cuda_out_of_memory",
                    507,
                ) from exception
            raise ModelInferenceError(
                "Virtual try-on inference failed. Check service logs for details.",
                "inference_failed",
                500,
            ) from exception

    def _is_cuda_oom(self, exception: Exception) -> bool:
        try:
            import torch

            return isinstance(exception, torch.cuda.OutOfMemoryError) or (
                isinstance(exception, RuntimeError) and "out of memory" in str(exception).lower()
            )
        except ImportError:
            return isinstance(exception, RuntimeError) and "out of memory" in str(exception).lower()

    @staticmethod
    def encode_png(image: Image.Image) -> bytes:
        output = io.BytesIO()
        image.save(output, format="PNG")
        return output.getvalue()


class InferenceResult:
    def __init__(self, image: Image.Image, duration_seconds: float, cuda_peak_memory_mib: float | None):
        self.image = image
        self.duration_seconds = duration_seconds
        self.cuda_peak_memory_mib = cuda_peak_memory_mib
