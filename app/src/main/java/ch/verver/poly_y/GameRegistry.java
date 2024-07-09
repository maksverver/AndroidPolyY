package ch.verver.poly_y;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Objects;

public class GameRegistry {
    private static final String SHARED_PREFERENCES_NAME = "poly_y_prefs";
    private static final String CURRENT_GAME_KEY = "current_game";

    private final static String TAG = "GameRegistry";
    private static @Nullable GameRegistry instance;

    private final SharedPreferences sharedPreferences;
    private @Nullable GameState currentGameState;

    GameRegistry(Context applicationContext) {
        sharedPreferences = applicationContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        // Load the current game. Maybe TODO: lazy-load instead?
        String currentGameString = sharedPreferences.getString(CURRENT_GAME_KEY, null);
        if (currentGameString != null) {
            try {
                currentGameState = GameState.decodeFromString(currentGameString);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode GameState string!", e);
                // continue anyway -- this means we lose the game state, but at least the player
                // can start a new game, which is better than crashing.
            }
        }
    }

    public static synchronized GameRegistry getInstance(Context applicationContext) {
        if (instance == null) instance = new GameRegistry(applicationContext);
        return instance;
    }

    public synchronized @Nullable GameState getCurrentGameState() {
        return currentGameState;
    }

    public synchronized void saveCurrentGameState(@Nullable GameState gameState) {
        if (Objects.equals(gameState, currentGameState)) {
            Log.i(TAG, "Game state unchanged; skipping save.");
            return;
        }
        currentGameState = gameState;
        if (gameState == null) {
            sharedPreferences.edit().remove(CURRENT_GAME_KEY).apply();
        } else {
            String encodedGameState = gameState.encodeAsString();
            Log.i(TAG, "Saving state: " + encodedGameState);
            sharedPreferences.edit().putString(CURRENT_GAME_KEY, encodedGameState).apply();
        }
    }
}
