# 📱 AI Spam Detector - Native Android App

This is the native Android mobile application for the **AI Spam Detector** project.
It runs in the background on your Android device, intercepts every incoming SMS message in real-time, analyzes it with the 5-Layer AI Engine, and shows a heads-up warning notification whenever spam or phishing is detected.

---

## 🚀 How to Build & Install on Your Phone

### Step 1: Open in Android Studio
1. Open **Android Studio** on your laptop/PC.
2. Click **Open** and select the folder:
   `c:\Users\lenovo\OneDrive\Desktop\Ai _spam detector\android_app`
3. Wait 30 seconds for Gradle to sync dependencies automatically.

### Step 2: Build the APK
1. In Android Studio's top menu, click:
   **Build** $\to$ **Build Bundle(s) / APK(s)** $\to$ **Build APK(s)**.
2. When the build finishes, a notification will appear at the bottom right:
   *`APK(s) generated successfully`*. Click **locate**.
3. You will find the installed file:
   `app-debug.apk` (inside `app/build/outputs/apk/debug/`).

### Step 3: Install on Your Phone
1. Transfer `app-debug.apk` to your phone via USB cable, WhatsApp, Google Drive, or Bluetooth.
2. On your phone, tap `app-debug.apk` and tap **Install**.
3. Open the **AI Spam Detector** app.
4. Tap **ALLOW** when prompted for SMS and Notification permissions.

---

## ⚡ How It Works on Your Phone

1. **Automatic SMS Interception**:
   - The app's `SmsReceiver` wakes up whenever any SMS reaches your phone.
   - It reads the text and calls your cloud AI API (`https://ai-spam-detector.onrender.com/api/analyze`).
   - If SPAM: Your phone buzzes with a high-priority red alert:  
     `🚨 [SPAM DETECTED] From: +1234567890 • Flagged due to artificial urgency & suspicious link`
   - If SAFE: Shows a green verification badge:  
     `🛡️ [VERIFIED SAFE] Clean message`

2. **Manual Scanner**:
   - Copy any message from **WhatsApp, Email, Telegram, or Instagram**.
   - Open the app, paste it in the **Manual Message Scanner**, and tap **SCAN WITH AI** to see the full reasoning.

3. **Set as Default SMS App (Optional)**:
   - Tap the **"DEFAULT APP"** button in the app to make AI Spam Detector your primary SMS application!

4. **Change Backend URL**:
   - Tap the **⚙️ Settings icon** in the top right to change your Render cloud URL at any time.
