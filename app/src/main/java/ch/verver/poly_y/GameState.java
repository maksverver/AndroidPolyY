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

    private GameState(BoardGeometry geometry, int[] moves) {
        this.geometry = geometry;
        this.moves = moves;
        this.pieces = new byte[geometry.vertices.size()];
        byte player = 1;
        for (int move : moves) {
            this.pieces[move] = player;
            player = (byte) (3 - player);
        }
        // TODO: calculate winner etc.
    }

    /** Creates an empty board with the given geometry, and player 1 to move. */
    public static GameState createInitial(BoardGeometry geometry) {
        return new GameState(geometry, NO_MOVES);
    }

    public BoardGeometry getGeometry() {
        return geometry;
    }

    /** Returns the next player (1 or 2), or 0 if the game is over. */
    public int getNextPlayer() {
        // TODO: check game over!
        return (moves.length & 1) + 1;
    }

    public @Nullable BoardGeometry.Vertex getLastMove() {
        return moves.length == 0 ? null : geometry.vertices.get(moves[moves.length - 1]);
    }

    /** Returns the color of the piece on the field; either 1 or 2, or 0 if empty. */
    public int getPiece(BoardGeometry.Vertex v) {
        return pieces[v.id];
    }

    public boolean canMove(BoardGeometry.Vertex v) {
        if (getNextPlayer() == 0) return false;
        return moves.length < 2 || pieces[v.id] == 0;
    }

    @CheckResult
    public GameState move(BoardGeometry.Vertex v) {
        if (!canMove(v)) throw new IllegalArgumentException("Invalid move");

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
        StringBuffer sb = new StringBuffer();
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
}
