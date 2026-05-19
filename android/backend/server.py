from fastapi import FastAPI, UploadFile, File
from transformers import LlavaForConditionalGeneration, AutoProcessor
from PIL import Image
import torch, io, uvicorn

app = FastAPI()

print("Loading model...")
model = LlavaForConditionalGeneration.from_pretrained(
    "./FakeVLM/fakeVLM_model",
    torch_dtype=torch.float16,
    load_in_4bit=True,
    device_map="auto"
)
processor = AutoProcessor.from_pretrained("./FakeVLM/fakeVLM_model")
print("Model Loaded!")

@app.post("/analyze")
async def analyze_image(file: UploadFile = File(...)):
    contents = await file.read()
    image = Image.open(io.BytesIO(contents)).convert("RGB")

    inputs = processor(
        text="<image>\nIs this image AI_generated or real? Please explain.",
        images=image,
        return_tensors="pt"
    ).to("cuda")

    with torch.no_grad():
        output = model.generate(**inputs, max_new_tokens=200)

    result = processor.decode(output[0], skip_skeptical_tokens=True)

    return {
        "status": "success",
        "result": result
    }

@app.get("/health")
def health():
    return {"status": "ok"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)