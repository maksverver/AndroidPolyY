package ch.verver.poly_y;

import androidx.annotation.Nullable;

/** Immutable parameters used to invoke AI. */
public final class AiConfig {

    public static final AiConfig HINT_CONFIG = new AiConfig(1000, false);

    public static final int MIN_DIFFICULTY = 1;
    public static final int MEDIUM_DIFFICULTY = 5;
    public static final int HARD_DIFFICULTY = 10;
    public static final int MAX_DIFFICULTY = 15;

    /**
     * Create an AI config from a difficulty level.
     *
     * <p>Level 5 is a reasonable starting level. Level 10 is very difficult.
     */
    static AiConfig fromDifficulty(int difficulty) {
        // The opening book is used starting at level 10.
        return fromDifficulty(difficulty, difficulty >= HARD_DIFFICULTY);
    }

    static AiConfig fromDifficulty(int difficulty, boolean useOpeningBook) {
        if (difficulty < MIN_DIFFICULTY || difficulty > MAX_DIFFICULTY) {
            throw new IllegalArgumentException("Difficulty must be between " + MIN_DIFFICULTY + " and " + MAX_DIFFICULTY);
        }
        // iterations == pow(2, difficulty), so:
        //  - level  1:     4 iterations
        //  - level  5:    64 iterations
        //  - level 10:  2048 iterations (1~2 seconds)
        //  - level 15: 65536 iterations (30~60 seconds).
        // There are 32 playouts per iteration, so even level 1 is not completely random.
        int iterations = 2 << difficulty;
        return new AiConfig(iterations, useOpeningBook);
    }

    /**
     * Number of times to expand the search tree in the MCTS algorithm. Runtime grows linearly
     * with this value, while playing strength grows logarithmically. Typical performance is around
     * 2000 iterations per second.
     */
    final long iterations;

    /**
     * Whether to use the opening book. This determines the first few moves in the game, and
     * greatly increases the strength of opening moves, regardless of the {@link #iterations}
     * setting. Typically it only makes sense to set this when {@link #}iterations} is high, e.g.
     * higher than 1000, otherwise the play strength is very uneven between different phases of the
     * game.
     */
    final boolean openingBook;

    AiConfig(long iterations, boolean openingBook) {
        if (iterations < 1) throw new IllegalArgumentException("iterations must be at least 1");
        this.iterations = iterations;
        this.openingBook = openingBook;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AiConfig)) return false;
        AiConfig other = (AiConfig) obj;
        return iterations == other.iterations && openingBook == other.openingBook;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(iterations) * 31 + Boolean.hashCode(openingBook);
    }

    /**
     * Encodes the config as a string that consists of comma-separated integers.
     *
     * <p>The format is: "version,iterations,openingBook", where version is currently 1.
     */
    public String encodeAsString() {
        return "1," + iterations + "," + (openingBook ? 1 : 0);
    }

    /**
     * Decodes the game state string created with {@link #encodeAsString()}.
     *
     * @throws IllegalArgumentException if the string could not be parsed
     */
    public static AiConfig decodeFromString(String s) {
        String[] parts = s.split(",");
        int[] ints = new int[parts.length];
        for (int i = 0; i < parts.length; ++i) {
            ints[i] = Integer.parseInt(parts[i]);
        }
        if (ints.length < 3) throw new IllegalArgumentException("Not enough parts");
        int version = ints[0];
        if (version != 1) throw new IllegalArgumentException("Unsupported version number: " + version);
        int iterations = ints[1];
        int openingBook = ints[2];
        return new AiConfig(iterations, openingBook != 0);
    }
}
