"""
AI Spam Detector - Flask Web App & REST API.
Unified single-engine architecture. Mobile-first PWA.
"""

import os
import sys
from flask import Flask, render_template, request, jsonify

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
    if not text:
        return jsonify({"status": "error", "message": "Please enter a message to analyze."}), 400
    try:
        result = pipeline.analyze(raw_text=text)
        return jsonify({"status": "success", "data": result})
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    print(f"\n* AI Spam Detector running at http://localhost:{port}")
    app.run(host='0.0.0.0', port=port, debug=False)
