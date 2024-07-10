package ch.verver.poly_y;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.MainThread;

public class GameActivity extends Activity {
    private static final String TAG = "GameActivity";

    private GameRegistry gameRegistry;
    private TextView statusTextView;
    private Button hintButton;
    private Button confirmButton;
    private GameView gameView;
    private GameStateWithSelection state;
    private boolean hintInProgress = false;

    private final GameView.FieldClickListener fieldClickListener = (BoardGeometry.Vertex v) -> {
        // TODO: only toggle selection if it is the player's turn!
        if (state.gameState.isValidMove(v)) {
            changeState(state.toggleSelection(v));
        }
    };

    @MainThread
    private void changeState(GameStateWithSelection newState) {
        state = newState;
        gameView.setGameState(state);
        confirmButton.setEnabled(state.selection != null);
        updateHintButton();
        statusTextView.setText(getStatusText(state.gameState));
        gameRegistry.saveCurrentGameState(state.gameState);
    }

    private String getStatusText(GameState gameState) {
        switch (gameState.getNextPlayer()) {
            case 0:
                switch (gameState.getWinner()) {
                    case 0: return getString(R.string.game_status_tied);
                    case 1: return getString(R.string.game_status_player_1_won);
                    case 2: return getString(R.string.game_status_player_2_won);
                    default: return getString(R.string.game_status_game_over);
                }
            case 1: return getString(R.string.game_status_player_1_to_move);
            case 2: return getString(R.string.game_status_player_2_to_move);
            default: return getString(R.string.game_status_invalid_state);
        }
    }

    @MainThread
    private void updateHintButton() {
        // TODO: only enable when it's my turn!
        hintButton.setEnabled(!state.gameState.isGameOver() && state.selection == null && !hintInProgress);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Support overriding the application state using extras passed in the intent that launched
        // the activity. This is useful for debugging (see DEVELOPMENT.txt).
        Bundle extras = getIntent().getExtras();
        GameState overrideGameState = null;
        if (extras != null) {
            String gameStateString = extras.getString("override_game_state");
            if (gameStateString != null) {
                try {
                    overrideGameState = GameState.decodeFromString(gameStateString);
                    Log.i(TAG, "Overriding game state from intent extra!");
                } catch (Exception e) {
                    Log.e(TAG, "Could not parse game state string; skipping game state override.", e);
                }
            }
        }

        // Create the layout and connect views.
        setContentView(R.layout.game_layout);
        statusTextView = findViewById(R.id.statusTextView);
        hintButton = findViewById(R.id.hintButton);
        confirmButton = findViewById(R.id.confirmButton);
        hintButton.setOnClickListener(this::onHintButtonClick);
        confirmButton.setOnClickListener(this::onConfirmButtonClick);
        gameView = findViewById(R.id.gameView);
        gameView.addFieldClickListener(fieldClickListener);

        gameRegistry = GameRegistry.getInstance(getApplicationContext());
        GameState gameState = overrideGameState != null ? overrideGameState : gameRegistry.getCurrentGameState();
        if (gameState == null) {
            // Start a new game. This isn't atomic, but whatever.
            gameState = GameState.DEFAULT_GAME_STATE;
            gameRegistry.saveCurrentGameState(gameState);
        }
        changeState(new GameStateWithSelection(gameState));
    }

    private void onHintButtonClick(View unusedView) {
        hintButton.setEnabled(false);
        hintInProgress = true;
        final GameState originalGameState = state.gameState;
        AiManager.getInstance().requestAiMove(originalGameState, AiConfig.HINT_CONFIG, (move, unusedProbability) -> {
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
}
