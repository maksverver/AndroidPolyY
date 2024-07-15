document.getElementById('campaign-level').textContent = String(appApi.getCampaignLevel());
document.getElementById('campaign-difficulty').textContent = String(appApi.getCampaignDifficulty());
document.getElementById('campaign-ai-player').textContent = appApi.getCampaignAiPlayer() === 1 ? 'first' : 'second';
