package ch.verver.poly_y;

import android.util.Log;

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
        void onAiMove(BoardGeometry.Vertex vertex, double winProbability);
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
            BoardGeometry geometry = gameState.getGeometry();
            double winProbability = 0.5;
            BoardGeometry.Vertex lastMove = gameState.getLastMove();
            BoardGeometry.Vertex bestMove;
            if (lastMove != null && gameState.canSwap() &&
                    TreeBot.shouldSwap(geometry.vertexToCodeCupId(lastMove))) {
                bestMove = lastMove;
            } else {
                // Note: we create a new bot for each request, instead of updating the game state. This
                // reduces performance, since we cannot reuse information from a subtree, but it makes
                // the code simpler and the performance more consistent.
                // TODO: opening book support!
                // TODO: calculate winProbability too
                TreeBot treeBot = new TreeBot();
                bestMove = geometry.codeCupIdToVertex(
                        treeBot.findBestMoveInIterations(gameState.getCodeCupMoves(), config.iterations));
            }
            try {
                callback.onAiMove(bestMove, winProbability);
            } catch (Throwable t) {
                Log.e(TAG, "callback failed", t);
            }
        });
    }
}
