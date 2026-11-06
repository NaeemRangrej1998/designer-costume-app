import asyncio
from io import BytesIO
from pathlib import Path

import pytest
from fastapi import UploadFile
from PIL import Image

from app.services.image_validation import ImageValidationError, load_validated_image, save_upload


def test_upload_requires_nonempty_content(tmp_path: Path):
    upload = UploadFile(filename="empty.png", file=BytesIO(b""))
    with pytest.raises(ImageValidationError, match="empty"):
        asyncio.run(save_upload(upload, tmp_path / "empty", 100))


def test_image_is_normalized_to_rgb_and_orientation_applied(tmp_path: Path):
    source_path = tmp_path / "sample.png"
    Image.new("RGBA", (12, 8), (255, 0, 0, 127)).save(source_path)
    image = load_validated_image(source_path, 20)
    try:
        assert image.mode == "RGB"
        assert image.size == (12, 8)
    finally:
        image.close()


def test_image_dimensions_are_bounded(tmp_path: Path):
    source_path = tmp_path / "sample.png"
    Image.new("RGB", (12, 8)).save(source_path)
    with pytest.raises(ImageValidationError, match="dimensions"):
        load_validated_image(source_path, 10)
