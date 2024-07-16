const app = typeof window.app === 'object' ? window.app : null;

// Defaults are for testing in a local browser where the app API is not available.
const campaignLevel = app ? app.getCampaignLevel() : 1;
const campaignDifficulty = app ? app.getCampaignDifficulty() : 1;
const campaignAiPlayer = app ? app.getCampaignAiPlayer() : 1;

// Fill in the DOM with values from the app.
document.getElementById('campaign-level').textContent = String(campaignLevel);
document.getElementById('campaign-difficulty').textContent = campaignDifficulty;
document.getElementById('campaign-ai-player').textContent =
  campaignAiPlayer === 1 ? 'first' :
  campaignAiPlayer === 2 ? 'second' : 'none';

// Connect difficulty range slider to value.
const difficultyRangeElem = document.getElementById('difficulty-range');
const difficultyValueElem = document.getElementById('difficulty-value');
difficultyRangeElem.oninput = function() {
  difficultyValueElem.textContent = String(difficultyRangeElem.value);
};

document.getElementById('start-custom-game-button').onclick = function() {
  const pieRule = document.getElementById('pie-rule-on').checked;
  const aiPlayer =
      document.getElementById('ai-player-1').checked ? 1 :
      document.getElementById('ai-player-2').checked ? 2 : 0;
  const openingBook =
      document.getElementById('opening-book-on').checked;
  const difficulty = parseInt(document.getElementById('difficulty-range').value);
  app.startCustomGame(pieRule, aiPlayer, openingBook, difficulty);
};
