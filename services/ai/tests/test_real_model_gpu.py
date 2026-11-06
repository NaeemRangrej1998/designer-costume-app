import os
from pathlib import Path

import pytest
from PIL import Image

from app.core.config import get_settings
from app.services.model_loader import ModelLoader
from app.services.try_on_service import TryOnService

pytestmark = pytest.mark.gpu


def test_real_fashn_inference_on_gpu():
    if os.getenv("RUN_REAL_MODEL_TESTS") != "true":
        pytest.skip("Set RUN_REAL_MODEL_TESTS=true for explicit real-model inference.")
    person_path = os.getenv("PERSON_SAMPLE_PATH")
    garment_path = os.getenv("GARMENT_SAMPLE_PATH")
    if not person_path or not garment_path:
        pytest.skip("Set PERSON_SAMPLE_PATH and GARMENT_SAMPLE_PATH to consented test images.")
    try:
        import torch
    except ImportError:
        pytest.skip("PyTorch is not installed.")
    if not torch.cuda.is_available():
        pytest.skip("CUDA GPU is not available.")

    settings = get_settings()
    loader = ModelLoader(settings)
    if not loader.weights_available:
        pytest.skip("FASHN VTON and DWPose weights are not installed.")
    loader.initialize()
    assert loader.status()["inference_available"], loader.status()["error"]
    service = TryOnService(loader, settings)
    with Image.open(Path(person_path)) as source:
        person = source.convert("RGB")
    with Image.open(Path(garment_path)) as source:
        garment = source.convert("RGB")
    try:
        result = service.run(
            person, garment, "tops", "flat-lay",
            settings.model_num_timesteps, settings.model_guidance_scale, settings.model_seed,
        )
        assert result.image.size == (settings.output_width, settings.output_height)
        assert result.duration_seconds > 0
        assert result.cuda_peak_memory_mib is not None
        result.image.close()
    finally:
        person.close()
        garment.close()
