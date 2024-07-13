package ch.verver.poly_y;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public class GameActivity extends Activity {
    private static final String TAG = "GameActivity";

    /** AI will offer to resign when win probability is below this threshold. */
    private static final float RESIGNATION_THRESHOLD = 0.05f;

    /** Number of moves to play after resignation is refused, before offering again. */
    private static final int SUPPRESS_AI_RESIGNATION = 5;

    private GameRegistry gameRegistry;
    private TextView statusTextView;
    private Button backButton;
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
    private int suppressAiResignation = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the layout and connect views.
        setContentView(R.layout.game_layout);
        statusTextView = findViewById(R.id.statusTextView);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(this::onBackButtonClick);
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
        changeState(gameState);
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

    private void onBackButtonClick(View unusedView) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void onResignButtonClick(View unusedView) {
        changeState(state.gameState.resign());
    }

    private void onHintButtonClick(View unusedView) {
        hintButton.setEnabled(false);
        hintInProgress = true;
        final GameState originalGameState = state.gameState;
        requestAiMove(AiConfig.HINT_CONFIG, (move, probability) -> {
            hintInProgress = false;
            if (!originalGameState.equals(state.gameState)) {
                Log.w(TAG, "Game state has changed! Ignoring AI hint.");
                updateHintButton();
            } else {
                changeStateWithSelection(state.setSelection(move));
            }
        });
    }

    private void onConfirmButtonClick(View unusedView) {
        if (state.selection == null) {
            Log.w(TAG, "Confirm button clicked but no selection active!");
        } else if (!state.gameState.isValidMove(state.selection)) {
            Log.w(TAG, "Selected move is not valid!");
        } else {
            changeState(state.gameState.move(state.selection));
        }
    }

    private void onFieldClicked(BoardGeometry.Vertex v) {
        if (isPlayerTurn() && state.gameState.isValidMove(v)) {
            changeStateWithSelection(state.toggleSelection(v));
        }
    }

    private boolean isPlayerTurn() {
        int player = state.gameState.getNextPlayer();
        return player != 0 && player != aiPlayer;
    }

    private boolean isAiTurn() {
        int player = state.gameState.getNextPlayer();
        return player != 0 && player == aiPlayer;
    }

    private void updateHintButton() {
        hintButton.setEnabled(
                !hintInProgress && !inCampaign && isPlayerTurn() &&
                        BoardGeometry.DEFAULT_GEOMETRY.equals(state.gameState.getGeometry()));
    }

    private void changeState(GameState newState) {
        changeStateWithSelection(new GameStateWithSelection(newState));
    }

    private void changeStateWithSelection(GameStateWithSelection newState) {
        state = newState;
        gameView.setGameState(state);
        backButton.setVisibility(state.gameState.isGameOver() ? View.VISIBLE : View.GONE);
        resignButton.setVisibility(state.gameState.isGameOver() ? View.GONE : View.VISIBLE);
        resignButton.setEnabled(isPlayerTurn());
        confirmButton.setEnabled(state.selection != null);
        statusTextView.setText(getStatusText(state.gameState));
        gameRegistry.setCurrentGameState(state.gameState);
        updateHintButton();
        maybeTriggerAiMove();
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

    private void maybeTriggerAiMove() {
        if (!isAiTurn()) return;
        final GameState originalGameState = state.gameState;
        if (originalGameState.getNextPlayer() == aiPlayer) {
            requestAiMove(aiConfig, (move, probability) -> {
                if (!originalGameState.equals(state.gameState)) {
                    // This should not happen normally; but just to be sure, retrigger AI.
                    Log.w(TAG, "Game state has changed! ");
                    maybeTriggerAiMove();
                    return;
                }

                if (probability >= RESIGNATION_THRESHOLD || suppressAiResignation > 0) {
                    // Play the AI move normally.
                    changeState(originalGameState.move(move));
                    if (suppressAiResignation > 0) --suppressAiResignation;
                } else {
                    // Win probability is low. Offer the AI's resignation. If the player accepts,
                    // the game ends. If the player refuses, the move is played and the game
                    // continues normally.
                    final AtomicBoolean accepted = new AtomicBoolean(false);
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.ai_offers_resignation)
                            .setPositiveButton("Accept", (dialog, which) -> accepted.set(true))
                            .setNegativeButton("Refuse", (dialog, which) -> accepted.set(false))
                            .setOnDismissListener((dialog) -> {
                                assert originalGameState.equals(state.gameState);
                                if (accepted.get()) {
                                    changeState(originalGameState.resign());
                                } else {
                                    changeState(originalGameState.move(move));
                                    suppressAiResignation = SUPPRESS_AI_RESIGNATION;
                                }
                            })
                            .create()
                            .show();
                }
            });
        }
    }

    private void requestAiMove(AiConfig config, AiManager.AiMoveCallback callback) {
        progressBar.setProgress(0);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        AiManager.getInstance().requestAiMove(
                state.gameState,
                config,
                (move, probability) -> {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(ProgressBar.INVISIBLE);
                        callback.move(move, probability);
                    });
                },
                (percent) -> {
                    runOnUiThread(() -> progressBar.setProgress(percent));
                });
    }
}
