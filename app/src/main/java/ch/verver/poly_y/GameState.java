package ch.verver.poly_y;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import java.util.Arrays;

/** Immutable representation of a Poly-Y game in progress. */
public final class GameState {
    private static final int[] NO_MOVES = new int[0];

    public static final GameState DUMMY_GAME_STATE =
            new GameState(BoardGeometry.DUMMY_GEOMETRY, NO_MOVES);

    public static final GameState DEFAULT_GAME_STATE =
            new GameState(BoardGeometry.DEFAULT_GEOMETRY, NO_MOVES);

    private final BoardGeometry geometry;
    private final int[] moves;
    private final byte[] pieces;
    private final byte[] cornerWinners;  // per corner, id of player that captured it
    private final byte[] scores;  // number of captured corners, per player
    private final int winner;

    private GameState(BoardGeometry geometry, int[] moves) {
        this.geometry = geometry;
        this.moves = moves;
        this.pieces = new byte[geometry.vertices.size()];
        this.cornerWinners = new byte[geometry.sides];
        this.scores = new byte[3];

        // Place pieces on the board, one by one.
        {
            int player = 1;
            for (int move : moves) {
                this.pieces[move] = (byte) player;
                player = 3 - player;
            }
        }

        // Determine which sides are captured by which players.
        //
        // This is calculated by flood filling all connected groups. This is a little expensive to
        // do every time a game state is constructed, and it could be optimized with something like
        // an immutable Union-Find data structure, but it's not really worth the trouble for now.
        {
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
                return;
            }
        }
        winner = 0;
    }

    /** Creates an empty board with the given geometry, and player 1 to move. */
    public static GameState createInitial(BoardGeometry geometry) {
        return new GameState(geometry, NO_MOVES);
    }

    public BoardGeometry getGeometry() {
        return geometry;
    }

    public boolean isGameOver() {
        // The second condition can only happen if geometry.sides is even, and each player has
        // captured half of the corners.
        return winner != 0 || scores[0] == 0;
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
     * Returns the winner (player 1 or 2) if the game is over, or 0 if the game is not over.
     *
     * <p>If the game is over, it is possible that getWinner() returns 0 only if geometry.sides()
     * is even, and each  player captured half of the corners.
     */
    public int getWinner() {
        return winner;
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
        return moves.length < 2 || pieces[v.id] == 0;
    }

    @CheckResult
    public GameState move(BoardGeometry.Vertex v) {
        if (!isValidMove(v)) throw new IllegalArgumentException("Invalid move");

        int[] newMoves = new int[moves.length + 1];
        System.arraycopy(moves, 0, newMoves, 0, moves.length);
        newMoves[moves.length] = v.id;

        return new GameState(geometry, newMoves);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof GameState)) return false;
        GameState other = (GameState) obj;
        return geometry.equals(other.geometry) && Arrays.equals(moves, other.moves);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(moves) + geometry.hashCode();
    }

    /**
     * Encodes the game state as a string that consists of comma-separated integers.
     *
     * <p>The format is: "version,boardSize,sides,moveCount,move1,move2,..,moveN", where version is
     * currently 1.
     */
    public String encodeAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(1);  // version number
        sb.append(',');
        sb.append(geometry.boardSize);
        sb.append(',');
        sb.append(geometry.sides);
        sb.append(',');
        sb.append(moves.length);
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
        if (ints.length < 4) throw new IllegalArgumentException("Not enough parts");
        int version = ints[0];
        if (version != 1) throw new IllegalArgumentException("Unsupported version number: " + version);
        int boardSize = ints[1];
        int sides = ints[2];
        int moveCount = ints[3];
        if (ints.length != moveCount + 4) throw new IllegalArgumentException("Invalid move count: " + moveCount);
        return new GameState(
                new BoardGeometry(boardSize, sides),
                Arrays.copyOfRange(ints, 4, moveCount + 4));
    }

    private static boolean hasAtLeast3Bits(int x) {
        assert x >= 0;
        x &= x - 1;
        x &= x - 1;
        return x != 0;
    }
}
