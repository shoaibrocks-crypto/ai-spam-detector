"""
Layer 1: Dataset & Preprocessing Module for AI Spam Detector.
Comprehensive curated dataset covering ALL message types:
SMS, Emails, WhatsApp, Social Media DMs, App Notifications, OTPs, and more.
"""

import re
import html

# ============================================================================
# COMPREHENSIVE TRAINING DATASET - Covers every real-world message type
# ============================================================================
TRAINING_DATA = [
    # ===================== SPAM MESSAGES =====================

    # --- Phishing & Account Verification Scams ---
    {"text": "URGENT: Your Bank of America account has been temporarily locked due to suspicious activity. Click here to verify: http://bit.ly/secure-bank-login or your account will be permanently closed.", "label": "spam"},
    {"text": "PayPal Security Alert: We detected an unauthorized sign-in from Nigeria. If this was not you, update your password immediately at https://paypa1-security-verify.net to prevent loss of funds.", "label": "spam"},
    {"text": "Netflix Account Update: Your payment failed for this month. Update your billing card now at http://netflix-billing-update.cc to avoid account termination within 24 hours.", "label": "spam"},
    {"text": "Apple Support: Your iCloud has been breached and private photos are at risk. Log in now to safeguard your data: http://apple-id-verify-cloud.com", "label": "spam"},
    {"text": "Your Amazon Prime membership has expired! Renew now to avoid losing access to exclusive deals: http://amzn-prime-renew.xyz", "label": "spam"},
    {"text": "Google Security Warning: Someone from an unknown device signed into your Gmail. Secure your account now: http://google-security-check.top", "label": "spam"},
    {"text": "Instagram Alert: Your account will be disabled for violating community guidelines. Appeal here: http://instagram-appeal-center.cc", "label": "spam"},

    # --- Financial Bait, Lottery & Crypto Scams ---
    {"text": "CONGRATULATIONS! You have been selected as the grand prize winner of $1,000,000 in the International Mobile Lottery! Call +447891234567 or claim your prize at http://claim-million.org", "label": "spam"},
    {"text": "Exclusive Offer! You are eligible for an instant personal loan of $50,000 with 0% interest for 12 months. No credit check needed! Reply YES or visit http://fast-cash-loan.biz", "label": "spam"},
    {"text": "Make $5,000 a week working from home on your phone! Guaranteed returns on crypto investment. WhatsApp +1-800-FAKE-JOB now to get started today!", "label": "spam"},
    {"text": "FREE BITCOIN AIRDROP! Elon Musk is giving away 5000 BTC. Send 0.1 BTC to receive 1.0 BTC immediately back at http://musk-crypto-bonus.live", "label": "spam"},
    {"text": "Dear Customer, You have a pending refund of Rs.15,000 from Income Tax Department. Click http://incometax-refund.xyz to claim before it expires.", "label": "spam"},
    {"text": "You've been pre-approved for a credit card with Rs.5,00,000 limit! No annual fee. Apply now: http://instant-card-apply.vip", "label": "spam"},

    # --- SMS & Promo Spam ---
    {"text": "Free entry in 2 a weekly comp to win FA Cup final tkts 21st May 2005. Text FA to 87121 to receive entry question(std txt rate).", "label": "spam"},
    {"text": "SIX chances to win CASH! From 100 to 20,000 pounds txt> CSH11 and send to 87575. Cost 150p/day 6days.", "label": "spam"},
    {"text": "HOT SINGLES in your area want to chat with you right now! Click here to browse profiles: http://hot-chat-dating.vip/meet", "label": "spam"},
    {"text": "Special 70% discount on all designer watches and pharmaceuticals! Free worldwide shipping. Visit our online pharmacy now: http://cheap-meds-direct.com", "label": "spam"},
    {"text": "You have 1 new unread message from an admirer! Dial 09061743810 to listen. Calls cost £1.50/min. 18+ only.", "label": "spam"},

    # --- Delivery & Package Scams ---
    {"text": "Final reminder: Your package delivery failed because the address was incomplete. Pay $1.99 redelivery fee now at http://usps-tracking-package-redelivery.info", "label": "spam"},
    {"text": "FedEx Notice: Your parcel #FX829401 is held at customs. Pay clearance fee of $4.99 to release: http://fedex-customs-pay.club", "label": "spam"},
    {"text": "Flipkart Delivery Alert: Your order could not be delivered. Reschedule at http://flipkart-reschedule.xyz or it will be returned.", "label": "spam"},

    # --- Government & Authority Impersonation ---
    {"text": "IRS Final Notice: You owe $4,210 in unpaid federal taxes. A warrant for your arrest has been issued. Call back immediately to settle payment.", "label": "spam"},
    {"text": "EPFO Alert: Your PF withdrawal is pending due to KYC mismatch. Update your Aadhaar now at http://epfo-kyc-update.top to avoid account freeze.", "label": "spam"},

    # --- Gift Card & Survey Scams ---
    {"text": "Claim your $500 Amazon Gift Card reward now! Complete a 30-second survey to qualify: http://amzn-survey-giftcard.xyz", "label": "spam"},
    {"text": "You've been selected for a Walmart customer satisfaction survey! Complete it and win a $1000 gift card: http://walmart-survey-winner.live", "label": "spam"},

    # --- WhatsApp & Social Media Spam ---
    {"text": "Hi! I found your number on a business directory. I have an amazing investment opportunity with 300% returns in just 30 days. Interested? Reply YES.", "label": "spam"},
    {"text": "BREAKING: WhatsApp is going to start charging! Forward this message to 20 contacts to keep your account free forever!", "label": "spam"},
    {"text": "Your WhatsApp account will expire in 48 hours. Verify your account now: http://whatsapp-verify-account.cc", "label": "spam"},
    {"text": "Hey! Check out this amazing weight loss pill that helped me lose 20kg in 2 weeks! Order here: http://miracle-diet-pills.biz", "label": "spam"},

    # --- Job Scam SMS ---
    {"text": "Dear Candidate, You are shortlisted for a work from home job. Salary Rs.25000-50000/month. Contact HR on WhatsApp: +91-9876543210", "label": "spam"},
    {"text": "Amazon is hiring! Earn $35/hour from home. No experience needed. Apply now at http://amazon-jobs-hiring.top", "label": "spam"},

    # ===================== HAM (LEGITIMATE) MESSAGES =====================

    # --- Work & Office Emails ---
    {"text": "Hi Alex, please find attached the quarterly financial report and slide deck for tomorrow morning's board meeting. Let me know if you need any edits before 9 AM.", "label": "ham"},
    {"text": "Team, as a reminder, the sprint planning meeting has been moved to 2:30 PM today in Conference Room B. Zoom link is attached to the calendar invite.", "label": "ham"},
    {"text": "Good morning, could you please review the pull request on GitHub when you have a moment? I fixed the unit tests and addressed your feedback on error handling.", "label": "ham"},
    {"text": "Hi Sarah, thank you for your interview yesterday. We were very impressed with your background and would love to invite you for the final round next Tuesday.", "label": "ham"},
    {"text": "Attached is the revised contract for the software vendor. Our legal department reviewed the clauses and approved the changes.", "label": "ham"},
    {"text": "Hey team, I've deployed the staging build. Please run your regression tests and report any issues by EOD. The production release is scheduled for Friday.", "label": "ham"},

    # --- Personal & Everyday SMS/Chat ---
    {"text": "Hey! Are we still on for dinner tonight at 7? Thinking about trying that new Italian place downtown.", "label": "ham"},
    {"text": "I just landed at the airport. Grabbing my luggage and heading to the taxi stand now. See you in about 30 minutes!", "label": "ham"},
    {"text": "Mom called earlier and asked if you could pick up some milk and eggs on your way home from work.", "label": "ham"},
    {"text": "Happy birthday! Hope you have a wonderful day celebrating with family and friends. Wishing you all the best this year!", "label": "ham"},
    {"text": "Can you send me the recipe for that pasta dish you made last weekend? Want to try cooking it tonight.", "label": "ham"},
    {"text": "Hey, forgot my keys on the counter. Could you leave the back door unlocked or hide the spare key under the pot?", "label": "ham"},
    {"text": "Just finished reading that book you recommended. It was incredible, especially the ending! What are you reading next?", "label": "ham"},
    {"text": "Running 10 minutes late for our coffee meetup. Traffic is crazy today. Save me a seat!", "label": "ham"},
    {"text": "Did you watch the cricket match last night? What an incredible finish! Can't believe they won in the last over.", "label": "ham"},

    # --- WhatsApp & Group Chats (Legitimate) ---
    {"text": "Guys, who's bringing the snacks for movie night on Saturday? I'll handle the drinks and popcorn.", "label": "ham"},
    {"text": "Hey everyone, I've created a shared Google Doc for the trip itinerary. Please add your preferences by Wednesday.", "label": "ham"},
    {"text": "Bro can you send me that meme you posted yesterday? I want to forward it to my cousin lol", "label": "ham"},
    {"text": "Family group: Grandma's birthday party is on Sunday at 4 PM. Please confirm who all are coming so we can plan the food.", "label": "ham"},

    # --- Legitimate Transactional Notifications ---
    {"text": "Your Uber ride with Michael is arriving now in a Silver Toyota Camry (License: 7XYZ89). Please confirm your driver before entering.", "label": "ham"},
    {"text": "Your verification security code is 492018. It expires in 5 minutes. If you did not request this code, please ignore this message.", "label": "ham"},
    {"text": "Your order #84920 from Best Buy has shipped! Track your package on the carrier site or view order history in your account.", "label": "ham"},
    {"text": "Doctor's appointment reminder: You have an appointment with Dr. Chen on Friday, Oct 14th at 10:00 AM. Reply 1 to confirm or 2 to reschedule.", "label": "ham"},
    {"text": "Your Swiggy order #SW1892 has been picked up by the delivery partner and will arrive in approximately 25 minutes.", "label": "ham"},
    {"text": "Zomato: Your food is being prepared! Estimated delivery in 35 minutes. Track your order in the app.", "label": "ham"},

    # --- OTP & Bank Notifications (Legitimate) ---
    {"text": "Your OTP for SBI Net Banking login is 847291. Valid for 3 minutes. Do not share this OTP with anyone.", "label": "ham"},
    {"text": "Rs.2,500.00 debited from A/c XX4521 on 18-Sep. UPI Ref: 329184729134. If not done by you, call 1800-XXX-XXXX.", "label": "ham"},
    {"text": "Your HDFC Credit Card payment of Rs.8,450 is due on 25-Sep-2026. Pay now to avoid late fee charges.", "label": "ham"},
    {"text": "INR 1,200.00 credited to your account XX7834 via UPI from RAHUL SHARMA. UPI Ref No: 426819374.", "label": "ham"},
    {"text": "Your Paytm wallet has been credited with Rs.500. Current balance: Rs.1,250. Transaction ID: PT829401.", "label": "ham"},

    # --- App Notifications (Legitimate) ---
    {"text": "Reminder: You have a meeting 'Project Sync' in 15 minutes on Google Meet. Click to join.", "label": "ham"},
    {"text": "Your Spotify Premium subscription has been renewed. Rs.119 charged to your card ending 4521.", "label": "ham"},
    {"text": "LinkedIn: Priya Sharma viewed your profile. See what they're up to.", "label": "ham"},
    {"text": "YouTube: New video from your subscription - 'Building AI Projects from Scratch' by Tech With Tim.", "label": "ham"},
    {"text": "Weather Alert: Heavy rainfall expected in your area tomorrow. Carry an umbrella and stay safe.", "label": "ham"},
]

# Preset demos for UI
DEMO_PRESETS = [
    {
        "title": "Phishing Email",
        "icon": "🎣",
        "text": "URGENT NOTICE: Your Chase Bank online access will be deactivated in 12 hours due to unverified suspicious login attempts. Verify your identity immediately by clicking: http://chase-security-login-auth.com/verify to prevent permanent account suspension."
    },
    {
        "title": "Lottery Scam SMS",
        "icon": "💰",
        "text": "CONGRATULATIONS! You have won 1.5 Bitcoin ($95,000 USD) in the 2026 International Crypto Draw! Reply CLAIM or visit http://crypto-win-payout.live to receive funds within 24 hours."
    },
    {
        "title": "Delivery Scam",
        "icon": "📦",
        "text": "USPS Alert: Package tracking ID #US89218 is held at distribution depot due to invalid house number. Pay $2.49 redelivery fee now at http://usps-redelivery-notice.xyz"
    },
    {
        "title": "Job Scam",
        "icon": "💼",
        "text": "Dear Candidate, You are shortlisted for Amazon work from home job. Salary $3000-$5000/month. No experience needed. Contact HR on WhatsApp: +1-800-555-FAKE"
    },
    {
        "title": "Work Email",
        "icon": "✉️",
        "text": "Hi team, I have pushed the latest commits to the feature branch. Please review the pull request and run the integration tests before merging into main. Let's discuss in our standup tomorrow."
    },
    {
        "title": "Friend Chat",
        "icon": "💬",
        "text": "Hey mate! Are we still catching up for coffee after work today? Let me know if 5:30 PM works for you, or we can push it to tomorrow."
    },
    {
        "title": "Bank OTP",
        "icon": "🔐",
        "text": "Your OTP for SBI Net Banking login is 847291. Valid for 3 minutes. Do not share this OTP with anyone."
    },
    {
        "title": "Food Delivery",
        "icon": "🍕",
        "text": "Your Swiggy order #SW1892 has been picked up by the delivery partner and will arrive in approximately 25 minutes."
    },
]

def preprocess_text(text: str) -> str:
    """Clean and standardize raw text input."""
    if not text or not isinstance(text, str):
        return ""
    cleaned = html.unescape(text)
    cleaned = re.sub(r'https?://\S+|www\.\S+', ' [URL] ', cleaned)
    cleaned = re.sub(r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b', ' [EMAIL] ', cleaned)
    cleaned = re.sub(r'[\$£€¥₹]\s*\d+([.,]\d+)?|\d+([.,]\d+)?\s*(dollars|usd|pounds|gbp|euros|inr|rupees)', ' [MONEY] ', cleaned, flags=re.IGNORECASE)
    cleaned = re.sub(r'\b(Rs\.?\s*\d+([.,]\d+)?)\b', ' [MONEY] ', cleaned)
    cleaned = re.sub(r'([!?.]){2,}', r'\1', cleaned)
    cleaned = cleaned.lower()
    cleaned = re.sub(r'\s+', ' ', cleaned).strip()
    return cleaned
