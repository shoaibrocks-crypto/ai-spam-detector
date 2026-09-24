"""
Comprehensive Automated Test Suite for Unified AI Spam Detector.
Verifies all 5 architectural layers, multi-channel message coverage, and REST API endpoints.
"""

import unittest
import json
from src.dataset import TRAINING_DATA, DEMO_PRESETS, preprocess_text
from src.tokenizer import SubwordTokenizer, SemanticEmbeddingEngine
from src.models import SpamDetectionEngine
from src.decision_engine import DecisionEngine
from src.reason_mapper import ReasonMapper
from src.pipeline import SpamDetectorPipeline
from app import app

class TestSpamDetector5Layers(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pipeline = SpamDetectorPipeline()
        cls.client = app.test_client()

    # -------------------------------------------------------------
    # LAYER 1 TESTS: Input Preprocessing
    # -------------------------------------------------------------
    def test_layer1_preprocessing(self):
        raw = "Click https://phishing-site.xyz/login NOW and win Rs.50,000!!!"
        cleaned = preprocess_text(raw)
        self.assertIn("[url]", cleaned)
        self.assertIn("[money]", cleaned)
        self.assertNotIn("!!!", cleaned)
        self.assertEqual(cleaned, cleaned.lower())

    # -------------------------------------------------------------
    # LAYER 2 TESTS: Tokenization & Embedding
    # -------------------------------------------------------------
    def test_layer2_tokenization(self):
        tokenizer = SubwordTokenizer()
        tokens = tokenizer.tokenize("urgent chase bank verification")
        self.assertIn("[CLS]", tokens)
        self.assertIn("[SEP]", tokens)
        ids = tokenizer.convert_tokens_to_ids(tokens)
        self.assertEqual(len(tokens), len(ids))

    def test_layer2_embedding(self):
        engine = SemanticEmbeddingEngine(vocab_size=1000, embedding_dim=64)
        token_ids = [10, 20, 30, 40]
        sentence_vec = engine.embed_sentence(token_ids)
        self.assertEqual(sentence_vec.shape, (64,))
        # Check normalized length
        norm = float(sum(sentence_vec ** 2) ** 0.5)
        self.assertAlmostEqual(norm, 1.0, places=3)

    # -------------------------------------------------------------
    # LAYER 3 TESTS: Unified AI Model Engine
    # -------------------------------------------------------------
    def test_layer3_unified_ai_engine(self):
        engine = SpamDetectionEngine(input_dim=64, hidden1=16, hidden2=8)
        dummy_vec = [0.1] * 64
        p_ham, p_spam, details = engine.predict(dummy_vec)
        self.assertAlmostEqual(p_ham + p_spam, 1.0, places=3)
        self.assertIn("architecture", details)

    # -------------------------------------------------------------
    # LAYER 4 TESTS: Decision Engine & Tone Evaluation
    # -------------------------------------------------------------
    def test_layer4_decision_engine(self):
        decision_eng = DecisionEngine()
        text = "URGENT NOTICE: Your account has been suspended! Immediate action required at http://bad-link.xyz"
        eval_data = decision_eng.evaluate_tone_and_intent(text)
        self.assertGreater(eval_data["scores"]["urgency"], 0)
        self.assertGreater(eval_data["scores"]["suspicious_url"], 0)

        # Calibrate decision
        calib = decision_eng.calibrate_decision(0.80, eval_data)
        self.assertEqual(calib["verdict"], "SPAM")
        self.assertGreater(calib["calibrated_probabilities"]["spam"], 0.90)

    # -------------------------------------------------------------
    # LAYER 5 TESTS: Output Layer & AI Reason Mapping
    # -------------------------------------------------------------
    def test_layer5_reason_mapper(self):
        mapper = ReasonMapper()
        text = "URGENT: Click http://bit.ly/scam"
        fake_decision = {
            "verdict": "SPAM",
            "calibrated_probabilities": {"spam": 0.98, "ham": 0.02, "spam_percent": "98.0%", "ham_percent": "2.0%"},
            "behavioral_signals": [{
                "category": "Artificial Urgency",
                "description": "Uses time pressure",
                "severity": "HIGH",
                "matches": ["urgent"]
            }]
        }
        reasons = mapper.generate_reasons(text, fake_decision, {})
        self.assertEqual(reasons["verdict"], "SPAM")
        self.assertIn("Flagged due to", reasons["primary_headline"])
        self.assertIn("<mark", reasons["highlighted_html"])

    # -------------------------------------------------------------
    # END-TO-END PIPELINE TESTS
    # -------------------------------------------------------------
    def test_pipeline_spam_detection(self):
        spam_sample = "URGENT: Chase Bank account temporarily suspended! Verify at http://chase-auth.xyz"
        res = self.pipeline.analyze(spam_sample)
        self.assertEqual(res["verdict"], "SPAM")
        self.assertGreater(res["probabilities"]["spam"], 0.90)
        self.assertIn("layer1", res["layers"])
        self.assertIn("layer5", res["layers"])

    def test_pipeline_ham_detection(self):
        ham_sample = "Hey Sarah, are we still meeting for lunch at the cafe tomorrow at 12?"
        res = self.pipeline.analyze(ham_sample)
        self.assertEqual(res["verdict"], "HAM")
        self.assertGreater(res["probabilities"]["ham"], 0.50)

    # -------------------------------------------------------------
    # MULTI-CHANNEL MESSAGE RECOGNITION TESTS
    # -------------------------------------------------------------
    def test_whatsapp_job_scam(self):
        msg = "Shortlisted for Amazon work from home! Earn $5000/month. Contact HR on WhatsApp: +1-800-FAKE"
        res = self.pipeline.analyze(msg)
        self.assertEqual(res["verdict"], "SPAM")

    def test_bank_otp_ham(self):
        msg = "Your OTP for SBI Net Banking login is 847291. Valid for 3 minutes. Do not share with anyone."
        res = self.pipeline.analyze(msg)
        self.assertEqual(res["verdict"], "HAM")

    def test_delivery_notification_ham(self):
        msg = "Your Swiggy order #SW1892 has been picked up and will arrive in approximately 25 minutes."
        res = self.pipeline.analyze(msg)
        self.assertEqual(res["verdict"], "HAM")

    # -------------------------------------------------------------
    # REST API TESTS
    # -------------------------------------------------------------
    def test_api_presets(self):
        response = self.client.get('/api/presets')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertEqual(data["status"], "success")
        self.assertGreater(len(data["presets"]), 0)

    def test_api_analyze(self):
        payload = {
            "text": "Congratulations! You won $10,000 in mobile lottery. Claim at http://prize.xyz"
        }
        response = self.client.post('/api/analyze', json=payload)
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertEqual(data["status"], "success")
        self.assertEqual(data["data"]["verdict"], "SPAM")

    def test_api_predict_alias(self):
        payload = {
            "text": "Hey mate, let's catch up tomorrow evening for coffee."
        }
        response = self.client.post('/api/predict', json=payload)
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertEqual(data["status"], "success")
        self.assertEqual(data["data"]["verdict"], "HAM")

if __name__ == '__main__':
    unittest.main()
