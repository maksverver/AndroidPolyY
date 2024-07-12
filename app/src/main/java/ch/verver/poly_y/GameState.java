package ch.verver.poly_y;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/** Immutable representation of a Poly-Y game in progress. */
public final class GameState {
    private static final int[] NO_MOVES = new int[0];

    public static final GameState DUMMY_GAME_STATE = calculate(BoardGeometry.DUMMY_GEOMETRY, false);
    public static final GameState DEFAULT_GAME_STATE = calculate(BoardGeometry.DEFAULT_GEOMETRY);

    public static int otherPlayer(int player) {
        switch (player) {
            case 0: return 0;
            case 1: return 2;
            case 2: return 1;
            default:
                throw new IllegalArgumentException("Invalid player");
        }
    }

    public static GameState calculate(BoardGeometry geometry) {
        return calculate(geometry, true);
    }

    public static GameState calculate(BoardGeometry geometry, boolean canSwap) {
        return calculate(geometry, canSwap, NO_MOVES);
    }

    public static GameState calculate(BoardGeometry geometry, boolean canSwap, int[] moves) {
        return calculate(geometry, canSwap, moves, false);
    }

    public static GameState calculate(BoardGeometry geometry, boolean canSwap, int[] moves, boolean resigned) {
        byte[] pieces = new byte[geometry.vertices.size()];
        byte[] cornerWinners = new byte[geometry.sides];
        int[] scores = new int[3];
        int winner = 0;

        if (moves.length > 0) {
            // Place pieces on the board, one by one.
            {
                int player = 1;
                for (int move : moves) {
                    pieces[move] = (byte) player;
                    player = otherPlayer(player);
                }
            }

            // Determine which sides are captured by which players.
            //
            // This is calculated by flood filling all connected groups. This is a little expensive to
            // do every time a game state is constructed, and it could be optimized with something like
            // an immutable Union-Find data structure, but it's not really worth the trouble for now.
            BoardGeometry.Vertex[] queue = new BoardGeometry.Vertex[moves.length];
            boolean[] marked = new boolean[geometry.vertices.size()];
            for (BoardGeometry.Vertex v : geometry.vertices) {
                byte player = pieces[v.id];
                if (player != 0 && !marked[v.id]) {
                    int pos = 0, len = 0;
                    marked[v.id] = true;
                    queue[len++] = v;
                    int sidesMask = 0;
                    while (pos < len) {
                        v = queue[pos++];
                        sidesMask |= v.sidesMask;
                        for (BoardGeometry.Vertex w : v.neighbors) {
                            if (pieces[w.id] == player && !marked[w.id]) {
                                marked[w.id] = true;
                                queue[len++] = w;
                            }
                        }
                    }
                    if (hasAtLeast3Bits(sidesMask)) {
                        for (int i = 0; i < geometry.sides; ++i) {
                            if ((sidesMask & (1 << i)) != 0 &&
                                    (sidesMask & (1 << (i == 0 ? geometry.sides - 1 : i - 1))) != 0) {
                                if (cornerWinners[i] != 0) {
                                    // The board geometry should make this impossible!
                                    throw new RuntimeException("Internal error: multiple winners of the same corner");
                                }
                                cornerWinners[i] = player;
                            }
                        }
                    }
                }
            }
        }

        // Calculate the score for each player (the number of captured corners).
        for (int i = 0; i < geometry.sides; ++i) {
            scores[cornerWinners[i]]++;
        }

        // Calculate the winner: the player that has captured more than half the corners.
        for (int player = 1; player < scores.length; ++player) {
            if (2*scores[player] > cornerWinners.length) {
                winner = player;
                break;
            }
        }

        if (winner != 0 && resigned) {
            throw new IllegalArgumentException("Player cannot resign when the game is already won!");
        }

        return new GameState(geometry, canSwap, moves, resigned, pieces, cornerWinners, scores, winner);
    }

    // Input parameters
    private final BoardGeometry geometry;
    private final boolean canSwap;
    private final int[] moves;
    private final boolean resigned;

    // Derived data (calculated by calculate(), ignored by equals() and hashCode())
    private final byte[] pieces;
    private final byte[] cornerWinners;  // per corner, id of player that captured it
    private final int[] scores;  // number of captured corners, per player
    private final int winner;

    private GameState(
            BoardGeometry geometry, boolean canSwap, int[] moves, boolean resigned,
            byte[] pieces, byte[] cornerWinners, int[] scores, int winner) {
        this.geometry = geometry;
        this.canSwap = canSwap;
        this.moves = moves;
        this.resigned = resigned;
        this.pieces = pieces;
        this.cornerWinners = cornerWinners;
        this.scores = scores;
        this.winner = winner;
    }
    public BoardGeometry getGeometry() {
        return geometry;
    }

    public boolean isGameOver() {
        // The second condition can only happen if geometry.sides is even, and each player has
        // captured half of the corners.
        return winner != 0 || resigned || scores[0] == 0;
    }

    public boolean isResigned() {
        return resigned;
    }

    /** Returns the next player (1 or 2), or 0 if the game is over. */
    public int getNextPlayer() {
        return isGameOver() ? 0 : (moves.length & 1) + 1;
    }

    public @Nullable BoardGeometry.Vertex getLastMove() {
        return moves.length == 0 ? null : geometry.vertices.get(moves[moves.length - 1]);
    }

    /** Returns the color of the piece on the field; either 1 or 2, or 0 if empty. */
    public int getPiece(BoardGeometry.Vertex v) {
        return pieces[v.id];
    }

    /**
     * Returns whether it's possible to swap with the opponent's first move, which is true if this
     * is the second move, and the pie rule is in effect. The first move can be retrieved by
     * {@link #getLastMove()}.
     */
    public boolean canSwap() {
        return canSwap && this.moves.length == 1;
    }

    /**
     * Returns the winner (player 1 or 2) if the game is over, or 0 if the game is not over.
     *
     * <p>If the game is over, it is possible that getWinner() returns 0 only if geometry.sides()
     * is even, and each player captured half of the corners.
     */
    public int getWinner() {
        return resigned ? 2 - (moves.length & 1)  : winner;
    }

    /**
     * Returns the number of corners captured by the given player (1 or 2), or pass 0 to get
     * the number of corners that have not yet been captured.
     */
    public int getScore(int player) {
        return scores[player];
    }

    /**
     * Returns which player captured the given corner (1 or 2, or 0 if neither).
     *
     * <p>Sides are ordered in clockwise order. The i-th corner connects sides (i-1) and i, where -1
     * corresponds to {@code geometry.sides - 1}.
     */
    public int getCornerWinner(int corner) {
        return this.cornerWinners[corner];
    }

    public boolean isValidMove(BoardGeometry.Vertex v) {
        if (getNextPlayer() == 0) return false;
        return pieces[v.id] == 0 || canSwap();
    }

    @CheckResult
    public GameState move(BoardGeometry.Vertex v) {
        if (!isValidMove(v)) throw new IllegalArgumentException("Invalid move");

        int[] newMoves = new int[moves.length + 1];
        System.arraycopy(moves, 0, newMoves, 0, moves.length);
        newMoves[moves.length] = v.id;

        return calculate(geometry, canSwap, newMoves);
    }

    @CheckResult
    public GameState resign() {
        int nextPlayer = getNextPlayer();
        if (nextPlayer == 0) {
            throw new IllegalArgumentException("Cannot resign when the game is over");
        }
        return new GameState(geometry, canSwap, moves, true, pieces, cornerWinners, scores, winner);
    }

    public ArrayList<Integer> getCodeCupMoves() {
        ArrayList<Integer> res = new ArrayList<>();
        for (int i = 0; i < moves.length; ++i) {
            if (i == 1 && moves[1] == moves[0]) {
                res.add(-1);  // Swap
            } else {
                res.add(geometry.vertexIdToCodeCupId(moves[i]));
            }
        }
        return res;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof GameState)) return false;
        GameState other = (GameState) obj;
        return geometry.equals(other.geometry) &&
                canSwap == other.canSwap &&
                Arrays.equals(moves, other.moves) &&
                resigned == other.resigned;
    }

    @Override
    public int hashCode() {
        return Objects.hash(geometry.hashCode(), canSwap, Arrays.hashCode(moves), resigned);
    }

    /**
     * Encodes the game state as a string that consists of comma-separated integers.
     *
     * <p>The format is: "version,boardSize,sides,pieRule,resigned,move1,move2,..,moveN",
     * where version is currently 1.
     */
    public String encodeAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(1);  // version number
        sb.append(',');
        sb.append(geometry.boardSize);
        sb.append(',');
        sb.append(geometry.sides);
        sb.append(',');
        sb.append(canSwap ? 1 : 0);
        sb.append(',');
        sb.append(resigned ? 1 : 0);
        for (int move : moves) {
            sb.append(',');
            sb.append(move);
        }
        return sb.toString();
    }

    /**
     * Decodes the game state string created with {@link #encodeAsString()}.
     *
     * <p>This isn't safe to run on untrusted input! No attempt is made to ensure that the
     * parameters are within a reasonable range and/or that the moves are valid.
     *
     * @throws IllegalArgumentException if the string could not be parsed
     */
    public static GameState decodeFromString(String s) {
        String[] parts = s.split(",");
        int[] ints = new int[parts.length];
        for (int i = 0; i < parts.length; ++i) {
            ints[i] = Integer.parseInt(parts[i]);
        }
        if (ints.length < 5) throw new IllegalArgumentException("Not enough parts");
        int version = ints[0];
        if (version != 1) throw new IllegalArgumentException("Unsupported version number: " + version);
        int boardSize = ints[1];
        int sides = ints[2];
        boolean canSwap = ints[3] != 0;
        boolean resigned = ints[4] != 0;
        int[] moves = Arrays.copyOfRange(ints, 5, ints.length);
        return calculate(new BoardGeometry(boardSize, sides), canSwap, moves, resigned);
    }

    private static boolean hasAtLeast3Bits(int x) {
        assert x >= 0;
        x &= x - 1;
        x &= x - 1;
        return x != 0;
    }
}
