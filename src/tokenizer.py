"""
Layer 2: Tokenization & Embedding Layer.
- Tokenizes text into sub-words and n-grams.
- Converts token sequences into High-Dimensional Dense Vectors (Word & Sentence Embeddings)
  capturing semantic meaning and contextual representations.
"""

import re
import math
import numpy as np
from typing import List, Dict, Any, Tuple

class SubwordTokenizer:
    """
    Sub-word tokenizer implementing BPE/WordPiece style subword decomposition
    and n-gram extraction for handling out-of-vocabulary terms and prefixes/suffixes.
    """
    def __init__(self, vocab: List[str] = None):
        self.special_tokens = ["[PAD]", "[UNK]", "[CLS]", "[SEP]", "[URL]", "[MONEY]", "[EMAIL]"]
        self.vocab: Dict[str, int] = {}
        self.id_to_token: Dict[int, str] = {}
        
        # Default core subword vocabulary
        common_subwords = [
            "urg", "ent", "not", "ice", "veri", "fy", "auth", "login", "sec", "urity",
            "bank", "chase", "pay", "pal", "pass", "word", "sus", "pend", "lock",
            "congrat", "ulat", "ions", "win", "ner", "prize", "cash", "claim", "free",
            "bit", "coin", "crypto", "invest", "bonus", "dollar", "offer", "limit", "ed",
            "deliv", "ery", "track", "pack", "age", "usps", "order", "ship", "ped",
            "meet", "ing", "sched", "ule", "team", "proj", "ect", "repo", "rt", "rev", "iew",
            "coff", "ee", "dinn", "er", "ton", "ight", "tom", "orrow", "week", "end",
            "call", "help", "supp", "ort", "serv", "ice", "up", "date", "term", "in", "at",
            "http", "com", "org", "net", "xyz", "biz", "link", "click", "now", "act"
        ]
        
        # Build initial vocabulary
        all_initial = self.special_tokens + common_subwords
        for idx, tok in enumerate(all_initial):
            self.vocab[tok] = idx
            self.id_to_token[idx] = tok

    def fit(self, texts: List[str]):
        """Fit tokenizer vocabulary from corpus of texts."""
        for text in texts:
            words = re.findall(r'\b\w+\b', text.lower())
            for word in words:
                # Add word itself
                if word not in self.vocab and len(self.vocab) < 5000:
                    idx = len(self.vocab)
                    self.vocab[word] = idx
                    self.id_to_token[idx] = word
                
                # Extract subwords (character 3-4 grams)
                if len(word) > 4:
                    for i in range(len(word) - 2):
                        sub = word[i:i+3]
                        if sub not in self.vocab and len(self.vocab) < 5000:
                            idx = len(self.vocab)
                            self.vocab[sub] = idx
                            self.id_to_token[idx] = sub

    def tokenize(self, text: str) -> List[str]:
        """
        Tokenize raw string into tokens and sub-words.
        Known words are retained; complex or unknown words are decomposed into sub-word pieces.
        """
        if not text:
            return ["[CLS]", "[SEP]"]

        raw_tokens = re.findall(r'\[[A-Z]+\]|\w+|[^\w\s]', text)
        subwords = ["[CLS]"]

        for token in raw_tokens:
            low = token.lower()
            if token in self.special_tokens:
                subwords.append(token)
            elif low in self.vocab:
                subwords.append(low)
            else:
                # Subword decomposition algorithm
                decomposed = []
                start = 0
                while start < len(low):
                    matched = False
                    # Look for longest matching subword
                    for end in range(len(low), start, -1):
                        piece = low[start:end]
                        if piece in self.vocab or len(piece) <= 2:
                            prefix = "##" if start > 0 else ""
                            decomposed.append(f"{prefix}{piece}")
                            start = end
                            matched = True
                            break
                    if not matched:
                        decomposed.append(f"##{low[start]}")
                        start += 1
                if decomposed:
                    subwords.extend(decomposed)
                else:
                    subwords.append("[UNK]")

        subwords.append("[SEP]")
        return subwords

    def convert_tokens_to_ids(self, tokens: List[str]) -> List[int]:
        """Convert token strings to vocabulary IDs."""
        ids = []
        for t in tokens:
            cleaned_t = t.replace("##", "")
            if t in self.vocab:
                ids.append(self.vocab[t])
            elif cleaned_t in self.vocab:
                ids.append(self.vocab[cleaned_t])
            else:
                ids.append(self.vocab.get("[UNK]", 1))
        return ids


class SemanticEmbeddingEngine:
    """
    Layer 2 High-Dimensional Vector Embedding Engine:
    Maps token IDs to high-dimensional contextual semantic vectors (e.g. 64 dimensions).
    Captures semantic similarity, term significance, and topical intent.
    """
    def __init__(self, vocab_size: int = 5000, embedding_dim: int = 64, random_seed: int = 42):
        self.embedding_dim = embedding_dim
        self.vocab_size = vocab_size
        
        np.random.seed(random_seed)
        # Initialize dense embedding matrix using Xavier/Glorot normal distribution
        limit = np.sqrt(6.0 / (vocab_size + embedding_dim))
        self.embedding_matrix = np.random.uniform(-limit, limit, (vocab_size, embedding_dim))
        
        # Normalize zero vector for padding
        self.embedding_matrix[0] = np.zeros(embedding_dim)

    def embed_tokens(self, token_ids: List[int]) -> np.ndarray:
        """
        Lookup embeddings for an array of token IDs.
        Returns matrix of shape: (sequence_length, embedding_dim)
        """
        valid_ids = [min(max(0, tid), self.vocab_size - 1) for tid in token_ids]
        if not valid_ids:
            return np.zeros((1, self.embedding_dim))
        return self.embedding_matrix[valid_ids]

    def embed_sentence(self, token_ids: List[int], tokens: List[str] = None) -> np.ndarray:
        """
        Mean-pooled semantic sentence vector with contextual weighting:
        Produces a fixed-size high-dimensional embedding capturing total semantic meaning.
        """
        if not token_ids:
            return np.zeros(self.embedding_dim)
            
        token_vecs = self.embed_tokens(token_ids)
        
        # Apply term-importance weighting (CLS/SEP downweighted, content words preserved)
        weights = np.ones(len(token_ids))
        for i, tid in enumerate(token_ids):
            if tokens and i < len(tokens):
                if tokens[i] in ["[CLS]", "[SEP]", "[PAD]"]:
                    weights[i] = 0.2
                elif tokens[i] in ["[URL]", "[MONEY]", "urgent", "verify", "winner", "account"]:
                    weights[i] = 2.5
                    
        norm_weights = weights / (np.sum(weights) + 1e-9)
        sentence_vec = np.sum(token_vecs * norm_weights[:, np.newaxis], axis=0)
        
        # L2 normalize
        norm = np.linalg.norm(sentence_vec)
        if norm > 0:
            sentence_vec = sentence_vec / norm
            
        return sentence_vec

    def get_embedding_inspect_data(self, tokens: List[str], token_ids: List[int], sentence_vector: np.ndarray) -> Dict[str, Any]:
        """
        Helper for UI visualization: provides token breakdown, embedding dimensions,
        and high-dimensional vector snapshot.
        """
        # Take first 8 dimensions for quick visual preview
        preview_dims = [round(float(val), 4) for val in sentence_vector[:8]]
        
        return {
            "token_count": len(tokens),
            "tokens": tokens,
            "token_ids": token_ids[:15],
            "embedding_dim": self.embedding_dim,
            "vector_preview": preview_dims,
            "vector_l2_norm": round(float(np.linalg.norm(sentence_vector)), 4)
        }
