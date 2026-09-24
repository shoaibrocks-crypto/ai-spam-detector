/**
 * AI Spam Detector - Mobile-First PWA Controller
 * Connects frontend with the Unified 5-Layer AI Pipeline
 */

document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const msgInput = document.getElementById('msgInput');
  const inputStats = document.getElementById('inputStats');
  const btnScan = document.getElementById('btnScan');
  const btnScanText = document.getElementById('btnScanText');
  const btnClear = document.getElementById('btnClear');
  const spinner = document.getElementById('spinner');
  const presetsScroll = document.getElementById('presetsScroll');

  // Results Elements
  const results = document.getElementById('results');
  const verdictCard = document.getElementById('verdictCard');
  const verdictBadge = document.getElementById('verdictBadge');
  const riskTag = document.getElementById('riskTag');
  const confVal = document.getElementById('confVal');
  const pHam = document.getElementById('pHam');
  const pSpam = document.getElementById('pSpam');
  const barHam = document.getElementById('barHam');
  const barSpam = document.getElementById('barSpam');
  const headline = document.getElementById('headline');
  const reasonsList = document.getElementById('reasonsList');
  const highlightBox = document.getElementById('highlightBox');
  const recCard = document.getElementById('recCard');

  // Inspector Elements
  const iL1Raw = document.getElementById('iL1Raw');
  const iL1Clean = document.getElementById('iL1Clean');
  const iL2Cnt = document.getElementById('iL2Cnt');
  const iL2Tokens = document.getElementById('iL2Tokens');
  const iL2Vec = document.getElementById('iL2Vec');
  const iL3Arch = document.getElementById('iL3Arch');
  const iL3Prob = document.getElementById('iL3Prob');
  const iL4Bars = document.getElementById('iL4Bars');
  const iL4Cal = document.getElementById('iL4Cal');
  const iL5Out = document.getElementById('iL5Out');

  // Tabs
  const tabs = document.querySelectorAll('.tab');
  const tabContents = document.querySelectorAll('.tab-content');

  // =========================================================================
  // Tab Switching
  // =========================================================================
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      const targetId = tab.getAttribute('data-tab');
      tabs.forEach(t => t.classList.remove('active'));
      tabContents.forEach(tc => tc.classList.remove('active'));
      tab.classList.add('active');
      const pane = document.getElementById(targetId);
      if (pane) pane.classList.add('active');
    });
  });

  // =========================================================================
  // Character & Word Counter
  // =========================================================================
  function updateInputStats() {
    const text = msgInput.value;
    const chars = text.length;
    const words = text.trim() ? text.trim().split(/\s+/).length : 0;
    inputStats.textContent = `${chars} chars • ${words} words`;
  }
  msgInput.addEventListener('input', updateInputStats);

  btnClear.addEventListener('click', () => {
    msgInput.value = '';
    updateInputStats();
    results.style.display = 'none';
  });

  // =========================================================================
  // Load Presets
  // =========================================================================
  async function loadPresets() {
    try {
      const res = await fetch('/api/presets');
      const data = await res.json();
      if (data.status === 'success' || data.status === 'ok') {
        presetsScroll.innerHTML = '';
        data.presets.forEach(p => {
          const chip = document.createElement('button');
          chip.className = 'preset-chip';
          chip.innerHTML = `<span>${p.icon || '📩'}</span><span>${p.title}</span>`;
          chip.addEventListener('click', () => {
            msgInput.value = p.text;
            updateInputStats();
            runScan();
          });
          presetsScroll.appendChild(chip);
        });
      }
    } catch (err) {
      console.warn('Could not load presets:', err);
    }
  }
  loadPresets();

  // =========================================================================
  // Scan Execution
  // =========================================================================
  btnScan.addEventListener('click', runScan);

  async function runScan() {
    const text = msgInput.value.trim();
    if (!text) {
      alert('Please enter or select a message to scan.');
      msgInput.focus();
      return;
    }

    btnScan.disabled = true;
    spinner.classList.remove('hidden');
    btnScanText.textContent = 'Scanning...';

    try {
      const response = await fetch('/api/analyze', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: text })
      });

      const resData = await response.json();
      if (resData.status === 'success' || resData.status === 'ok') {
        renderResults(resData.data);
        renderInspector(resData.data.layers || resData.data.pipeline_layers);
      } else {
        alert(resData.message || 'Scanning error occurred.');
      }
    } catch (err) {
      console.error('Scan request failed:', err);
      alert('Unable to connect to AI Spam Detector backend.');
    } finally {
      btnScan.disabled = false;
      spinner.classList.add('hidden');
      btnScanText.textContent = 'Scan Message';
    }
  }

  // =========================================================================
  // Render Results
  // =========================================================================
  function renderResults(data) {
    results.style.display = 'flex';
    results.style.flexDirection = 'column';
    results.style.gap = '0.9rem';

    const isSpam = data.verdict === 'SPAM';
    verdictCard.className = `verdict-card ${isSpam ? 'spam-mode' : 'ham-mode'}`;
    verdictBadge.textContent = data.verdict_badge || `[${data.verdict}]`;
    riskTag.textContent = data.risk_level;
    confVal.textContent = data.confidence || data.confidence_score;

    // Probabilities
    const probs = data.probabilities;
    const spamPct = parseFloat(probs.spam_percent);
    const hamPct = parseFloat(probs.ham_percent);
    pHam.textContent = probs.ham_percent;
    pSpam.textContent = probs.spam_percent;
    barHam.style.width = `${hamPct}%`;
    barSpam.style.width = `${spamPct}%`;

    // Headline
    headline.textContent = data.headline || data.primary_headline;

    // Reason Cards
    reasonsList.innerHTML = '';
    const cards = data.reasons || data.reason_cards || [];
    cards.forEach(c => {
      const item = document.createElement('div');
      item.className = 'reason-item';
      item.innerHTML = `
        <div class="reason-header">
          <span class="reason-title">${escapeHtml(c.title)}</span>
          <span class="sev-tag sev-${c.severity}">${c.severity}</span>
        </div>
        <div class="reason-body">${escapeHtml(c.description)}</div>
      `;
      reasonsList.appendChild(item);
    });

    // Highlighted Text
    highlightBox.innerHTML = data.highlighted_html || escapeHtml(msgInput.value);

    // Security Recommendation
    recCard.innerHTML = `<strong>🛡️ Security Advice:</strong> ${escapeHtml(data.recommendation)}`;

    // Scroll to results smoothly
    results.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  // =========================================================================
  // Render Inspector (Tab 2)
  // =========================================================================
  function renderInspector(layers) {
    if (!layers) return;

    // Layer 1
    if (layers.layer1) {
      iL1Raw.textContent = layers.layer1.raw_text;
      iL1Clean.textContent = layers.layer1.cleaned_text;
    }

    // Layer 2
    if (layers.layer2) {
      const tokens = layers.layer2.tokens || layers.layer2.subword_tokens || [];
      iL2Cnt.textContent = tokens.length;
      iL2Tokens.innerHTML = '';
      tokens.forEach(t => {
        const span = document.createElement('span');
        span.className = 'token-pill';
        span.textContent = t;
        iL2Tokens.appendChild(span);
      });

      const preview = layers.layer2.vector_preview || [];
      iL2Vec.innerHTML = '';
      preview.forEach((v, idx) => {
        const cell = document.createElement('div');
        cell.className = 'vec-cell';
        cell.innerHTML = `<small>D${idx}</small><br>${v}`;
        iL2Vec.appendChild(cell);
      });
    }

    // Layer 3
    if (layers.layer3) {
      const details = layers.layer3.model_details || layers.layer3.model_inspect || {};
      iL3Arch.textContent = details.architecture || 'Dense Neural Network';
      iL3Prob.textContent = `Raw P(Ham): ${layers.layer3.raw_p_ham}  |  Raw P(Spam): ${layers.layer3.raw_p_spam}`;
    }

    // Layer 4
    if (layers.layer4) {
      const scores = layers.layer4.intent_scores || {};
      iL4Bars.innerHTML = '';
      for (const [k, v] of Object.entries(scores)) {
        const clamped = Math.min(1.0, v);
        const row = document.createElement('div');
        row.className = 'intent-row';
        row.innerHTML = `
          <div class="intent-row-label">
            <span>${k.replace('_', ' ').toUpperCase()}</span>
            <span>${clamped.toFixed(2)}</span>
          </div>
          <div class="intent-track">
            <div class="intent-fill" style="width: ${clamped * 100}%;"></div>
          </div>
        `;
        iL4Bars.appendChild(row);
      }
      const cal = layers.layer4.calibrated || layers.layer4.calibrated_probabilities || {};
      iL4Cal.textContent = `Calibrated P(Spam): ${cal.spam_percent || '-'} | Risk: ${layers.layer4.risk_level}`;
    }

    // Layer 5
    if (layers.layer5) {
      iL5Out.textContent = `Verdict: ${layers.layer5.verdict_badge || layers.layer5.verdict} (Confidence: ${layers.layer5.confidence || layers.layer5.confidence_score})\nReason: ${layers.layer5.headline || layers.layer5.primary_headline}`;
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;")
              .replace(/</g, "&lt;")
              .replace(/>/g, "&gt;")
              .replace(/"/g, "&quot;")
              .replace(/'/g, "&#039;");
  }
});
