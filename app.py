"""
AI Spam Detector - Flask Web App & REST API.
Unified single-engine architecture. Mobile-first PWA.
"""

import os
import sys
from flask import Flask, render_template, request, jsonify, send_from_directory

current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

from src.pipeline import SpamDetectorPipeline
from src.dataset import DEMO_PRESETS

app = Flask(__name__)
pipeline = SpamDetectorPipeline()

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/ai-spam-detector.apk')
@app.route('/download_apk')
def download_apk():
    return send_from_directory(current_dir, 'ai-spam-detector.apk', as_attachment=True)

@app.route('/manifest.json')
def manifest():
    return jsonify({
        "name": "AI Spam Detector",
        "short_name": "SpamGuard",
        "description": "AI-powered spam and phishing detection across SMS, Email, WhatsApp, and Notifications",
        "start_url": "/",
        "display": "standalone",
        "background_color": "#0a0e17",
        "theme_color": "#38bdf8",
        "icons": []
    })

@app.route('/api/presets', methods=['GET'])
def get_presets():
    return jsonify({"status": "success", "presets": DEMO_PRESETS})

@app.route('/api/analyze', methods=['POST'])
@app.route('/api/predict', methods=['POST'])
def analyze():
    data = request.get_json(silent=True) or {}
    text = data.get('text', '').strip()
    sensitivity = data.get('sensitivity', 'balanced')
    if not text:
        return jsonify({"status": "error", "message": "Please enter a message to analyze."}), 400
    try:
        result = pipeline.analyze(raw_text=text, sensitivity=sensitivity)
        return jsonify({"status": "success", "data": result})
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/api/analyze_file', methods=['POST'])
def analyze_file():
    if 'file' not in request.files:
        return jsonify({"status": "error", "message": "No file uploaded."}), 400
    uploaded_file = request.files['file']
    sensitivity = request.form.get('sensitivity', 'balanced')
    try:
        raw_content = uploaded_file.read().decode('utf-8', errors='ignore')
        # Extract lines / chat snippets
        lines = [line.strip() for line in raw_content.splitlines() if line.strip() and len(line.strip()) > 5]
        if not lines:
            return jsonify({"status": "error", "message": "File is empty or contains no valid text lines."}), 400
        
        batch_results = []
        for line in lines[:20]: # Cap to first 20 messages for speed
            res = pipeline.analyze(raw_text=line, sensitivity=sensitivity)
            batch_results.append({
                "text": line[:120] + ("..." if len(line) > 120 else ""),
                "verdict": res["verdict"],
                "confidence": res["confidence_score"],
                "risk_level": res["risk_level"]
            })
        return jsonify({"status": "success", "total_lines": len(lines), "scanned": len(batch_results), "results": batch_results})
    except Exception as e:
        return jsonify({"status": "error", "message": f"File scan error: {str(e)}"}), 500

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    print(f"\n* AI Spam Detector running at http://localhost:{port}")
    app.run(host='0.0.0.0', port=port, debug=False)
