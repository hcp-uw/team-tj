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
team-tj/
├── android/                    # Android application (Kotlin)
│   └── app/
│       └── src/main/           # Kotlin source, layouts, resources
├── backend/                    # FastAPI inference server
│   ├── server.py               # Entry point (python server.py)
│   └── app/                    # API routes, model handler, config
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
cd team-tj

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
cd backend
huggingface-cli download lingcco/fakeVLM --local-dir ./FakeVLM/fakeVLM_model
```

### Running the Server

```bash
conda activate fakevlm
cd backend
python server.py
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
3. Enable **Cloud Storage** and **Firestore Database**
4. Deploy security rules from `firebase/storage.rules` and `firebase/firestore.rules` (Firebase Console → Rules)
5. Download `google-services.json` and place it in `android/app/`
6. Set your backend URL in `android/local.properties` (optional; defaults to emulator host):
   ```properties
   api.base.url=http://10.0.2.2:8080
   ```
   For a physical device, use your computer's LAN IP, e.g. `http://192.168.1.10:8080`

### Firebase `PERMISSION_DENIED`

If the app shows **`PERMISSION_DENIED: Missing or insufficient permissions`**, the signed-in user is fine but **Firestore or Storage is rejecting writes**. Typical causes:

1. **Rules not published** — In Firebase Console, open **Firestore → Rules** and **Storage → Rules**, then paste the contents of `firebase/firestore.rules` and `firebase/storage.rules` from this repo and **Publish**.
2. **Firestore or Storage not enabled** — Create a default database and default bucket if the console prompts you.

Until rules allow `users/{uid}/…`, cloud history may stay empty; the app still runs inference if the Python backend is up.

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