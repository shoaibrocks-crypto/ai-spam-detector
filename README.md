# AI Spam Detector - Dual Platform (PC & Native Mobile App)

A complete cybersecurity solution featuring:
1. **PC / Laptop Experience**: Interactive 5-Layer AI Architecture Web Dashboard and REST API.
2. **Mobile Experience (Native Android App)**: Real-time background SMS interceptor that auto-detects every incoming message as the default detector on your Android phone.

---

## Architecture Overview

```
+--------------------------------------------------------------------------+
|                     1. INCOMING MESSAGE INGESTION                        |
|   [ Mobile: Android BroadcastReceiver ]      [ PC: Web Dashboard Input ] |
|   - Real-time SMS interception               - Emails, WhatsApp, Chat    |
+------------------------------------+-------------------------------------+
                                     |
                                     v
+--------------------------------------------------------------------------+
|                    2. TOKENIZATION & EMBEDDING LAYER                     |
|    - Sub-word decomposition (BPE / WordPiece style)                      |
|    - High-Dimensional Vector Embeddings (64-Dim Semantic Projection)     |
|    - Contextual term-importance weighting & L2 normalization             |
+------------------------------------+-------------------------------------+
                                     |
                                     v
+--------------------------------------------------------------------------+
|                      3. UNIFIED AI MODEL ENGINE                          |
|    - Deep Neural Network Classifier                                      |
|    - Input(64) -> Dense(32) -> ReLU -> Dropout(0.2)                     |
|      -> Dense(16) -> ReLU -> Dense(2) -> Softmax                         |
+------------------------------------+-------------------------------------+
                                     |
                                     v
+--------------------------------------------------------------------------+
|                           4. DECISION ENGINE                             |
|    - Behavioral intent evaluation (Urgency, Fear, Greed, Phishing)       |
|    - Pattern recognition (Spoofed domains, fake job scams, delivery)    |
|    - Transactional safety filter (guards genuine OTPs and bank alerts)   |
|    - Bayesian probability calibration: P(Spam) vs P(Ham)                 |
+------------------------------------+-------------------------------------+
                                     |
                                     v
+--------------------------------------------------------------------------+
|                            5. OUTPUT LAYER                               |
|   [ Mobile Heads-Up Notification ]          [ PC / Web Reason Cards ]    |
|   - Red Alert: 🚨 SPAM DETECTED             - Verdict [SPAM] or [HAM]    |
|   - Green: 🛡️ VERIFIED SAFE                 - In-text span highlighting  |
+--------------------------------------------------------------------------+
```

---

## Directory Structure

```
Ai _spam detector/
├── android_app/                # Native Android Mobile Application
│   ├── app/
│   │   ├── src/main/AndroidManifest.xml   # SMS permissions & receiver
│   │   └── src/main/java/.../
│   │       ├── MainActivity.kt            # Mobile UI & Scanner
│   │       ├── SmsReceiver.kt             # Intercepts every incoming SMS
│   │       ├── SpamApiClient.kt           # Connects to Render AI backend
│   │       ├── NotificationHelper.kt      # System alert manager
│   │       └── DetectionAdapter.kt        # Intercepted SMS log adapter
│   └── README.md                          # Android build & install guide
│
├── src/                        # 5-Layer AI Detection Core
│   ├── dataset.py              # Curated multi-channel training corpus
│   ├── tokenizer.py            # Subword tokenizer & 64D semantic embeddings
│   ├── models.py               # Unified Deep Neural Network classifier
│   ├── decision_engine.py      # Tone scoring & Bayesian probability calibration
│   ├── reason_mapper.py        # Explainability & in-text highlight generator
│   └── pipeline.py             # 5-Layer Pipeline Orchestrator
│
├── templates/                  # PC Web Dashboard HTML
├── static/                     # Dark glassmorphic CSS & JS
├── app.py                      # Flask REST API server (Cloud & Local)
├── test_pipeline.py            # Automated Unit & Integration test suite
├── run_app.bat                 # 1-Click PC Launcher
└── DEPLOYMENT_GUIDE.md         # Cloud deployment instructions
```

---

## How to Use

### 1. On PC / Laptop
- Double-click `run_app.bat` or run:
  ```powershell
  .\.python_env\python.exe app.py
  ```
- Open browser at **`http://localhost:5000`**.

### 2. On Mobile Phone (Android App)
- Open the `android_app/` folder in Android Studio.
- Click **Build $\to$ Build APK(s)** and install `app-debug.apk` on your phone.
- Grant SMS permission: every incoming text message will now be automatically intercepted, analyzed by AI, and flagged if it is spam or phishing!
