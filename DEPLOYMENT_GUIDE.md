# 🌐 How to Get Your Permanent Official Public Internet Link (100% Free)

Follow either of these two free methods to put your **AI Spam Detector** online permanently so anyone worldwide can open it on their phone, laptop, or PC 24/7.

---

## 🌟 Method 1: Render.com (Gives `https://ai-spam-detector.onrender.com`)

### Step 1: Upload Project to GitHub
1. Go to [github.com](https://github.com) and log in.
2. Click **New Repository** (name it `ai-spam-detector`).
3. Click **"uploading an existing file"** link.
4. Drag and drop these files and folders from your project folder:
   - `app.py`
   - `requirements.txt`
   - `Procfile`
   - `render.yaml`
   - `src/` folder
   - `templates/` folder
   - `static/` folder
5. Click **Commit changes**.

### Step 2: Deploy on Render
1. Go to [render.com](https://render.com) and sign in for free with your GitHub account.
2. Click **New +** $\to$ select **Web Service**.
3. Select your `ai-spam-detector` GitHub repository.
4. Render will automatically detect `render.yaml` and set:
   - **Environment**: Python
   - **Build Command**: `pip install -r requirements.txt`
   - **Start Command**: `gunicorn app:app`
5. Click **Create Web Service**.

🎉 Within 2 minutes, Render will assign you your official permanent public link:
👉 **`https://ai-spam-detector-xxxx.onrender.com`**

---

## 🚀 Method 2: Hugging Face Spaces (No Git Needed — Upload Directly in Browser)

### Step 1: Create a Free Space
1. Go to [huggingface.co/join](https://huggingface.co/join) and create a free account (or log in).
2. Go to [huggingface.co/new-space](https://huggingface.co/new-space).
3. Fill in:
   - **Space name**: `ai-spam-detector`
   - **License**: `mit`
   - **Space SDK**: Select **Docker** $\to$ **Blank**.
4. Click **Create Space**.

### Step 2: Drag & Drop Files
1. In your newly created space, click **Files** $\to$ **Add file** $\to$ **Upload files**.
2. Drag and drop all files from your desktop project:
   - `Dockerfile`
   - `requirements.txt`
   - `app.py`
   - `src/`
   - `templates/`
   - `static/`
3. Click **Commit changes to main**.

🎉 Hugging Face will automatically build your Docker container in under 60 seconds and give you a permanent public URL:
👉 **`https://huggingface.co/spaces/YOUR_USERNAME/ai-spam-detector`**

---

## 📱 Installing on Your Mobile Phone
Once your public link is live:
1. Open the link on your mobile phone (Chrome or Safari).
2. Tap **"Add to Home Screen"** or **"Install App"**.
3. It installs as a real native app on your phone home screen!
