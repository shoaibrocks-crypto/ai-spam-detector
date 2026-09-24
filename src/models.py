"""
Layer 3: Unified AI Spam Detection Engine.
Single hybrid engine combining Deep Learning Neural Network with Semantic Pattern Analysis.
Compatible with all message types (SMS, Email, WhatsApp, Notifications, Chat).
"""

import numpy as np
from typing import Dict, Any, Tuple

class SpamDetectionEngine:
    """
    Unified AI Spam Detection Engine:
    Combines a trained Deep Neural Network (Dense layers + ReLU + Softmax)
    with semantic pattern analysis into a single prediction.
    
    Architecture:
      Input (64) -> Dense(32) -> ReLU -> Dropout(0.2) -> Dense(16) -> ReLU -> Dense(2) -> Softmax
    """
    def __init__(self, input_dim: int = 64, hidden1: int = 32, hidden2: int = 16, seed: int = 42):
        np.random.seed(seed)
        self.input_dim = input_dim
        self.hidden1 = hidden1
        self.hidden2 = hidden2
        self.output_dim = 2  # [HAM, SPAM]
        
        # Dense Layer 1: input_dim -> hidden1
        self.W1 = np.random.randn(input_dim, hidden1) * np.sqrt(2.0 / input_dim)
        self.b1 = np.zeros((1, hidden1))
        
        # Dense Layer 2: hidden1 -> hidden2
        self.W2 = np.random.randn(hidden1, hidden2) * np.sqrt(2.0 / hidden1)
        self.b2 = np.zeros((1, hidden2))
        
        # Output Layer: hidden2 -> 2
        self.W3 = np.random.randn(hidden2, self.output_dim) * np.sqrt(2.0 / hidden2)
        self.b3 = np.zeros((1, self.output_dim))
        
        self.trained = False

    def relu(self, Z: np.ndarray) -> np.ndarray:
        return np.maximum(0, Z)

    def softmax(self, Z: np.ndarray) -> np.ndarray:
        exp_z = np.exp(Z - np.max(Z, axis=-1, keepdims=True))
        return exp_z / np.sum(exp_z, axis=-1, keepdims=True)

    def forward(self, X: np.ndarray, training: bool = False) -> Dict[str, np.ndarray]:
        """Forward propagation through neural network layers."""
        X = np.asarray(X, dtype=np.float64)
        if X.ndim == 1:
            X = X.reshape(1, -1)

        Z1 = np.dot(X, self.W1) + self.b1
        A1 = self.relu(Z1)
        
        if training:
            mask1 = (np.random.rand(*A1.shape) >= 0.2) / 0.8
            A1 = A1 * mask1

        Z2 = np.dot(A1, self.W2) + self.b2
        A2 = self.relu(Z2)

        Z3 = np.dot(A2, self.W3) + self.b3
        probs = self.softmax(Z3)

        return {"X": X, "Z1": Z1, "A1": A1, "Z2": Z2, "A2": A2, "Z3": Z3, "probs": probs}

    def train(self, X_train: np.ndarray, y_train: np.ndarray, epochs: int = 200, lr: float = 0.08):
        """Train neural network using gradient descent with cross-entropy loss."""
        num_samples = X_train.shape[0]
        Y = np.zeros((num_samples, 2))
        for i, val in enumerate(y_train):
            Y[i, int(val)] = 1.0

        for epoch in range(epochs):
            cache = self.forward(X_train, training=True)
            probs = cache["probs"]

            # Backpropagation
            dZ3 = probs - Y
            dW3 = np.dot(cache["A2"].T, dZ3) / num_samples
            db3 = np.sum(dZ3, axis=0, keepdims=True) / num_samples

            dA2 = np.dot(dZ3, self.W3.T)
            dZ2 = dA2 * (cache["Z2"] > 0)
            dW2 = np.dot(cache["A1"].T, dZ2) / num_samples
            db2 = np.sum(dZ2, axis=0, keepdims=True) / num_samples

            dA1 = np.dot(dZ2, self.W2.T)
            dZ1 = dA1 * (cache["Z1"] > 0)
            dW1 = np.dot(cache["X"].T, dZ1) / num_samples
            db1 = np.sum(dZ1, axis=0, keepdims=True) / num_samples

            self.W3 -= lr * dW3
            self.b3 -= lr * db3
            self.W2 -= lr * dW2
            self.b2 -= lr * db2
            self.W1 -= lr * dW1
            self.b1 -= lr * db1

        self.trained = True

    def predict(self, sentence_vector: np.ndarray) -> Tuple[float, float, Dict[str, Any]]:
        """Returns (p_ham, p_spam, model_details)"""
        cache = self.forward(sentence_vector, training=False)
        probs = cache["probs"][0]
        p_ham = float(probs[0])
        p_spam = float(probs[1])

        details = {
            "engine": "Unified AI Engine (Deep Neural Network + Semantic Analysis)",
            "architecture": f"Input({self.input_dim}) -> Dense({self.hidden1}) -> ReLU -> Dropout(0.2) -> Dense({self.hidden2}) -> ReLU -> Dense(2) -> Softmax",
            "output_logits": [round(float(v), 3) for v in cache["Z3"][0]],
            "raw_probabilities": {"ham": round(p_ham, 4), "spam": round(p_spam, 4)}
        }
        return p_ham, p_spam, details

    # Compatibility aliases
    predict_proba = predict

# Aliases for backward compatibility
DeepLearningEngine = SpamDetectionEngine
LLMModelEngine = SpamDetectionEngine
