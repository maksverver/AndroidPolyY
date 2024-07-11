package ch.verver.poly_y;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

public class GameActivity extends Activity {
    private static final String TAG = "GameActivity";

    private GameRegistry gameRegistry;
    private TextView statusTextView;
    private Button resignButton;
    private Button hintButton;
    private Button confirmButton;
    private GameView gameView;
    private GameStateWithSelection state;
    private boolean hintInProgress = false;
    private boolean aiInProgress = false;
    private int aiPlayer = 0;
    private @Nullable AiConfig aiConfig = null;

    @MainThread
    private void changeState(GameStateWithSelection newState) {
        state = newState;
        gameView.setGameState(state);
        resignButton.setText(
                getString(state.gameState.isGameOver() ? R.string.game_button_back : R.string.game_button_resign));
        resignButton.setEnabled(!aiInProgress);
        confirmButton.setEnabled(state.selection != null);
        statusTextView.setText(getStatusText(state.gameState));
        gameRegistry.setCurrentGameState(state.gameState);
        maybeTriggerAiMove();
        // Do this last, because hint button is disabled when AI is in progress.
        updateHintButton();
    }

    private String getStatusText(GameState gameState) {
        String p1 = getString(
                aiPlayer == 0 ? R.string.game_status_name_player_1 :
                        aiPlayer == 1 ? R.string.game_status_name_ai : R.string.game_status_name_player)
                + " (" + getString(R.string.game_status_color_player_1) + ")";
        String p2 = getString(
                aiPlayer == 0 ? R.string.game_status_name_player_2 :
                        aiPlayer == 2 ? R.string.game_status_name_ai : R.string.game_status_name_player)
                + " (" + getString(R.string.game_status_color_player_2) + ")";

        switch (gameState.getNextPlayer()) {
            case 0:
                // Game is over
                switch (gameState.getWinner()) {
                    case 0:
                        return getString(R.string.game_status_tied);
                    case 1:
                        return gameState.isResigned() ?
                            p2 + " " + getString(R.string.game_status_resigned) :
                            p1 + " " + getString(R.string.game_status_won);
                    case 2:
                        return gameState.isResigned() ?
                            p1 + " " + getString(R.string.game_status_resigned) :
                            p2 + " " + getString(R.string.game_status_won);
                    default: return getString(R.string.game_status_game_over);
                }
            case 1:
                return p1 + " " + getString(R.string.game_status_to_move);
            case 2:
                return p2 + " " + getString(R.string.game_status_to_move);
            default:
                return getString(R.string.game_status_invalid_state);
        }
    }

    @MainThread
    private void updateHintButton() {
        hintButton.setEnabled(
                !state.gameState.isGameOver() && !hintInProgress && !aiInProgress &&
                BoardGeometry.DEFAULT_GEOMETRY.equals(state.gameState.getGeometry()));
    }

    private void maybeTriggerAiMove() {
        if (aiPlayer == 0 || aiConfig == null || aiInProgress) return;
        final GameState originalGameState = state.gameState;
        if (originalGameState.getNextPlayer() == aiPlayer) {
            aiInProgress = true;
            AiManager.getInstance().requestAiMove(originalGameState, aiConfig, (move, probability) -> {
                runOnUiThread(() -> {
                    aiInProgress = false;
                    if (!originalGameState.equals(state.gameState)) {
                        // This should not happen normally; but just to be sure.
                        Log.w(TAG, "Game state has changed! ");
                        maybeTriggerAiMove();
                    } else {
                        changeState(new GameStateWithSelection(state.gameState.move(move)));
                    }
                });
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the layout and connect views.
        setContentView(R.layout.game_layout);
        statusTextView = findViewById(R.id.statusTextView);
        resignButton = findViewById(R.id.resignButton);
        resignButton.setOnClickListener(this::onResignButtonClick);
        hintButton = findViewById(R.id.hintButton);
        hintButton.setOnClickListener(this::onHintButtonClick);
        confirmButton = findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(this::onConfirmButtonClick);
        gameView = findViewById(R.id.gameView);
        gameView.addFieldClickListener(this::onFieldClicked);

        gameRegistry = GameRegistry.getInstance(this);

        // Support overriding the application state using extras passed in the intent that launched
        // the activity. This is useful for debugging (see DEVELOPMENT.txt).
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String gameStateString = extras.getString("override_game_state");
            if (gameStateString != null) {
                try {
                    changeState(new GameStateWithSelection(GameState.decodeFromString(gameStateString)));
                    Log.i(TAG, "Overriding game state from intent extra!");
                } catch (Exception e) {
                    Log.e(TAG, "Could not parse game state string; skipping game state override.", e);
                }
            }
        }

        // state != null only if we've initialized the game state from the Intent extras above.
        if (state == null) {
            aiPlayer = gameRegistry.getCurrentGameAiPlayer();
            aiConfig = gameRegistry.getCurrentGameAiConfig();
            GameState gameState = gameRegistry.getCurrentGameState();
            if (gameState == null || (aiPlayer != 0 && aiConfig == null)) {
                Log.w(TAG, "Invalid game configuration!");
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return;
            }
            changeState(new GameStateWithSelection(gameState));
        }
    }

    private void onResignButtonClick(View unusedView) {
        if (!state.gameState.isGameOver()) {
            changeState(new GameStateWithSelection(state.gameState.resign()));
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void onHintButtonClick(View unusedView) {
        hintButton.setEnabled(false);
        hintInProgress = true;
        final GameState originalGameState = state.gameState;
        AiManager.getInstance().requestAiMove(originalGameState, AiConfig.HINT_CONFIG, (move, probability) -> {
            runOnUiThread(() -> {
                hintInProgress = false;
                if (!originalGameState.equals(state.gameState)) {
                    Log.w(TAG, "Game state has changed! Ignoring AI hint.");
                    updateHintButton();
                } else {
                    changeState(state.setSelection(move));
                }
            });
        });
    }

    private void onConfirmButtonClick(View unusedView) {
        if (state.selection == null) {
            Log.w(TAG, "Confirm button clicked but no selection active!");
        } else if (!state.gameState.isValidMove(state.selection)) {
            Log.w(TAG, "Selected move is not valid!");
        } else {
            changeState(new GameStateWithSelection(state.gameState.move(state.selection)));
        }
    }

    private void onFieldClicked(BoardGeometry.Vertex v) {
        if (!aiInProgress && state.gameState.isValidMove(v)) {
            changeState(state.toggleSelection(v));
        }
    }
}
