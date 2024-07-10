package ch.verver.poly_y;

import androidx.annotation.Nullable;

import java.util.Objects;

/** Immutable parameters used to invoke AI. */
public final class AiConfig {

    static AiConfig HINT_CONFIG = new AiConfig(1000, false);

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
}
