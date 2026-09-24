"""
Unified 5-Layer AI Spam Detection Pipeline.
Single engine — no toggles, no dual-engine complexity.
Compatible with all message formats: SMS, Email, WhatsApp, Social Media, OTPs, Notifications.
"""

import numpy as np
from typing import Dict, Any

from src.dataset import TRAINING_DATA, preprocess_text
from src.tokenizer import SubwordTokenizer, SemanticEmbeddingEngine
from src.models import SpamDetectionEngine
from src.decision_engine import DecisionEngine
from src.reason_mapper import ReasonMapper

class SpamDetectorPipeline:
    def __init__(self):
        print("Initializing Unified AI Spam Detection Engine...")

        # Layer 2: Tokenizer & Embeddings
        self.tokenizer = SubwordTokenizer()
        corpus_texts = [sample["text"] for sample in TRAINING_DATA]
        self.tokenizer.fit(corpus_texts)
        self.embedding_engine = SemanticEmbeddingEngine(vocab_size=5000, embedding_dim=64)

        # Layer 3: Unified AI Engine
        self.engine = SpamDetectionEngine(input_dim=64, hidden1=32, hidden2=16)
        self._train_model(TRAINING_DATA)

        # Layer 4 & 5
        self.decision_engine = DecisionEngine()
        self.reason_mapper = ReasonMapper()
        print("AI Engine ready!")

    def _train_model(self, data):
        """Train the unified neural network on curated dataset."""
        X_list, y_list = [], []
        for item in data:
            cleaned = preprocess_text(item["text"])
            tokens = self.tokenizer.tokenize(cleaned)
            token_ids = self.tokenizer.convert_tokens_to_ids(tokens)
            vec = self.embedding_engine.embed_sentence(token_ids, tokens)
            X_list.append(vec)
            y_list.append(1 if item["label"] == "spam" else 0)
        self.engine.train(np.array(X_list), np.array(y_list), epochs=200, lr=0.08)

    def analyze(self, raw_text: str, *args, **kwargs) -> Dict[str, Any]:
        """Execute full 5-layer pipeline on input text."""
        
        # LAYER 1: Input
        cleaned_text = preprocess_text(raw_text)
        layer1 = {
            "name": "Input Layer",
            "raw_text": raw_text,
            "cleaned_text": cleaned_text,
            "char_count": len(raw_text),
            "word_count": len(raw_text.split())
        }

        # LAYER 2: Tokenization & Embedding
        tokens = self.tokenizer.tokenize(cleaned_text)
        token_ids = self.tokenizer.convert_tokens_to_ids(tokens)
        sentence_vector = self.embedding_engine.embed_sentence(token_ids, tokens)
        layer2_inspect = self.embedding_engine.get_embedding_inspect_data(tokens, token_ids, sentence_vector)
        layer2 = {
            "name": "Tokenization & Embedding",
            "subword_tokens": tokens,
            "tokens": tokens,
            "token_ids": token_ids,
            "embedding_dim": layer2_inspect["embedding_dim"],
            "vector_preview": layer2_inspect["vector_preview"],
            "vector_l2_norm": layer2_inspect["vector_l2_norm"],
            "vector_norm": layer2_inspect["vector_l2_norm"],
            "total_tokens": len(tokens)
        }

        # LAYER 3: AI Engine
        p_ham, p_spam, model_details = self.engine.predict(sentence_vector)
        layer3 = {
            "name": "AI Engine",
            "selected_engine": "Unified AI Engine",
            "model_details": model_details,
            "model_inspect": model_details,
            "raw_p_spam": round(p_spam, 4),
            "raw_p_ham": round(p_ham, 4)
        }

        # LAYER 4: Decision Engine
        behavioral_data = self.decision_engine.evaluate_tone_and_intent(raw_text)
        decision_result = self.decision_engine.calibrate_decision(p_spam, behavioral_data)
        layer4 = {
            "name": "Decision Engine",
            "intent_scores": behavioral_data["scores"],
            "behavioral_signals": behavioral_data["signals"],
            "signals": behavioral_data["signals"],
            "calibrated": decision_result["calibrated_probabilities"],
            "calibrated_probabilities": decision_result["calibrated_probabilities"],
            "risk_level": decision_result["risk_level"]
        }

        # LAYER 5: Output
        output = self.reason_mapper.generate_reasons(raw_text, decision_result, model_details)
        layer5 = {
            "name": "Output Layer",
            "verdict": output["verdict"],
            "verdict_badge": output["verdict_badge"],
            "confidence": output["confidence_score"],
            "confidence_score": output["confidence_score"],
            "primary_headline": output["primary_headline"],
            "headline": output["primary_headline"],
            "reason_cards": output["reason_cards"],
            "reasons": output["reason_cards"],
            "highlighted_html": output["highlighted_html"],
            "recommendation": output["recommendation"]
        }

        layers_dict = {
            "layer1": layer1,
            "layer2": layer2,
            "layer3": layer3,
            "layer4": layer4,
            "layer5": layer5
        }

        return {
            "verdict": output["verdict"],
            "verdict_badge": output["verdict_badge"],
            "confidence": output["confidence_score"],
            "confidence_score": output["confidence_score"],
            "risk_level": decision_result["risk_level"],
            "badge_color": decision_result["badge_color"],
            "probabilities": decision_result["calibrated_probabilities"],
            "headline": output["primary_headline"],
            "primary_headline": output["primary_headline"],
            "reasons": output["reason_cards"],
            "reason_cards": output["reason_cards"],
            "highlighted_html": output["highlighted_html"],
            "recommendation": output["recommendation"],
            "layers": layers_dict,
            "pipeline_layers": layers_dict
        }
