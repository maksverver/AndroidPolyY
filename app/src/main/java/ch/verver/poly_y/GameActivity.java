package ch.verver.poly_y;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class GameActivity extends Activity {
    private static final String TAG = "GameActivity";

    private GameRegistry gameRegistry;
    private TextView statusTextView;
    private Button resignButton;
    private Button hintButton;
    private Button confirmButton;
    private GameView gameView;
    private ProgressBar progressBar;

    private int aiPlayer = 0;
    private @Nullable AiConfig aiConfig = null;
    private boolean inCampaign;
    private GameStateWithSelection state;
    private boolean hintInProgress = false;
    private boolean aiInProgress = false;

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

    private void updateHintButton() {
        hintButton.setEnabled(
                !inCampaign && !state.gameState.isGameOver() && !hintInProgress && !aiInProgress &&
                BoardGeometry.DEFAULT_GEOMETRY.equals(state.gameState.getGeometry()));
    }

    private void maybeTriggerAiMove() {
        if (aiPlayer == 0 || aiConfig == null || aiInProgress) return;
        final GameState originalGameState = state.gameState;
        if (originalGameState.getNextPlayer() == aiPlayer) {
            aiInProgress = true;
            progressBar.setProgress(0);
            progressBar.setVisibility(ProgressBar.VISIBLE);
            AiManager.getInstance().requestAiMove(
                    originalGameState,
                    aiConfig,
                    (move, probability) -> {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(ProgressBar.INVISIBLE);
                            aiInProgress = false;
                            if (!originalGameState.equals(state.gameState)) {
                                // This should not happen normally; but just to be sure.
                                Log.w(TAG, "Game state has changed! ");
                                maybeTriggerAiMove();
                            } else {
                                changeState(new GameStateWithSelection(state.gameState.move(move)));
                            }
                        });
                    },
                    this::updateProgressBar);
        }
    }

    private void updateProgressBar(int percent) {
        runOnUiThread(() -> progressBar.setProgress(percent));
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
        progressBar = findViewById(R.id.progressBar);

        gameRegistry = GameRegistry.getInstance(this);

        // This check is necessary to avoid resetting the game state every time the activity is
        // recreated, e.g. when the screen orientation changes.
        if (savedInstanceState == null) {
            processIntent();
        }

        aiPlayer = gameRegistry.getCurrentGameAiPlayer();
        aiConfig = gameRegistry.getCurrentGameAiConfig();
        inCampaign = gameRegistry.getCurrentGameIsCampaign();
        GameState gameState = gameRegistry.getCurrentGameState();
        if (gameState == null || (aiPlayer != 0 && aiConfig == null)) {
            Log.w(TAG, "Invalid game configuration!");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        changeState(new GameStateWithSelection(gameState));
    }

    /**
     * Overrides the game state with one passed as an intent extra. This is useful for debugging
     * (see DEVELOPMENT.txt).
     */
    private void processIntent() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) return;
        String gameStateString = extras.getString("override_game_state");
        if (gameStateString == null) return;
        final GameState gameState;
        try {
            gameState = GameState.decodeFromString(gameStateString);
        } catch (Exception e) {
            Log.e(TAG, "Could not parse game state string; skipping game state override.", e);
            return;
        }
        Log.i(TAG, "Overriding game state from intent extra.");
        gameRegistry.startGame(gameState, 0, null, false);
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
        progressBar.setProgress(0);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        final GameState originalGameState = state.gameState;
        AiManager.getInstance().requestAiMove(
                originalGameState,
                AiConfig.HINT_CONFIG,
                (move, probability) -> {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(ProgressBar.INVISIBLE);
                        hintInProgress = false;
                        if (!originalGameState.equals(state.gameState)) {
                            Log.w(TAG, "Game state has changed! Ignoring AI hint.");
                            updateHintButton();
                        } else {
                            changeState(state.setSelection(move));
                        }
                    });
                },
                this::updateProgressBar);
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
