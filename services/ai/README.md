# Virtual Try-On AI Service

This FastAPI service is the Phase 9 foundation for evaluating FASHN VTON v1.5. It is separate from the Java backend. It does not download model weights during normal startup, and inference is disabled unless explicitly enabled.

## Important licensing and suitability notice

The FASHN VTON v1.5 model repository is Apache-2.0, but its official inference pipeline also uses FASHN Human Parser. The parser's repository points to NVIDIA's SegFormer license, which restricts use to non-commercial research and evaluation. The DWPose and YOLOX components are documented as Apache-2.0.

Consequently, this integration is **not cleared for the boutique's commercial production use**. The license acknowledgement switch is only a guard against accidental initialization; it is not legal permission. Do not enable this model for customer-facing or commercial inference unless all component licenses have been reviewed and the use is permitted. Consider a commercially licensed alternative if production use is intended.

The upstream model documentation lists `tops`, `bottoms`, and `one-pieces`, accepts worn-model or flat-lay garment images, and generates 576x864 images. Those broad categories do not establish reliable support for Indian garments such as sarees, lehengas, dupattas, or regional drapes. Such garments need dedicated quality, safety, and cultural suitability evaluation; no accuracy claim is made here.

Official references:

- [FASHN VTON v1.5 repository](https://github.com/fashn-AI/fashn-vton-1.5)
- [FASHN VTON v1.5 model card](https://huggingface.co/fashn-ai/fashn-vton-1.5)
- [FASHN Human Parser license notice](https://github.com/fashn-AI/fashn-human-parser#license)
- [NVIDIA SegFormer license](https://github.com/NVlabs/SegFormer/blob/master/LICENSE)

## Local setup

Use Python 3.11 or newer. From `services/ai`:

```bash
python3.11 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
cp .env.example .env
```

Set `AI_SERVICE_API_TOKEN` to a long, unique secret in the process environment (or in an untracked local `.env` loaded by your chosen runner). Never commit the secret. The service does not automatically load `.env` files.

Start the API on loopback:

```bash
uvicorn app.main:app --host 127.0.0.1 --port 8001
```

Check `GET http://127.0.0.1:8001/health`. Keep the service on loopback or a trusted private network; do not expose it directly to the public internet.

## Configuration and model loading

The defaults in `.env.example` keep inference disabled. The API token is required for every `/api/v1/*` endpoint. Set the required environment variables in the service process:

| Variable | Purpose |
|---|---|
| `AI_SERVICE_API_TOKEN` | Required bearer token for internal model and inference routes |
| `MODEL_USE_MODE` | `disabled` by default; `research` permits evaluation initialization only |
| `MODEL_LICENSE_ACKNOWLEDGED` | Must explicitly be `true` before research-mode initialization |
| `MODEL_WEIGHTS_DIR` | Existing local folder containing model and DWPose weights |
| `MODEL_DEVICE` | `auto`, `cpu`, or `cuda` |
| `MODEL_NUM_TIMESTEPS` | Default inference steps (default 30) |
| `MODEL_GUIDANCE_SCALE` | Guidance setting (default 1.5) |
| `MODEL_SEED` | Default reproducible seed |
| `MODEL_MAX_CONCURRENT_JOBS` | Simultaneous inference limit (default 1) |
| `AI_MAX_UPLOAD_BYTES` | Maximum size of each image (default 10 MiB) |
| `AI_MAX_IMAGE_DIMENSION` | Maximum width or height (default 4096 pixels) |
| `AI_TEMP_DIR` | Optional private temporary directory for validated uploads |
| `HF_HOME` | Hugging Face cache location; relevant because the upstream parser may download weights on initialization |

Weights must be acquired and installed explicitly. Before obtaining the parser or running the official upstream download script, review each dependency and weight license. The service checks for the try-on and DWPose files locally and does not invoke an upstream download command itself. The parser can download its own weights on first use, so initialization must only be attempted in a permitted, explicitly approved evaluation environment.

The environment-based research acknowledgement is not a license grant, and `MODEL_USE_MODE=research` must not be used for commercial deployment.

## API

All routes except health require `Authorization: Bearer <AI_SERVICE_API_TOKEN>`.

### `GET /health`

Public liveness response:

```json
{
  "status": "UP",
  "service": "Virtual Try-On AI Service"
}
```

### `GET /api/v1/model/info`

Returns model name/version, initialization state, device/GPU availability, supported categories, and output resolution.

### `GET /api/v1/model/status`

Reports whether local weights are present, whether the pipeline initialized, whether inference is available, GPU availability, and a sanitized initialization error when relevant. Do not expose this route outside trusted backend/service networks.

### `POST /api/v1/try-on`

Send `multipart/form-data` with:

| Field | Type | Values |
|---|---|---|
| `person_image` | file | JPEG, PNG, or WebP |
| `garment_image` | file | JPEG, PNG, or WebP |
| `category` | string | `tops`, `bottoms`, or `one-pieces` |
| `garment_photo_type` | string | `model` (default) or `flat-lay` |
| `num_timesteps` | integer | 1-50 (default from configuration) |
| `seed` | integer | 0-2147483647 |

Successful requests return a PNG with `X-Inference-Duration-Seconds`, and `X-CUDA-Peak-Memory-MiB` when CUDA memory metrics are available. Oversized files return 413, invalid input returns 422, unavailable inference returns 503, GPU OOM returns 507, and unexpected inference errors return a sanitized 500 response.

Example request (replace the token locally; do not paste a real token into shared logs):

```bash
curl -X POST http://127.0.0.1:8001/api/v1/try-on \
  -H "Authorization: Bearer $AI_SERVICE_API_TOKEN" \
  -F "person_image=@person.jpg" \
  -F "garment_image=@garment.jpg" \
  -F "category=tops" \
  -F "garment_photo_type=flat-lay" \
  --output try-on.png
```

## Tests

Install the development dependencies and run the deterministic API/unit tests:

```bash
pip install -r requirements-dev.txt
pytest -q
```

The regular suite uses a mock pipeline only to verify the API contract; it does not claim model-quality results and does not download model weights. A separate GPU integration test is skipped unless explicitly enabled with a CUDA GPU, locally installed model weights, a permitted research environment, and user-provided consented images:

```bash
RUN_REAL_MODEL_TESTS=true \
MODEL_USE_MODE=research \
MODEL_LICENSE_ACKNOWLEDGED=true \
AI_SERVICE_API_TOKEN=local-test-token \
PERSON_SAMPLE_PATH=/path/to/consented-person.jpg \
GARMENT_SAMPLE_PATH=/path/to/consented-garment.jpg \
pytest -m gpu -q
```

## Kaggle or Colab evaluation

See [`notebooks/fashn_vton_experiment.ipynb`](./notebooks/fashn_vton_experiment.ipynb) for an opt-in notebook workflow. It requires a GPU runtime and explicitly consented sample images. Kaggle and Colab are transient evaluation environments, not secure production hosts. Do not put customer images, API tokens, private notebook URLs, or other sensitive material in public notebooks or datasets.

## Privacy and operational limitations

- Uploads are streamed to a temporary directory, validated by decoded content rather than trusting filenames, and removed after each request.
- Use only images with appropriate consent. Keep logs free of uploaded image contents and credentials.
- This service has no customer authentication, persistence, queue, rate limiting, or production observability. The bearer token is a service-to-service foundation, not end-user auth.
- Concurrent model jobs default to one due to GPU memory use. CPU execution can be impractically slow.
- No model weights are bundled. Regular CI and tests do not fetch models or invoke external services.
