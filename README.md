# VerifAI

**VerifAI** is an Android application that detects AI-generated and deepfake images using a multimodal large language model backend. Users upload an image directly from their device; the app returns a verdict (real or fake) along with a natural-language explanation of the specific visual artifacts that indicate manipulation.

---

## Overview

The proliferation of synthetic media has made it increasingly difficult to distinguish real photographs from AI-generated content. VerifAI addresses this problem by combining a production-grade Android client with a vision-language model inference backend, providing both a binary classification and an interpretable explanation for every image analysed.

The model powering the backend is **FakeVLM** (NeurIPS 2025), a multimodal large language model fine-tuned specifically for synthetic image detection across categories including human faces, animals, scenery, and documents.

---

## Architecture (tentative)

```
User (Android App)
        │
        │  HTTPS  (image upload)
        ▼
  FastAPI Server  ──►  FakeVLM Model (HuggingFace Transformers)
        │
        │  JSON response (verdict + explanation)
        ▼
User (Android App)
```

| Layer | Technology |
|---|---|
| Mobile client | Android (Kotlin) |
| Authentication | Firebase Auth |
| API server | FastAPI (Python) |
| ML inference | FakeVLM via HuggingFace Transformers |
| Model | `lingcco/fakeVLM` (LLaVA-based, 7B parameters) |

---

## Features

- Upload any image from your Android device for AI/deepfake detection
- Receive a plain-language explanation of detected visual artifacts
- Firebase authentication for secure user access
- REST API backend compatible with future GCP deployment
- 4-bit quantized inference for consumer GPU compatibility

---

## Repository Structure

```
VerifAI/
├── app/                        # Android application (Kotlin)
│   └── src/
│       └── main/
│           ├── java/           # Kotlin source files
│           └── res/            # Layouts, drawables, strings
├── backend/
│   └── server.py               # FastAPI inference server
├── model/
│   └── fakeVLM_model/          # Downloaded model weights (not tracked)
└── README.md
```

---

## Backend Setup

### Prerequisites

- Python 3.10
- NVIDIA GPU (12 GB VRAM minimum; 16 GB recommended for production)
- CUDA 12.x
- WSL2 (if running on Windows) or native Linux

### Installation

```bash
# Clone the repository
git clone https://github.com/hcp-uw/team-tj.git
cd VerifAI

# Create and activate the conda environment
conda create -n fakevlm python=3.10 -y
conda activate fakevlm

# Install CUDA toolkit (required for compilation)
conda install -c nvidia cuda-toolkit cuda-runtime cuda-nvcc -y

# Install Python dependencies
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu128
pip install transformers accelerate bitsandbytes
pip install fastapi uvicorn python-multipart Pillow

# Download the FakeVLM model weights
huggingface-cli download lingcco/fakeVLM --local-dir ./model/fakeVLM_model
```

### Running the Server

```bash
conda activate fakevlm
python backend/server.py
```

The server starts on `http://0.0.0.0:8080`.

### API Reference

#### `POST /analyze`

Accepts a multipart image upload and returns a detection result.

**Request**

```bash
curl -X POST http://localhost:8080/analyze \
  -F "file=@image.jpg"
```

**Response**

```json
{
  "status": "success",
  "result": "This is a fake image. The image exhibits underlying characteristic inconsistencies in its features that suggest it is artificially created."
}
```

#### `GET /health`

Returns server status.

```json
{ "status": "ok" }
```

---

## Android Setup

### Prerequisites

- Android Studio (latest stable)
- Android SDK 26+
- A Firebase project with Authentication enabled

### Configuration

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Enable **Email/Password** authentication
3. Download `google-services.json` and place it in `app/`
4. Update the server URL in the app to point to your backend

### Building

Open the project in Android Studio and run on a device or emulator (API 26+).

---

## Model

VerifAI uses **FakeVLM**, a multimodal large language model presented at NeurIPS 2025 by OpenDataLab. It is built on the LLaVA architecture and fine-tuned on the FakeClue dataset, which contains fine-grained natural-language annotations of visual artifacts across multiple image categories.

- Paper: [FakeVLM — NeurIPS 2025](https://github.com/opendatalab/FakeVLM)
- Model weights: [lingcco/fakeVLM on HuggingFace](https://huggingface.co/lingcco/fakeVLM)
- Evaluated on FakeClue, LOKI, and DD-VQA benchmarks

---

## Roadmap

- [ ] GCP deployment with vLLM for production throughput
- [ ] Priority request queueing for concurrent users
- [ ] Confidence score in API response
- [ ] In-app image history and result storage
- [ ] Support for video frame analysis

---

## Acknowledgements

- [FakeVLM](https://github.com/opendatalab/FakeVLM) by OpenDataLab
- [LLaVA](https://github.com/haotian-liu/LLaVA) — base architecture
- [vLLM](https://github.com/vllm-project/vllm) — planned production inference engine

---

## License

This project is for academic and research purposes. See [LICENSE](LICENSE) for details.