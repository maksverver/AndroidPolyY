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

        gameRegistry = GameRegistry.getInstance();
        changeState(new GameStateWithSelection(gameRegistry.getCurrentGameState()));
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
