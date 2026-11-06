import warnings
from pathlib import Path

from fastapi import UploadFile
from PIL import Image, ImageOps, UnidentifiedImageError

ALLOWED_FORMATS = {"JPEG", "PNG", "WEBP"}
CHUNK_SIZE = 1024 * 1024


class ImageValidationError(Exception):
    def __init__(self, public_message: str, status_code: int = 422):
        super().__init__(public_message)
        self.public_message = public_message
        self.status_code = status_code


async def save_upload(upload: UploadFile, destination: Path, max_bytes: int) -> Path:
    total = 0
    try:
        with destination.open("wb") as target:
            while chunk := await upload.read(CHUNK_SIZE):
                total += len(chunk)
                if total > max_bytes:
                    raise ImageValidationError("Image exceeds the maximum allowed size.", 413)
                target.write(chunk)
    except OSError as exception:
        raise ImageValidationError("Could not read uploaded image.") from exception
    if total == 0:
        destination.unlink(missing_ok=True)
        raise ImageValidationError("Uploaded image is empty.")
    return destination


def load_validated_image(path: Path, max_dimension: int) -> Image.Image:
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(path) as source:
                image_format = source.format
                width, height = source.size
                if image_format not in ALLOWED_FORMATS:
                    raise ImageValidationError("Only JPEG, PNG, and WebP images are supported.")
                if width <= 0 or height <= 0 or max(width, height) > max_dimension:
                    raise ImageValidationError("Image dimensions are invalid or exceed the configured limit.")
                source.verify()
            with Image.open(path) as source:
                source.load()
                oriented = ImageOps.exif_transpose(source)
                return oriented.convert("RGB")
    except ImageValidationError:
        raise
    except (UnidentifiedImageError, OSError, ValueError, Image.DecompressionBombError,
            Image.DecompressionBombWarning) as exception:
        raise ImageValidationError("Image is corrupted or cannot be decoded.") from exception
