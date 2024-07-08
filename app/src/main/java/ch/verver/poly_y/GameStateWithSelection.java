package ch.verver.poly_y;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import java.util.Objects;

/** Immutable pair of a game state and an optional selected field. */
public class GameStateWithSelection {
    public static final GameStateWithSelection DUMMY_GAME_STATE_WITH_SELECTION
            = new GameStateWithSelection(GameState.DUMMY_GAME_STATE);

    public final GameState gameState;
    public final @Nullable BoardGeometry.Vertex selection;

    public GameStateWithSelection(GameState gameState) {
        this(gameState, null);
    }

    public GameStateWithSelection(GameState gameState, @Nullable BoardGeometry.Vertex selection) {
        this.gameState = gameState;
        this.selection = selection;
    }

    GameStateWithSelection clearSelection() {
        return selection == null ? this : new GameStateWithSelection(gameState, null);
    }

    @CheckResult
    GameStateWithSelection toggleSelection(BoardGeometry.Vertex v) {
        return new GameStateWithSelection(gameState, v.equals(selection) ? null : v);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof GameStateWithSelection)) return false;
        GameStateWithSelection other = (GameStateWithSelection) obj;
        return Objects.equals(gameState, other.gameState) && Objects.equals(selection, other.selection);
    }

    @Override
    public int hashCode() {
        return gameState.hashCode() + 31 * (selection == null ? -1 : selection.hashCode());
    }
}
