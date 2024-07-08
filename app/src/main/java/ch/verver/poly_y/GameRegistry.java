package ch.verver.poly_y;

public class GameRegistry {
    private final static GameRegistry instance = new GameRegistry();

    private GameState currentGameState = GameState.createInitial(BoardGeometry.DEFAULT_GEOMETRY);

    public static GameRegistry getInstance() {
        return instance;
    }

    public GameState getCurrentGameState() {
        return currentGameState;
    }

    public void saveCurrentGameState(GameState gameState) {
        currentGameState = gameState;
    }
}
