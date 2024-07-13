package ch.verver.poly_y.ai;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.verver.poly_y.ai.Patterns.patterns;
import static ch.verver.poly_y.ai.Board.POSITIONS;
import static ch.verver.poly_y.ai.Board.win;
import static ch.verver.poly_y.ai.Board.cornerSet;
import static ch.verver.poly_y.ai.Board.edge;
import static ch.verver.poly_y.ai.Board.neighbours;
import static ch.verver.poly_y.ai.Board.edgeDistance;

/**
 * Code for the Lynx3 bot that won the Codecup competition for the game Poly-Y.
 *
 * <p>This code implements monte carlo tree search with the AMAF heuristic and pattern detection</p>
 *
 * <p>Originally written by Lesley Wevers & Steven te Brinke, adapted by Maks Verver to be used
 * in the Android app.</p>
 *
 * @link <a href="https://github.com/lwevers/lynx/blob/master/Lynx3.java">original source code</a>
 */
public class TreeBot {

    // Simulation heuristics parameters
    private static final int EDGE_BIAS_THRESHOLD = 50;    // Avoid playing at the edge for this number of simulation steps

    // Tree search parameters
    private static final int SAMPLES = 32;                // Number of samples at tree nodes, has a huge effect on performance
    private static final double ALPHA = 0.75;            // Blending parameter for AMAF samples and actual samples in the computation of the score of a node

    // Derived from Random.java, removed synchronization and other checks to improve performance
    private long seed = System.nanoTime();

    public static boolean shouldSwap(int move) {
        // We swap for all symmetries of move 15, and for all center moves (edge distance > 1)
        return edgeDistance[move] > 1 || move == 15 || move == 24 || move == 71 || move == 81 || move == 96 || move == 95 || move == 74 || move == 63 || move == 18 || move == 11;
    }

    public static class BestMove {
        public final int move;
        public final float winProbability;

        BestMove(int move, float winProbability) {
            this.move = move;
            this.winProbability = winProbability;
        }
    }

    public Tree createTree(List<Integer> playedMoves) {
        GameState state = new GameState();
        boolean myTurn = playedMoves.size() % 2 == 0;
        for (int move : playedMoves) {
            if (move == -1) {
                state.swapPlayers();
            } else {
                if (myTurn) {
                    state.updateMyMove(move);
                } else {
                    state.updateOpMove(move);
                }
            }
            myTurn = !myTurn;
        }
        assert myTurn;
        return new Tree(state);
    }

    private int randomInt() {
        seed = seed * 0x5deece66dL + 0xbL;
        return (int) (seed >>> 16);
    }

    private int randomInt(int n) {
        // Not completely correct, as results are a little biased, but good enough for our purpose
        // We shift by 16 because the higher bits provide higher quality randomness
        return (randomInt() >>> 16) % n;
    }

    // Get a move from the opening book based on the moves played so far
    // Returns either the move from the opening book, or 0 if the opening book does not contain the given move sequence
    static public int getOpeningMove(List<Integer> moves) {
        Object[] root = OpeningBook.openingBook;

        // Recursively go through the opening book for every move played so far
        for (int move : moves) {
            if (move == -1) {
                continue;  // swap
            }
            if (root.length > 1) {
                // There is a child opening book, update the root to this child
                root = (Object[]) root[move];
            } else {
                // There is no child opening book, the move sequence is not in the opening book and we are done
                root = null;
            }
            if (root == null) {
                return 0;
            }
        }

        return (Integer) root[0];    // The move sequence is in the opening book, return the move from the opening book
    }

    // This class encodes a game state
    private class GameState {
        // Determine the winner given a board encoded in the bitset given by l and r
        int[] todo = new int[POSITIONS];    // Stack for depth first search
        // The moves that can still be played in this state, up to index 'end' defined below
        private final byte[] remainingMoves;
        // The position of a given move in the remainingMoves array, i.e. positions[i] is the position of move i in the remainingMoves array
        // This array is used to allow O(1) removal of moves from the remainingMoves array
        private final byte[] positions;
        // Moves in remainingMoves starting from index end have already been played
        private int end;
        // Bit sets encoding the board state
        private long myMovesLeft, myMovesRight;    // Moves played by me
        private long opMovesLeft, opMovesRight; // Moves played by the opponent

        // Constructs the initial game state
        public GameState() {
            remainingMoves = new byte[POSITIONS];
            positions = new byte[POSITIONS + 1];
            for (int i = 0; i < POSITIONS; i++) {
                remainingMoves[i] = (byte) (i + 1);
                positions[i + 1] = (byte) i;
            }
            end = POSITIONS;
        }

        // Copy constructor
        public GameState(GameState other) {
            this.remainingMoves = Arrays.copyOf(other.remainingMoves, other.end);
            this.positions = other.positions.clone();
            this.end = other.end;
            this.myMovesLeft = other.myMovesLeft;
            this.myMovesRight = other.myMovesRight;
            this.opMovesLeft = other.opMovesLeft;
            this.opMovesRight = other.opMovesRight;
        }

        // Swaps the moves done by the players
        public void swapPlayers() {
            long mML = myMovesLeft;
            long mMR = myMovesRight;
            myMovesLeft = opMovesLeft;
            myMovesRight = opMovesRight;
            opMovesLeft = mML;
            opMovesRight = mMR;
        }

        // Sets the move at the given index as unavailable for playing
        // Swaps the move at the given index in remainingMoves with the move at remainingMoves[end - 1] and reduces end by one
        private void swapOut(int index) {
            end--;
            swap(index, end);
        }

        // Sets a given move as unavailable for playing
        private void remove(int move) {
            swapOut(positions[move]);
        }

        // Swaps two entries in the remainingMoves array
        public void swap(int i, int j) {
            int temp = remainingMoves[i];
            remainingMoves[i] = remainingMoves[j];
            remainingMoves[j] = (byte) temp;

            // Update the positions of the entries
            positions[remainingMoves[i]] = (byte) i;
            positions[remainingMoves[j]] = (byte) j;
        }

        // Updates the state with my move
        public void updateMyMove(int move) {
            remove(move);
            if (move < 64) {
                myMovesLeft |= 1L << move;
            } else {
                myMovesRight |= 1L << (move - 64);
            }
        }

        // Updates the state with an opponent move
        public void updateOpMove(int move) {
            remove(move);
            if (move < 64) {
                opMovesLeft |= 1L << move;
            } else {
                opMovesRight |= 1L << (move - 64);
            }
        }

        // Monte carlo sampling with the all-moves-as-first (AMAF) heuristic
        public AmafPlayout sample(final boolean myMoveAtStart) {
            AmafPlayout result = new AmafPlayout();

            result.samples += SAMPLES;
            long _cML, _cMR;    // The moves done by the current player
            long _oML, _oMR;    // The moves done by the opponent of the current player
            if (myMoveAtStart) {
                _cML = myMovesLeft;
                _cMR = myMovesRight;
                _oML = opMovesLeft;
                _oMR = opMovesRight;
            } else {
                _cML = opMovesLeft;
                _cMR = opMovesRight;
                _oML = myMovesLeft;
                _oMR = myMovesRight;
            }

            final boolean myMoveAtEnd = myMoveAtStart ^ ((end & 1) == 1);    // Is it my move at the end of the game?
            int[] play = new int[11];    // Stack that encodes possible moves that can be played based on patterns

            // Perform the given number of sample games
            for (int m = 0; m < SAMPLES; m++) {
                // The moves done by the current player
                long cML = _cML;
                long cMR = _cMR;

                // The moves done by the opponent of the current player
                long oML = _oML;
                long oMR = _oMR;

                int end = this.end;        // We use a copy of end to see which moves we can still do in this simulation run
                int lastMove = 0;        // The last move done by the opponent

                while (end > 0) {
                    int move = 0;

                    // Respond to the last opponent move based on patterns
                    if (lastMove != 0) {
                        int pc = 0;                            // Index pointing to the top of the 'play' stack
                        long[] ps = patterns[lastMove];        // Patterns that we have to apply
                        for (int i = 0; i < ps.length; i += 4) {
                            // Check if pattern matches
                            if ((ps[i] & cML) == ps[i] && (ps[i + 1] & cMR) == ps[i + 1] && (ps[i + 2] & oML) == 0 && (ps[i + 3] & oMR) == 0 && !isSet(cML, cMR, (int) (ps[i + 3] >>> 48))) {
                                // Pattern matches, add the move corresponding to this pattern to the play stack
                                play[pc++] = (int) (ps[i + 3] >>> 48);
                            }
                        }

                        if (pc > 0) {
                            // Select a random move from the play stack
                            move = play[randomInt(pc)];
                        }
                    }

                    // If the patterns did not result in a move, do a random move
                    if (move == 0) {
                        move = remainingMoves[randomInt(end)];

                        // Bias moves early in the game away from the edges, the idea is that more patterns will develop than fully random play
                        if (end > 96) {
                            // At the beginning of the game (first 10 moves), only play in the middle of the board
                            while (edgeDistance[move] < 2) {
                                move = remainingMoves[randomInt(end)];
                            }
                        } else if (end > EDGE_BIAS_THRESHOLD) {
                            while (edgeDistance[move] < 1) {
                                // Avoid moves at edges early in the simulation
                                move = remainingMoves[randomInt(end)];
                            }
                        }
                    }

                    // Remove the chosen move from available moves
                    swap(positions[move], --end);

                    // Add the move to current player
                    if (move < 64) {
                        cML |= 1L << move;
                    } else {
                        cMR |= 1L << (move - 64);
                    }

                    lastMove = move;

                    // Swap players
                    long cML_ = cML;
                    long cMR_ = cMR;
                    cML = oML;
                    cMR = oMR;
                    oML = cML_;
                    oMR = cMR_;
                }

                // Did we win?
                // 1: we won
                // 0: we lost
                int win = myMoveAtEnd == winner(cML, cMR) ? 1 : 0;

                // Update the AMAF playout result
                result.wins += win;
                for (int j = 1; j < POSITIONS + 1; j++) {
                    if ((myMoveAtEnd == isSet(cML, cMR, j))) { // We have set j
                        result.mySamples[j]++;        // Our samples with this move
                        result.myWins[j] += win;    // Our wins with this move
                    } else {
                        result.opSamples[j]++;        // Our samples when the opponent played this move
                        result.opWins[j] += win;    // Our number of wins when the opponent played this move
                    }
                }
            }

            return result;
        }

        // Is a given position set in the bitset given by l and r?
        private boolean isSet(long l, long r, int i) {
            return (i < 64 && ((1L << i) & l) != 0) || (i >= 64 && ((1L << (i - 64)) & r) != 0);
        }

        public boolean winner(long l, long r) {
            // l and r encode the positions on the board that have been played by us.
            // Note that we update l and r in this method to remove nodes that we have already seen in the DFS search
            // I.e. l and r encode the nodes played by us that we have not yet processed

            int corners = 0;    // The corners that we have captured

            // Do a depth first search from the positions along the edge to find the edges connected from this position
            for (int i : edge) {
                if (isSet(l, r, i)) {    // Did we play this move?
                    int top = 0;        // Index for the top of the stack
                    todo[top++] = i;    // Start the search from position i

                    int edges = 0;        // The edges connected by the current component
                    while (top != 0) {
                        // Process the node on the top of the stack

                        int current = todo[--top];        // Pop one position from the stack
                        edges |= Board.edges[current];    // Update the edges reachable

                        // Add neighbours of the current node to the stack
                        for (int j = 0; j < neighbours[current].length; j++) {
                            int n = neighbours[current][j];
                            if (isSet(l, r, n)) {    // Is this node played by us?
                                // Mark the neighbour as processed
                                if (n < 64) {
                                    l &= ~(1L << n);
                                } else {
                                    r &= ~(1L << (n - 64));
                                }

                                // Add the neighour to the top of the stack
                                todo[top++] = n;

                                // Optimization: we do not have to check the next neighbour
                                // Proven by hand waving and the absence of a counter example
                                j++;
                            }
                        }
                    }

                    // Update the corners captured by the connected edges
                    corners |= cornerSet[edges];

                    // If we won with the given captured corners, we can stop
                    if (win[corners]) {
                        return true;
                    }
                }
            }

            // We did not win
            return false;
        }
    }

    // Implementation of a tree node for monte carlo tree search (MCTS)
    public class Tree {
        private final GameState state;    // The game state at the current node
        private final boolean myMove;        // Is it my move? (note that this could also be passed around instead of storing it in tree nodes)

        // Statistics gathered about child nodes based on the AMAF heuristic
        private final Statistics statistics = new Statistics();

        // A mapping of moves to child nodes
        private final Map<Integer, Tree> children = new HashMap<>();

        public Tree(GameState state) {
            this(state, true);
        }

        public Tree(GameState state, boolean myMove) {
            this.state = state;
            this.myMove = myMove;
        }

        // Constructs a child node from a parent node and a given move
        public Tree(Tree parent, int m) {
            state = new GameState(parent.state);
            myMove = !parent.myMove;
            if (parent.myMove) state.updateMyMove(m);
            else state.updateOpMove(m);
        }

        // Gets the child node of this node for a given move
        //
        // Note: this is currently unused, but could be used to optimize the AI by reusing the
        // computed data from the previous move in the next move.
        public Tree treeAfterMove(int move) {
            if (!children.containsKey(move)) {
                return new Tree(this, move);
            }
            return children.get(move);
        }

        public BestMove getBestMove() {
            // Select the move with the highest number of samples
            int mostSamples = -1;
            int bestMove = state.remainingMoves[0];
            float bestProbability = 0.5f;
            for (int i = 0; i < state.end; i++) {
                int move = state.remainingMoves[i];
                Tree child = children.get(move);
                if (child != null) {
                    int samples = child.statistics.samples;
                    if (samples > mostSamples) {
                        mostSamples = samples;
                        bestMove = move;
                        if (samples > 0) bestProbability = (float) child.statistics.wins / samples;
                    }
                }
            }
            return new BestMove(bestMove, bestProbability);
        }

        // Expands the tree by creating the most promising child node, playing a monte carlo playout in this child node, and updating the statistics in all parent nodes
        public AmafPlayout expand() {
            // If there are no moves remaining in this node, we are done
            if (state.end == 0) return new AmafPlayout();

            AmafPlayout result;
            int selected = 0;

            // Select the best node for the current player
            if (myMove) {
                // Select the node with the highest win rate
                double bestScore = -1.0;
                for (int i = 0; i < state.end; i++) {
                    int move = state.remainingMoves[i];

                    // Compute the win rate based on the AMAF heuristic
                    double score = ((double) statistics.amafWins[move]) / statistics.amafSamples[move];

                    // If we have actual samples of this node available, we do a linear interpolation of the
                    // AMAF score with the actual samples based on the ALPHA parameter (alpha-AMAF)
                    if (children.containsKey(move)) {
                        Tree child = children.get(move);
                        score = score * ALPHA + (((double) child.statistics.wins) / child.statistics.samples) * (1.0 - ALPHA);
                    }

                    // If there is no data at all available for this node, we must investigate it
                    if (statistics.amafSamples[move] == 0) {
                        score = 100.0;
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        selected = move;
                    }
                }
            } else {
                // Select the node with the lowest win rate
                double bestScore = -1.0;
                for (int i = 0; i < state.end; i++) {
                    int move = state.remainingMoves[i];

                    // Compute the win rate based on the AMAF heuristic
                    double score = ((double) (statistics.amafSamples[move] - statistics.amafWins[move])) / statistics.amafSamples[move];

                    // If we have actual samples of this node available, we do a linear interpolation of the
                    // AMAF score with the actual samples based on the ALPHA parameter (alpha-AMAF)
                    if (children.containsKey(move)) {
                        Tree child = children.get(move);
                        score = score * ALPHA + (((double) (child.statistics.samples - child.statistics.wins)) / child.statistics.samples) * (1.0 - ALPHA);
                    }

                    // If there is no data at all available for this node, we must investigate it
                    if (statistics.amafSamples[move] == 0) {
                        score = 100.0;
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        selected = move;
                    }
                }
            }

            Tree existingChild = children.get(selected);
            if (existingChild == null) {
                // If this child does not exist, create it
                Tree child = new Tree(this, selected);
                children.put(selected, child);

                // Evaluate this node with monte-carlo sampling
                // The result is a AmafPlayout instance containing number of wins, samples and AMAF statistics (number of wins and samples for all other moves played)
                result = child.state.sample(child.myMove);

                // Update the statistics of the child node
                child.statistics.add(result, child.myMove);
            } else {
                // The child exists, recursively expand this child
                result = existingChild.expand();
            }

            // Update the statistics of this node
            statistics.add(result, myMove);

            return result;
        }
    }

    // The result of a set of AMAF playouts (as constructed by)
    static class AmafPlayout {
        int samples;        // The number of samples performed
        int wins;            // The number of times we won

        int[] mySamples = new int[POSITIONS + 1];    // mySamples[i] is the number of samples where move i was played by me
        int[] myWins = new int[POSITIONS + 1];       // myWins[i] is the number of samples where move i was played by me and I won
        int[] opSamples = new int[POSITIONS + 1];    // opSamples[i] is the number of samples where move i was played by the opponent
        int[] opWins = new int[POSITIONS + 1];       // opWins[i] is the number of samples where move i was played by the opponent and I won
    }

    // The statistics stored in a node
    static class Statistics {
        int samples;    // The number of times this node has been sampled
        int wins;        // The number of samples where we have won

        int[] amafSamples = new int[POSITIONS + 1];     // amafSamples[i] is the number of times move i was played in a monte carlo game
        int[] amafWins = new int[POSITIONS + 1];        // amafWins[i] is the number of times move i was played in a monte carlo game, and we won

        // Update the statistics with a given playout result
        public void add(AmafPlayout playout, boolean myMove) {
            samples += playout.samples;
            wins += playout.wins;

            if (myMove) {
                // If it is my move, add the statistics for my moves
                for (int i = 1; i < POSITIONS + 1; i++) {
                    amafSamples[i] += playout.mySamples[i];
                    amafWins[i] += playout.myWins[i];
                }
            } else {
                // If it is the opponent move, add the statistics for the opponent moves
                for (int i = 1; i < POSITIONS + 1; i++) {
                    amafSamples[i] += playout.opSamples[i];
                    amafWins[i] += playout.opWins[i];
                }
            }
        }

        public Statistics clone() {
            Statistics result = new Statistics();

            result.samples = samples;
            result.wins = wins;
            result.amafSamples = amafSamples.clone();
            result.amafWins = amafWins.clone();

            return result;
        }
    }

    /**
     * Runs a benchmark and returns the number of expansions per second.
     */
    public double benchmark(long durationMillis) {
        Tree tree = new Tree(new GameState());

        // Call expand() a few times to warm up.
        for (int repeat = 0; repeat < 50; ++repeat) {
            tree.expand();
        }

        long start = System.currentTimeMillis();
        long finish;
        long expansions = 0;
        while ((finish = System.currentTimeMillis()) - start < durationMillis) {
            // Repeat 50 times to reduce the overhead of the outer loop. (Most devices do over
            // 1000 expansions per second, so this doesn't affect the total duration much.)
            for (int repeat = 0; repeat < 50; ++repeat) {
                tree.expand();
                expansions++;
            }
        }
        return expansions * 1000.0 / (finish - start);
    }
}
