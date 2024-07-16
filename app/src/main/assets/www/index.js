if (typeof app === 'object') {
  // Fill in the DOM with values from the app.
  document.getElementById('campaign-level').textContent = String(app.getCampaignLevel());
  document.getElementById('campaign-difficulty').textContent = app.getCampaignDifficulty();
  document.getElementById('campaign-ai-player').textContent =
    app.getCampaignAiPlayer() === 1 ? 'first' :
    app.getCampaignAiPlayer() === 2 ? 'second' : 'none';
}

// Connect difficulty range slider to value.
const difficultyRangeElem = document.getElementById('difficulty-range');
const difficultyValueElem = document.getElementById('difficulty-value');
difficultyRangeElem.oninput = function() {
  difficultyValueElem.textContent = String(difficultyRangeElem.value);
};

// Toggle AI option visibility
const aiPlayer0Elem = document.getElementById('ai-player-0');
const aiPlayer1Elem = document.getElementById('ai-player-1');
const aiPlayer2Elem = document.getElementById('ai-player-2');
const openingBookOnElem = document.getElementById('opening-book-on');
const openingBookOffElem = document.getElementById('opening-book-off');

function onAiPlayerChanged() {
  const aiDisabled = aiPlayer0Elem.checked;
  openingBookOffElem.disabled = aiDisabled;
  openingBookOnElem.disabled = aiDisabled;
  difficultyRangeElem.disabled = aiDisabled;
}

aiPlayer0Elem.onchange = onAiPlayerChanged;
aiPlayer1Elem.onchange = onAiPlayerChanged;
aiPlayer2Elem.onchange = onAiPlayerChanged;

// Start a custom game when the button is called.
document.getElementById('start-custom-game-button').onclick = function() {
  const pieRule = document.getElementById('pie-rule-on').checked;
  const aiPlayer = aiPlayer1Elem.checked ? 1 : aiPlayer2Elem.checked ? 2 : 0;
  const openingBook = openingBookOnElem.checked;
  const difficulty = parseInt(difficultyRangeElem.value);
  app.startCustomGame(pieRule, aiPlayer, openingBook, difficulty);
};
