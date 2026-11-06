from io import BytesIO
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from app.core.config import Settings
from app.main import create_app
from app.services.model_loader import ModelLoader

TOKEN = "test-internal-token"


def settings(tmp_path: Path, **overrides) -> Settings:
    values = {
        "app_version": "test",
        "api_token": TOKEN,
        "model_name": "fashn-vton-1.5",
        "model_version": "1.5.0",
        "model_weights_dir": tmp_path / "weights",
        "model_device": "auto",
        "model_num_timesteps": 30,
        "model_guidance_scale": 1.5,
        "model_seed": 42,
        "model_max_concurrent_jobs": 1,
        "model_use_mode": "disabled",
        "model_license_acknowledged": False,
        "max_upload_bytes": 1024 * 1024,
        "max_image_dimension": 12000,
        "temp_root": tmp_path / "tmp",
        "huggingface_home": tmp_path / "hf",
    }
    values.update(overrides)
    return Settings(**values)


def encoded_image(format_name="PNG", size=(16, 20)) -> bytes:
    stream = BytesIO()
    Image.new("RGB", size, (80, 120, 160)).save(stream, format=format_name)
    return stream.getvalue()


def auth_headers():
    return {"Authorization": f"Bearer {TOKEN}"}


def test_health_endpoint_does_not_require_internal_token(tmp_path):
    with TestClient(create_app(settings(tmp_path))) as client:
        response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {
        "status": "UP",
        "service": "Virtual Try-On AI Service",
    }


def test_model_status_and_info_require_internal_bearer_token(tmp_path):
    with TestClient(create_app(settings(tmp_path))) as client:
        assert client.get("/api/v1/model/status").status_code == 401
        assert client.get("/api/v1/model/info", headers=auth_headers()).status_code == 200
        response = client.get("/api/v1/model/status", headers=auth_headers())
    assert response.status_code == 200
    assert response.json()["inference_available"] is False
    assert response.json()["weights_available"] is False
    assert str(tmp_path) not in response.text


def test_missing_api_token_disables_internal_endpoints(tmp_path):
    with TestClient(create_app(settings(tmp_path, api_token=None))) as client:
        response = client.get("/api/v1/model/status")
    assert response.status_code == 503


def test_unsupported_category_is_rejected_without_running_model(tmp_path):
    files = {
        "person_image": ("person.png", encoded_image(), "image/png"),
        "garment_image": ("garment.png", encoded_image(), "image/png"),
    }
    with TestClient(create_app(settings(tmp_path))) as client:
        response = client.post(
            "/api/v1/try-on",
            headers=auth_headers(),
            files=files,
            data={"category": "sarees"},
        )
    assert response.status_code == 422
    assert "tops, bottoms, or one-pieces" in response.json()["detail"]


def test_missing_person_image_is_rejected(tmp_path):
    with TestClient(create_app(settings(tmp_path))) as client:
        response = client.post(
            "/api/v1/try-on",
            headers=auth_headers(),
            files={"garment_image": ("garment.png", encoded_image(), "image/png")},
            data={"category": "tops"},
        )
    assert response.status_code == 422


@pytest.mark.parametrize("content", [b"", b"not an image"])
def test_empty_and_corrupted_images_are_rejected(tmp_path, content):
    files = {
        "person_image": ("person.png", encoded_image(), "image/png"),
        "garment_image": ("garment.png", content, "image/png"),
    }
    with TestClient(create_app(settings(tmp_path))) as client:
        response = client.post(
            "/api/v1/try-on",
            headers=auth_headers(),
            files=files,
            data={"category": "tops"},
        )
    assert response.status_code == 422
    assert "detail" in response.json()


def test_unsupported_image_format_is_rejected(tmp_path):
    files = {
        "person_image": ("person.png", encoded_image(), "image/png"),
        "garment_image": ("garment.png", encoded_image("BMP"), "image/png"),
    }
    with TestClient(create_app(settings(tmp_path))) as client:
        response = client.post(
            "/api/v1/try-on",
            headers=auth_headers(),
            files=files,
            data={"category": "tops"},
        )
    assert response.status_code == 422
    assert "JPEG, PNG, and WebP" in response.json()["detail"]


def test_oversized_upload_is_rejected_and_temporary_files_are_removed(tmp_path):
    config = settings(tmp_path, max_upload_bytes=128)
    files = {
        "person_image": ("person.png", encoded_image(), "image/png"),
        "garment_image": ("large.png", encoded_image(size=(100, 100)), "image/png"),
    }
    with TestClient(create_app(config)) as client:
        response = client.post(
            "/api/v1/try-on",
            headers=auth_headers(),
            files=files,
            data={"category": "tops"},
        )
    assert response.status_code == 413
    assert list((tmp_path / "tmp").iterdir()) == []


def test_model_initialization_failure_is_safe_and_does_not_expose_paths(tmp_path):
    config = settings(
        tmp_path, model_use_mode="research", model_license_acknowledged=True
    )
    with TestClient(create_app(config, ModelLoader(config))) as client:
        response = client.get("/api/v1/model/status", headers=auth_headers())
    assert response.status_code == 200
    assert response.json()["initialization_status"] == "error"
    assert response.json()["error"] == "Required model weights are not installed."
    assert str(tmp_path) not in response.text


def test_research_mode_requires_license_acknowledgement(tmp_path):
    config = settings(tmp_path, model_use_mode="research")
    with TestClient(create_app(config, ModelLoader(config))) as client:
        response = client.get("/api/v1/model/status", headers=auth_headers())
    assert response.status_code == 200
    assert response.json()["inference_available"] is False
    assert "commercial use is not authorized" in response.json()["error"]


def test_mock_pipeline_contract_returns_png_without_real_model_weights(tmp_path):
    class MockPipelineLoader(ModelLoader):
        def initialize(self):
            self.pipeline = object()
            self.device_type = "cpu"
            self.initialization_status = "ready"

        def run(self, **kwargs):
            return type("Output", (), {"images": [Image.new("RGB", (576, 864), "purple")]})()

    config = settings(
        tmp_path, model_use_mode="research", model_license_acknowledged=True
    )
    files = {
        "person_image": ("person.png", encoded_image(), "image/png"),
        "garment_image": ("garment.png", encoded_image(), "image/png"),
    }
    with TestClient(create_app(config, MockPipelineLoader(config))) as client:
        response = client.post(
            "/api/v1/try-on",
            headers=auth_headers(),
            files=files,
            data={"category": "tops"},
        )
    assert response.status_code == 200
    assert response.headers["content-type"] == "image/png"
    assert response.headers["x-inference-duration-seconds"]
    output = Image.open(BytesIO(response.content))
    try:
        assert output.size == (576, 864)
    finally:
        output.close()
    assert list((tmp_path / "tmp").iterdir()) == []
