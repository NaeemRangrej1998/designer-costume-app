from fastapi import FastAPI

app = FastAPI(
    title="Virtual Try-On AI Service",
    description="Service foundation for future virtual try-on model integration.",
    version="0.1.0",
)


@app.get("/health")
def get_health():
    return {"status": "UP", "service": "Virtual Try-On AI Service"}
