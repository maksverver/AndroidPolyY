package ch.verver.poly_y;

import android.util.Log;

import androidx.annotation.Nullable;

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

    public interface AiMoveCallback {
        void move(BoardGeometry.Vertex vertex, float winProbability);
    }

    public interface AiProgressCallback {
        void updateProgress(int percent);
    }

    /**
     * <p>Schedules a background task to calculate an AI move.
     *
     * <p>Note that callbacks are called on the background thread!
     *
     * @param gameState the game (in progress)
     * @param config the AI configuration
     * @param moveCallback callback that receives the selected move
     * @param progressCallback callback that is called periodically to report progress
     * @throws IllegalArgumentException if the game is over
     */
    void requestAiMove(GameState gameState, AiConfig config,
            AiMoveCallback moveCallback, @Nullable AiProgressCallback progressCallback) {
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
                    TreeBot.Tree tree = new TreeBot().createTree(ccMovesPlayed);
                    if (progressCallback == null) {
                        expand(tree, config.iterations);
                    } else {
                        expandWithProgress(tree, config.iterations, progressCallback);
                    }
                    TreeBot.BestMove ccBestMove = tree.getBestMove();
                    ccMove = ccBestMove.move;
                    winProbability = ccBestMove.winProbability;
                    assert ccMove != 0;
                }
                bestMove = geometry.codeCupIdToVertex(ccMove);
            }
            long durationMs = System.currentTimeMillis() - startMs;
            Log.i(TAG, "AI selected move " + bestMove.id + " with win probability " + winProbability + " in " + durationMs + " ms");
            try {
                moveCallback.move(bestMove, winProbability);
            } catch (Throwable t) {
                Log.e(TAG, "callback failed", t);
            }
        });
    }

    private static void expand(TreeBot.Tree tree, long iterations) {
        while (iterations-- > 0) tree.expand();
    }

    private static void expandWithProgress(TreeBot.Tree tree, long iterations, AiProgressCallback callback) {
        long i = 0;
        for (int percent = 1 ; percent <= 100; ++percent) {
            long j = iterations * percent / 100;
            while (i < j) {
                tree.expand();
                ++i;
            }
            callback.updateProgress(percent);
        }
        // Should hold unless the `iterations * percent` overflowed!
        assert i == iterations;
    }
}
