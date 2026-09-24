"""
Layer 5: Output Layer & AI Reason Mapping.
- Produces final verdict: [SPAM] or [HAM].
- Maps behavioral triggers, tone indicators, and model weights into human-readable
  explanations (AI Reason Mapping).
- Identifies suspicious token spans and generates highlighted text for visual inspection.
"""

import html
import re
from typing import Dict, Any, List

class ReasonMapper:
    """
    Translates model metrics and behavioral detections into explainable AI insights.
    """
    def generate_reasons(self, text: str, decision_data: Dict[str, Any], model_inspect: Dict[str, Any]) -> Dict[str, Any]:
        """
        Builds the complete Output Layer payload including AI Reason Mapping and highlighted spans.
        """
        verdict = decision_data["verdict"]
        probs = decision_data["calibrated_probabilities"]
        signals = decision_data.get("behavioral_signals", [])
        
        reasons_list = []
        highlight_spans = []

        if verdict == "SPAM":
            # Primary headline synthesis
            if signals:
                unique_categories = []
                for s in signals:
                    cat = s["category"]
                    if cat not in unique_categories:
                        unique_categories.append(cat)
                primary_headline = f"Flagged due to {' and '.join(unique_categories[:2]).lower()}."
            else:
                primary_headline = "Flagged due to anomalous semantic patterns and suspicious heuristics."

            # Build detailed cards
            for sig in signals:
                reasons_list.append({
                    "title": sig["category"],
                    "description": sig["description"],
                    "severity": sig["severity"]
                })
                # Collect match substrings for highlighting
                for m in sig.get("matches", []):
                    if m != "CAPS_LOCK" and len(m) > 1:
                        highlight_spans.append(m)

            # Security recommendation
            recommendation = (
                "Do not click any embedded links or provide credentials. "
                "Verify security alerts through official company channels directly."
            )
        else:
            primary_headline = "Passed safety checks: natural interpersonal communication tone."
            reasons_list.append({
                "title": "Legitimate Linguistic Structure",
                "description": "Text exhibits normal vocabulary distribution and standard context.",
                "severity": "SAFE"
            })
            reasons_list.append({
                "title": "Absence of Coercive Indicators",
                "description": "No artificial urgency, fake lottery rewards, or credential solicitation detected.",
                "severity": "SAFE"
            })
            recommendation = "Message appears safe for standard viewing."

        # Generate HTML highlighted text
        highlighted_html = self.create_highlighted_text(text, highlight_spans)

        return {
            "verdict": verdict,
            "verdict_badge": f"[{verdict}]",
            "confidence_score": probs["spam_percent"] if verdict == "SPAM" else probs["ham_percent"],
            "primary_headline": primary_headline,
            "reason_cards": reasons_list,
            "highlighted_html": highlighted_html,
            "recommendation": recommendation,
            "signals_detected_count": len(signals)
        }

    def create_highlighted_text(self, text: str, highlight_phrases: List[str]) -> str:
        """
        Highlights suspicious triggers and tokens within the original text using safe HTML tags.
        """
        if not text:
            return ""
            
        escaped_text = html.escape(text)
        
        # Sort phrases by length descending to match longer multi-word tokens first
        unique_phrases = sorted(list(set(highlight_phrases)), key=len, reverse=True)
        
        for phrase in unique_phrases:
            if not phrase or len(phrase.strip()) < 2:
                continue
            escaped_phrase = html.escape(phrase)
            pattern = re.compile(re.escape(escaped_phrase), re.IGNORECASE)
            escaped_text = pattern.sub(
                f'<mark class="trigger-highlight" title="Suspicious pattern detected">{escaped_phrase}</mark>',
                escaped_text
            )

        return escaped_text
