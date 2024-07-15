document.getElementById('campaign-level').textContent = String(app.getCampaignLevel());
document.getElementById('campaign-difficulty').textContent = String(app.getCampaignDifficulty());
document.getElementById('campaign-ai-player').textContent = app.getCampaignAiPlayer() === 1 ? 'first' : 'second';
