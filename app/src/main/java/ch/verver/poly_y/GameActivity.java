package ch.verver.poly_y;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class GameActivity extends Activity {
    private static final String TAG = "GameActivity";

    private GameRegistry gameRegistry;
    private GameView gameView;
    private Button confirmButton;
    private GameStateWithSelection state;

    private final GameView.FieldClickListener fieldClickListener = (BoardGeometry.Vertex v) -> {
        // TODO: only toggle selection if it is the player's turn!
        if (state.gameState.canMove(v)) {
            changeState(state.toggleSelection(v));
        }
    };

    private void changeState(GameStateWithSelection newState) {
        state = newState;
        gameView.setGameState(state);
        confirmButton.setEnabled(state.selection != null);
        gameRegistry.saveCurrentGameState(state.gameState);
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
        gameView = findViewById(R.id.gameView);
        confirmButton = findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener((View unused) -> {
            if (state.selection == null) {
                Log.w(TAG, "Confirm button clicked but no selection active!");
            } else if (!state.gameState.canMove(state.selection)) {
                Log.w(TAG, "Selected move is not valid!");
            } else {
                changeState(new GameStateWithSelection(state.gameState.move(state.selection)));
            }
        });

        gameRegistry = GameRegistry.getInstance(getApplicationContext());
        GameState gameState = overrideGameState != null ? overrideGameState : gameRegistry.getCurrentGameState();
        if (gameState == null) {
            // Start a new game. This isn't atomic, but whatever.
            gameState = GameState.DEFAULT_GAME_STATE;
            gameRegistry.saveCurrentGameState(gameState);
        }
        changeState(new GameStateWithSelection(gameState));
    }

    @Override
    protected void onStart() {
        super.onStart();
        gameView.addFieldClickListener(fieldClickListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        gameView.removeFieldClickListener(fieldClickListener);
    }
}
