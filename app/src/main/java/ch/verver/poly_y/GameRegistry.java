package ch.verver.poly_y;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Objects;

public class GameRegistry {
    private static final String TAG = "GameRegistry";
    private static final String SHARED_PREFERENCES_NAME = "poly_y_prefs";

    private static final String CURRENT_GAME_STATE_KEY = "current_game_state";
    private static final String CURRENT_GAME_AI_PLAYER_KEY = "current_game_ai_player";
    private static final String CURRENT_GAME_AI_CONFIG_KEY = "current_game_ai_config";
    private static final String CURRENT_GAME_IS_CAMPAIGN_KEY = "current_game_is_campaign";

    private static @Nullable GameRegistry instance;

    private final SharedPreferences sharedPreferences;

    private @Nullable GameState currentGameState;
    private int currentGameAiPlayer;
    private @Nullable AiConfig currentGameAiConfig;
    private boolean currentGameIsCampaign;

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
    }

    public static synchronized GameRegistry getInstance(Context context) {
        if (instance == null) instance = new GameRegistry(context.getApplicationContext());
        return instance;
    }

    public synchronized @Nullable GameState getCurrentGameState() {
        return currentGameState;
    }

    public synchronized void setCurrentGameState(@Nullable GameState gameState) {
        if (Objects.equals(gameState, this.currentGameState)) {
            Log.i(TAG, "Game state unchanged; skipping save.");
            return;
        }
        this.currentGameState = gameState;
        if (gameState == null) {
            sharedPreferences.edit().remove(CURRENT_GAME_STATE_KEY).apply();
        } else {
            String encodedGameState = gameState.encodeAsString();
            Log.i(TAG, "Saving state: " + encodedGameState);
            sharedPreferences.edit().putString(CURRENT_GAME_STATE_KEY, encodedGameState).apply();
        }
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
            GameState gameState, int aiPlayer, @Nullable AiConfig aiConfig, boolean isCampaign) {
        if (aiPlayer < 0 || aiPlayer > 2) {
            throw new IllegalArgumentException("aiPlayer must be 0, 1 or 2");
        }
        if (aiPlayer != 0 && aiConfig == null) {
            throw new IllegalArgumentException("aiConfig must not be null when aiPlayer is nonzero");
        }
        if (isCampaign && aiPlayer == 0) {
            throw new IllegalArgumentException("aiPlayer must be nonzero when isCampaign is true");
        }

        currentGameState = gameState;
        currentGameAiPlayer = aiPlayer;
        currentGameAiConfig = aiConfig;
        currentGameIsCampaign = isCampaign;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(CURRENT_GAME_STATE_KEY, gameState.encodeAsString());
        editor.putInt(CURRENT_GAME_AI_PLAYER_KEY, aiPlayer);
        if (aiConfig == null) {
            editor.remove(CURRENT_GAME_AI_CONFIG_KEY);
        } else {
            editor.putString(CURRENT_GAME_AI_CONFIG_KEY, aiConfig.encodeAsString());
        }
        editor.putBoolean(CURRENT_GAME_IS_CAMPAIGN_KEY, isCampaign);
        editor.apply();
    }
}
