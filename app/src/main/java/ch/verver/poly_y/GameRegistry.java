package ch.verver.poly_y;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Tracks the current game in progress and some related information. This ensures the game is not
 * lost when the app is restarted.
 *
 * <p>All data is stored in {@link SharedPreferences}.</p>>
 */
public class GameRegistry {
    public static final int MIN_CAMPAIGN_LEVEL = 1;
    public static final int MAX_CAMPAIGN_LEVEL = MIN_CAMPAIGN_LEVEL + (AiConfig.MAX_DIFFICULTY - AiConfig.MIN_DIFFICULTY + 1)*2;

    private static final String TAG = "GameRegistry";
    private static final String SHARED_PREFERENCES_NAME = "poly_y_prefs";

    private static final String CURRENT_GAME_STATE_KEY = "current_game_state";
    private static final String CURRENT_GAME_AI_PLAYER_KEY = "current_game_ai_player";
    private static final String CURRENT_GAME_AI_CONFIG_KEY = "current_game_ai_config";
    private static final String CURRENT_GAME_IS_CAMPAIGN_KEY = "current_game_is_campaign";
    private static final String CAMPAIGN_LEVEL_KEY = "current_campaign_level_key";

    private static @Nullable GameRegistry instance;

    private final SharedPreferences sharedPreferences;

    private @Nullable GameState currentGameState;
    private int currentGameAiPlayer;
    private @Nullable AiConfig currentGameAiConfig;
    private boolean currentGameIsCampaign;
    private int campaignLevel;

    GameRegistry(Context applicationContext) {
        sharedPreferences = applicationContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        // Load the current game. Maybe TODO: lazy-load instead?
        loadFromSharedPreferences();
    }

    private void loadFromSharedPreferences() {
        String gameStateString = sharedPreferences.getString(CURRENT_GAME_STATE_KEY, null);
        if (gameStateString != null) {
            try {
                currentGameState = GameState.decodeFromString(gameStateString);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode GameState string!", e);
                // continue anyway -- this means we lose the game state, but at least the player
                // can start a new game, which is better than crashing.
                currentGameState = null;
            }
        }
        currentGameAiPlayer = sharedPreferences.getInt(CURRENT_GAME_AI_PLAYER_KEY, 0);
        String aiConfigString = sharedPreferences.getString(CURRENT_GAME_AI_CONFIG_KEY, null);
        if (aiConfigString != null) {
            try {
                currentGameAiConfig = AiConfig.decodeFromString(aiConfigString);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode AiConfig string!", e);
                currentGameAiConfig = null;
                currentGameAiPlayer = 0;
            }
        }
        currentGameIsCampaign = sharedPreferences.getBoolean(CURRENT_GAME_IS_CAMPAIGN_KEY, false);
        campaignLevel = sharedPreferences.getInt(CAMPAIGN_LEVEL_KEY, MIN_CAMPAIGN_LEVEL);
    }

    public static synchronized GameRegistry getInstance(Context context) {
        if (instance == null) instance = new GameRegistry(context.getApplicationContext());
        return instance;
    }

    public synchronized @Nullable GameState getCurrentGameState() {
        return currentGameState;
    }

    public synchronized void setCurrentGameState(@Nullable GameState gameState) {
        if (Objects.equals(gameState, currentGameState)) {
            Log.i(TAG, "Game state unchanged; skipping save.");
            return;
        }
        updateGameState(currentGameState, gameState).apply();;
    }

    public synchronized int getCurrentGameAiPlayer() {
        return currentGameAiPlayer;
    }

    public synchronized @Nullable AiConfig getCurrentGameAiConfig() {
        return currentGameAiConfig;
    }

    public synchronized boolean getCurrentGameIsCampaign() {
        return currentGameIsCampaign;
    }

    public synchronized void startGame(
            GameState newGameState, int aiPlayer, @Nullable AiConfig aiConfig, boolean isCampaign) {
        if (aiPlayer < 0 || aiPlayer > 2) {
            throw new IllegalArgumentException("aiPlayer must be 0, 1 or 2");
        }
        if (aiPlayer != 0 && aiConfig == null) {
            throw new IllegalArgumentException("aiConfig must not be null when aiPlayer is nonzero");
        }
        if (isCampaign && aiPlayer == 0) {
            throw new IllegalArgumentException("aiPlayer must be nonzero when isCampaign is true");
        }

        SharedPreferences.Editor editor = updateGameState(currentGameState, newGameState);

        currentGameAiPlayer = aiPlayer;
        editor.putInt(CURRENT_GAME_AI_PLAYER_KEY, aiPlayer);

        currentGameAiConfig = aiConfig;
        if (aiConfig == null) {
            editor.remove(CURRENT_GAME_AI_CONFIG_KEY);
        } else {
            editor.putString(CURRENT_GAME_AI_CONFIG_KEY, aiConfig.encodeAsString());
        }

        currentGameIsCampaign = isCampaign;
        editor.putBoolean(CURRENT_GAME_IS_CAMPAIGN_KEY, isCampaign);

        editor.apply();
    }

    public int getCampaignLevel() {
        return campaignLevel;
    }

    public int getCampaignDifficulty() {
        return AiConfig.MIN_DIFFICULTY + ((campaignLevel - MIN_CAMPAIGN_LEVEL) >> 1);
    }

    public int getCampaignAiPlayer() {
        return ((campaignLevel - MIN_CAMPAIGN_LEVEL) & 1) + 1;
    }

    @CheckResult
    private SharedPreferences.Editor updateGameState(@Nullable GameState oldGameState, @Nullable GameState newGameState) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (currentGameIsCampaign && currentGameAiPlayer != 0 &&
                oldGameState != null && !oldGameState.isGameOver() &&
                newGameState != null && newGameState.isGameOver()) {
            currentGameIsCampaign = false;
            editor.putBoolean(CURRENT_GAME_IS_CAMPAIGN_KEY, false);

            int winner = newGameState.getWinner();
            if (winner == currentGameAiPlayer) {
                campaignLevel = Math.max(campaignLevel - 1, MIN_CAMPAIGN_LEVEL);
            } else if (winner == GameState.otherPlayer(currentGameAiPlayer)) {
                campaignLevel = Math.min(campaignLevel + 1, MAX_CAMPAIGN_LEVEL);
            }
            editor.putInt(CAMPAIGN_LEVEL_KEY, campaignLevel);
        }

        currentGameState = newGameState;
        if (newGameState == null) {
            Log.i(TAG, "Deleting game state");
            editor.remove(CURRENT_GAME_STATE_KEY);
        } else {
            String encodedGameState = newGameState.encodeAsString();
            Log.i(TAG, "Saving game state: " + encodedGameState);
            editor.putString(CURRENT_GAME_STATE_KEY, encodedGameState);
        }

        return editor;
    }
}
