from pathlib import Path

import pytest
from PIL import Image

from app.core.config import Settings
from app.services.model_loader import ModelLoader
from app.services.try_on_service import ModelInferenceError, TryOnService


class FailingTestLoader(ModelLoader):
    def __init__(self, settings):
        super().__init__(settings)
        self.pipeline = object()
        self.device_type = "cpu"
        self.initialization_status = "ready"

    def status(self):
        return {"inference_available": True}

    def run(self, **kwargs):
        raise RuntimeError("internal implementation path and stack details")


def test_inference_failure_returns_safe_error_without_paths(tmp_path: Path):
    settings = Settings(
        app_version="test",
        api_token="test",
        model_name="fashn-vton-1.5",
        model_version="1.5.0",
        model_weights_dir=tmp_path / "weights",
        model_device="cpu",
        model_num_timesteps=30,
        model_guidance_scale=1.5,
        model_seed=42,
        model_max_concurrent_jobs=1,
        model_use_mode="research",
        model_license_acknowledged=True,
        max_upload_bytes=1024,
        max_image_dimension=100,
        temp_root=tmp_path / "tmp",
        huggingface_home=tmp_path / "hf",
    )
    service = TryOnService(FailingTestLoader(settings), settings)
    image = Image.new("RGB", (4, 4))
    with pytest.raises(ModelInferenceError) as error:
        service.run(image, image, "tops", "model", 30, 1.5, 42)
    assert error.value.status_code == 500
    assert error.value.public_message == "Virtual try-on inference failed. Check service logs for details."
    assert str(tmp_path) not in error.value.public_message
    image.close()


def test_cuda_out_of_memory_is_mapped_to_507(tmp_path: Path):
    settings = Settings(
        app_version="test",
        api_token="test",
        model_name="fashn-vton-1.5",
        model_version="1.5.0",
        model_weights_dir=tmp_path / "weights",
        model_device="cpu",
        model_num_timesteps=30,
        model_guidance_scale=1.5,
        model_seed=42,
        model_max_concurrent_jobs=1,
        model_use_mode="research",
        model_license_acknowledged=True,
        max_upload_bytes=1024,
        max_image_dimension=100,
        temp_root=tmp_path / "tmp",
        huggingface_home=tmp_path / "hf",
    )

    class OutOfMemoryLoader(FailingTestLoader):
        def run(self, **kwargs):
            raise RuntimeError("CUDA out of memory")

    image = Image.new("RGB", (4, 4))
    with pytest.raises(ModelInferenceError) as error:
        TryOnService(OutOfMemoryLoader(settings), settings).run(
            image, image, "tops", "model", 30, 1.5, 42
        )
    assert error.value.status_code == 507
    assert error.value.code == "cuda_out_of_memory"
    image.close()
