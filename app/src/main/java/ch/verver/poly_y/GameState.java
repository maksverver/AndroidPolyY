package ch.verver.poly_y;

import androidx.annotation.CheckResult;

/** Immutable representation of a Poly-Y game in progress. */
public class GameState {
    public static final GameState DUMMY_GAME_STATE = createInitial(BoardGeometry.DUMMY_GEOMETRY);

    private final BoardGeometry geometry;
    private final int nextPlayer;
    private final byte[] pieces;
    private final boolean canSwap;

    private GameState(BoardGeometry geometry, int nextPlayer, byte[] pieces, boolean canSwap) {
        this.geometry = geometry;
        this.nextPlayer = nextPlayer;
        this.pieces = pieces;
        this.canSwap = canSwap;
        // TODO: calculate winner etc. (or in move())
    }

    /** Creates an empty board with the given geometry, and player 1 to move. */
    public static GameState createInitial(BoardGeometry geometry) {
        return new GameState(geometry, 1, new byte[geometry.vertices.size()], true);
    }

    public BoardGeometry getGeometry() {
        return geometry;
    }

    /** Returns the next player (1 or 2), or 0 if the game is over. */
    public int getNextPlayer() {
        return nextPlayer;
    }

    /** Returns the color of the piece on the field; either 1 or 2, or 0 if empty. */
    public int getPiece(BoardGeometry.Vertex v) {
        return pieces[v.id];
    }

    public boolean canMove(BoardGeometry.Vertex v) {
        if (nextPlayer == 0) return false;
        return canSwap || pieces[v.id] == 0;
    }

    @CheckResult
    public GameState move(BoardGeometry.Vertex v) {
        if (!canMove(v)) throw new IllegalArgumentException("Invalid move");

        byte[] newPieces = pieces.clone();
        newPieces[v.id] = (byte) nextPlayer;
        return new GameState(geometry, 3 - nextPlayer, newPieces, nextPlayer == 1 && canSwap);
    }

    // TODO: winners etc.
    // TODO: equals, hashCode
}
