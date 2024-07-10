package ch.verver.poly_y;

import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.verver.poly_y.ai.TreeBot;

public class AiManager {
    private static final String TAG = "AiManager";

    private static class InstanceHolder {
        public static AiManager instance = new AiManager();
    }

    public static AiManager getInstance() {
        return InstanceHolder.instance;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface AIMoveCallback {
        void onAiMove(BoardGeometry.Vertex vertex, float winProbability);
    }

    /**
     * <p>Schedules a background task to calculate an AI move. When the calculation is complete, the
     * callback will be invoked on the background thread.
     *
     * @param gameState the game (in progress)
     * @param config the AI configuration
     * @param callback callback that receives the selected move; will be run on the AI thread!
     * @throws IllegalArgumentException if the game is over
     */
    void requestAiMove(GameState gameState, AiConfig config, AIMoveCallback callback) {
        if (gameState.isGameOver()) {
            throw new IllegalArgumentException("Game must not be over!");
        }
        if (!BoardGeometry.DEFAULT_GEOMETRY.equals(gameState.getGeometry())) {
            throw new IllegalArgumentException("Geometry not supported by AI!");
        }
        executor.submit(() -> {
            long startMs = System.currentTimeMillis();
            BoardGeometry geometry = gameState.getGeometry();
            BoardGeometry.Vertex lastMove = gameState.getLastMove();
            final BoardGeometry.Vertex bestMove;
            float winProbability = 0.5f;  // unknown probability
            if (lastMove != null && gameState.canSwap() &&
                    TreeBot.shouldSwap(geometry.vertexToCodeCupId(lastMove))) {
                bestMove = lastMove;
            } else {
                ArrayList<Integer> ccMovesPlayed = gameState.getCodeCupMoves();
                int ccMove = config.openingBook ? TreeBot.getOpeningMove(ccMovesPlayed) : 0;
                if (ccMove == 0) {
                    // No opening book move. Run the MCTS algorithm to find a good move.
                    //
                    // Note: we create a new tree from scratch every time, instead of reusing the
                    // subtree from the previous move. This reduces play strength slightly, because
                    // we cannot reuse information from a subtree, but it makes the code simpler.
                    TreeBot.BestMove ccBestMove = new TreeBot().findBestMoveInIterations(ccMovesPlayed, config.iterations);
                    ccMove = ccBestMove.move;
                    winProbability = ccBestMove.winProbability;
                }
                bestMove = geometry.codeCupIdToVertex(ccMove);
            }
            long durationMs = System.currentTimeMillis() - startMs;
            Log.i(TAG, "AI selected move " + bestMove.id + " with win probability " + winProbability + " in " + durationMs + " ms");
            try {
                callback.onAiMove(bestMove, winProbability);
            } catch (Throwable t) {
                Log.e(TAG, "callback failed", t);
            }
        });
    }
}
