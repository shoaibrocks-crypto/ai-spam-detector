"""
Layer 4: Decision Engine.
- Evaluates semantic intent, psychological tone, and suspicious patterns.
- Assesses urgency, coercion, greed/reward bait, and credential harvesting signals.
- Blends AI Model Engine output (Option A or Option B) with behavioral pattern analysis
  to generate calibrated class probabilities: P(Spam) vs P(Ham).
"""

import re
from typing import Dict, Any, List, Tuple

class DecisionEngine:
    """
    Evaluates semantic intent, tone, and suspicious patterns to compute calibrated class probabilities.
    """
    def __init__(self):
        # Psychological Tone Patterns
        self.urgency_patterns = [
            r'\b(urgent|immediately|act now|hurry|final notice|within \d+ hours?|limited time|last chance)\b',
            r'\b(immediate action|account (will be|has been) (locked|suspended|terminated|deactivated))\b'
        ]
        self.fear_coercion_patterns = [
            r'\b(arrest|warrant|lawsuit|penalty|legal action|unauthorized (sign-in|access|activity)|breach)\b',
            r'\b(compromised|safeguard your data|prevent loss of funds)\b'
        ]
        self.reward_greed_patterns = [
            r'\b(congratulations|selected as (a|the) winner|grand prize|lottery|millionaire|claim your prize)\b',
            r'\b(free (bitcoin|crypto|gift card|entry)|guaranteed returns|make \$\d+|earn \$\d+|earn rs\.?\s*\d+)\b',
            r'\b(pending refund.*income tax|pre-approved for|crypto draw|crypto bonus|airdrop)\b',
            r'\b(\d+%\s*returns|investment opportunity|found your number.*directory)\b'
        ]
        
        # Deceptive Structural Patterns
        self.credential_harvesting_patterns = [
            r'\b(verify (your )?(identity|account|password|card|billing)|log ?in (now|here)|update (your )?password)\b',
            r'\b(security alert|online access|unverified login|billing card)\b'
        ]
        self.suspicious_urls_patterns = [
            r'https?://[^\s/$.?#].[^\s]*\.(xyz|cc|top|live|vip|biz|info|club)\b',
            r'https?://bit\.ly/\S+|https?://tinyurl\.com/\S+',
            r'https?://\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}' # IP address as domain
        ]
        self.commercial_promo_patterns = [
            r'\b(\d+%\s*discount|special\s+discount|pharmacy|meds|pharmaceuticals|cheap\s+pills|online\s+pharmacy|hot\s+singles)\b',
            r'\b(free\s+worldwide\s+shipping|exclusive\s+deal|buy\s+direct)\b'
        ]
        self.job_scam_patterns = [
            r'\b(shortlisted for|work from home|part[- ]time job|no experience needed|contact hr on whatsapp|earn \$\d+/hour|daily income)\b'
        ]
        self.delivery_scam_patterns = [
            r'\b(package.*held|parcel.*held|redelivery fee|invalid house number|customs fee|package delivery failed)\b'
        ]
        self.premium_rate_patterns = [
            r'\b(calls cost [£\$]|txt> \w+|send to \d{4,6}|cost \d+p/day|dial 090\d+|win cash|unread message from an admirer)\b'
        ]
        self.viral_hoax_patterns = [
            r'\b(forward this message to \d+|start charging.*whatsapp|keep your account free|whatsapp.*expire in \d+ hours)\b'
        ]
        self.legitimate_transaction_patterns = [
            r'\b(credited to your account|debited from a/c|upi ref|balance:?|valid for \d+ minutes?|do not share (this )?otp)\b',
            r'\b(has shipped|picked up by the delivery partner|appointment with dr\.|uber ride with)\b'
        ]

    def evaluate_tone_and_intent(self, text: str) -> Dict[str, Any]:
        """
        Extract behavioral indicators and intent scores from raw text.
        """
        low = text.lower()
        signals = []
        scores = {
            "urgency": 0.0,
            "fear_coercion": 0.0,
            "reward_greed": 0.0,
            "credential_harvesting": 0.0,
            "suspicious_url": 0.0
        }
        
        # Check Urgency
        for pat in self.urgency_patterns:
            matches = re.findall(pat, low)
            if matches:
                scores["urgency"] += len(matches) * 0.35
                signals.append({
                    "category": "Artificial Urgency",
                    "description": "Uses time-pressure to bypass rational decision-making",
                    "severity": "HIGH",
                    "matches": [m[0] if isinstance(m, tuple) else m for m in matches]
                })

        # Check Fear / Coercion
        for pat in self.fear_coercion_patterns:
            matches = re.findall(pat, low)
            if matches:
                scores["fear_coercion"] += len(matches) * 0.4
                signals.append({
                    "category": "Fear & Coercion Tone",
                    "description": "Fabricates legal or security threats to incite panic",
                    "severity": "CRITICAL",
                    "matches": [m[0] if isinstance(m, tuple) else m for m in matches]
                })

        # Check Reward / Greed
        for pat in self.reward_greed_patterns:
            matches = re.findall(pat, low)
            if matches:
                scores["reward_greed"] += len(matches) * 0.4
                signals.append({
                    "category": "Financial / Lottery Bait",
                    "description": "Promises unrealistic financial windfalls or fake prizes",
                    "severity": "HIGH",
                    "matches": [m[0] if isinstance(m, tuple) else m for m in matches]
                })

        # Check Credential Harvesting
        for pat in self.credential_harvesting_patterns:
            matches = re.findall(pat, low)
            if matches:
                scores["credential_harvesting"] += len(matches) * 0.3
                signals.append({
                    "category": "Credential Harvesting",
                    "description": "Attempts to solicit passwords, credentials, or personal verification",
                    "severity": "HIGH",
                    "matches": [m[0] if isinstance(m, tuple) else m for m in matches]
                })

        # Check Suspicious Link Patterns
        for pat in self.suspicious_urls_patterns:
            matches = re.findall(pat, text, flags=re.IGNORECASE)
            if matches:
                scores["suspicious_url"] += len(matches) * 0.45
                signals.append({
                    "category": "Untrusted / Spoofed Domain",
                    "description": "Contains high-risk TLD (.xyz, .cc, .top) or URL shortener redirect",
                    "severity": "CRITICAL",
                    "matches": matches
                })

        # Check Commercial Promo Bait
        for pat in self.commercial_promo_patterns:
            matches = re.findall(pat, low)
            if matches:
                signals.append({
                    "category": "Unsolicited Commercial Promotion",
                    "description": "Aggressive discount or illicit pharmaceutical advertising",
                    "severity": "HIGH",
                    "matches": [m[0] if isinstance(m, tuple) else m for m in matches]
                })

        # Check Fake Job Scams
        for pat in self.job_scam_patterns:
            matches = re.findall(pat, low)
            if matches:
                scores["reward_greed"] += len(matches) * 0.4
                signals.append({
                    "category": "Fake Employment / Job Scam",
                    "description": "Unsolicited work-from-home or unrealistic salary promises",
                    "severity": "HIGH",
                    "matches": [m[0] if isinstance(m, tuple) else m for m in matches]
                })

        # Check Delivery & Package Scams
        for pat in self.delivery_scam_patterns:
            matches = re.findall(pat, low)
            if matches:
                scores["urgency"] += len(matches) * 0.35
                signals.append({
                    "category": "Deceptive Delivery Notice",
                    "description": "Fabricated parcel issue soliciting fees or clicks",
                    "severity": "HIGH",
                    "matches": [m[0] if isinstance(m, tuple) else m for m in matches]
                })

        # Check Premium Rate Scams
        for pat in self.premium_rate_patterns:
            matches = re.findall(pat, low)
            if matches:
                scores["reward_greed"] += len(matches) * 0.45
                signals.append({
                    "category": "Premium Rate Toll Scam",
                    "description": "Premium rate billing, pay-per-day SMS, or predatory charges",
                    "severity": "CRITICAL",
                    "matches": [m[0] if isinstance(m, tuple) else m for m in matches]
                })

        # Check Viral Hoaxes & Chain Messages
        for pat in self.viral_hoax_patterns:
            matches = re.findall(pat, low)
            if matches:
                scores["urgency"] += len(matches) * 0.4
                signals.append({
                    "category": "Viral Chain Hoax / Spam",
                    "description": "Coercive forwarding chain or fake service expiration",
                    "severity": "HIGH",
                    "matches": [m[0] if isinstance(m, tuple) else m for m in matches]
                })

        # Check Legitimate Transactional Indicators (Safety filter)
        is_legitimate_tx = False
        for pat in self.legitimate_transaction_patterns:
            if re.search(pat, low):
                is_legitimate_tx = True
                break

        # Capitalization & Punctuation shouting check
        alpha_chars = [c for c in text if c.isalpha()]
        if len(alpha_chars) > 10:
            caps_ratio = sum(1 for c in alpha_chars if c.isupper()) / len(alpha_chars)
            if caps_ratio > 0.4:
                signals.append({
                    "category": "Aggressive Presentation",
                    "description": f"Excessive uppercase lettering ({int(caps_ratio * 100)}% CAPS)",
                    "severity": "MEDIUM",
                    "matches": ["CAPS_LOCK"]
                })

        total_risk_score = sum(min(1.0, s) for s in scores.values())
        return {
            "scores": scores,
            "total_risk_score": total_risk_score,
            "signals": signals,
            "is_legitimate_tx": is_legitimate_tx
        }

    def calibrate_decision(self, model_p_spam: float, behavioral_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Blends Layer 3 model probabilities with Layer 4 behavioral & pattern evaluation.
        Produces final calibrated class probabilities and risk assessment.
        """
        risk_score = behavioral_data["total_risk_score"]
        signals = behavioral_data.get("signals", [])
        is_legitimate_tx = behavioral_data.get("is_legitimate_tx", False)

        # Filter out purely presentation signals (CAPS alone doesn't make a bank alert spam)
        malicious_signals = [s for s in signals if s["category"] != "Aggressive Presentation"]
        has_suspicious_url = any(s["category"] == "Untrusted / Spoofed Domain" for s in signals)

        if is_legitimate_tx and not has_suspicious_url and len(malicious_signals) == 0:
            # Verified safe banking / delivery / OTP alert
            calibrated_p_spam = min(0.15, model_p_spam * 0.25)
        elif len(malicious_signals) >= 2:
            # Strong multi-signal pattern elevates spam probability
            calibrated_p_spam = 1.0 - ((1.0 - model_p_spam) * (0.35 ** len(malicious_signals)))
        elif len(malicious_signals) == 1:
            if model_p_spam < 0.25:
                calibrated_p_spam = min(0.38, model_p_spam * 1.4)
            else:
                calibrated_p_spam = max(model_p_spam, 0.75)
        else:
            # Clean text or CAPS only: dampen false positives
            calibrated_p_spam = model_p_spam * 0.45

        # Clamp between [0.01, 0.99]
        calibrated_p_spam = max(0.01, min(0.99, calibrated_p_spam))
        calibrated_p_ham = 1.0 - calibrated_p_spam

        # Classification verdict
        verdict = "SPAM" if calibrated_p_spam >= 0.50 else "HAM"

        # Risk level determination
        if calibrated_p_spam >= 0.85:
            risk_level = "CRITICAL RISK"
            badge_color = "danger"
        elif calibrated_p_spam >= 0.50:
            risk_level = "MODERATE RISK"
            badge_color = "warning"
        elif calibrated_p_spam >= 0.20:
            risk_level = "LOW RISK"
            badge_color = "info"
        else:
            risk_level = "SAFE / CLEAN"
            badge_color = "success"

        return {
            "verdict": verdict,
            "calibrated_probabilities": {
                "spam": round(calibrated_p_spam, 4),
                "ham": round(calibrated_p_ham, 4),
                "spam_percent": f"{calibrated_p_spam * 100:.1f}%",
                "ham_percent": f"{calibrated_p_ham * 100:.1f}%"
            },
            "risk_level": risk_level,
            "badge_color": badge_color,
            "behavioral_signals": behavioral_data["signals"],
            "layer4_risk_score": round(risk_score, 3)
        }
