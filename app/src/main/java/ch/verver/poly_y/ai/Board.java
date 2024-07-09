package ch.verver.poly_y.ai;

class Board {
    public static final int POSITIONS = 106;            // Total number of positions on the board

    // Determines whether we win with a given set of captured corners
    // Given a set of captured corners encoded as the bitmask i, win[i] is true if this set of captured corners is winning, otherwise win[i] is false
    public static final boolean[] win = {false, false, false, false, false, false, false, true, false, false, false, true, false, true, true, true, false, false, false, true, false, true, true, true, false, true, true, true, true, true, true, true};

    // Corner masks for a given edge mask, determines which corners we captured given a set of edges
    // Given a set of edges encoded as a bitmask i, cornerSet[i] gives the corners captured by a component that connects these edges
    public static final int[] cornerSet = {0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 2, 0, 8, 12, 14, 0, 0, 0, 3, 0, 1, 4, 7, 0, 17, 16, 19, 24, 25, 28, 31};

    // A list of board positions that are on the edge of the playing board
    // We only use this to find connections between edges in the 'winner' method, as we dont need to check from every edge, this array only contains the moves from three edges
    public static final int[] edge = {1, 4, 9, 16, 25, 36, 49, 2, 5, 10, 17, 26, 37, 61, 72, 82, 91, 99, 106};

    // The neighbour positions of each positions in either clockwise or counter-clockwise order
    public static final int[][] neighbours = {null, {4, 3, 2}, {5, 6, 3, 1}, {1, 2, 6, 7, 8, 4}, {9, 8, 3, 1}, {10, 11, 6, 2}, {2, 3, 7, 12, 11, 5}, {3, 6, 12, 13, 14, 8}, {3, 4, 9, 15, 14, 7}, {4, 8, 15, 16}, {5, 11, 18, 17}, {19, 18, 10, 5, 6, 12}, {19, 20, 13, 7, 6, 11}, {21, 20, 12, 7, 14, 22}, {23, 22, 13, 7, 8, 15}, {16, 24, 23, 14, 8, 9}, {9, 15, 24, 25}, {26, 27, 18, 10}, {17, 27, 28, 19, 11, 10}, {18, 11, 12, 20, 29, 28}, {19, 29, 30, 21, 13, 12}, {32, 22, 13, 20, 30, 31}, {32, 33, 23, 14, 13, 21}, {34, 33, 22, 14, 15, 24}, {34, 35, 25, 16, 15, 23}, {36, 35, 24, 16}, {37, 38, 27, 17}, {17, 18, 28, 39, 38, 26}, {19, 18, 27, 39, 40, 29}, {19, 20, 30, 41, 40, 28}, {21, 20, 29, 41, 42, 31}, {32, 21, 30, 42, 43, 44}, {33, 22, 21, 31, 44, 45}, {34, 23, 22, 32, 45, 46}, {35, 24, 23, 33, 46, 47}, {34, 24, 25, 36, 48, 47}, {25, 35, 48, 49}, {26, 38, 50}, {51, 50, 37, 26, 27, 39}, {51, 38, 27, 28, 40, 52}, {39, 52, 53, 41, 29, 28}, {54, 53, 40, 29, 30, 42}, {55, 54, 41, 30, 31, 43}, {55, 42, 31, 44, 56}, {32, 31, 43, 56, 57, 45}, {32, 33, 46, 58, 57, 44}, {34, 33, 45, 58, 59, 47}, {34, 35, 48, 60, 59, 46}, {35, 36, 49, 61, 60, 47}, {61, 48, 36}, {62, 51, 38, 37}, {50, 38, 39, 52, 63, 62}, {51, 39, 40, 53, 64, 63}, {64, 65, 54, 41, 40, 52}, {55, 66, 65, 53, 41, 42}, {54, 66, 67, 56, 43, 42}, {68, 67, 55, 43, 44, 57}, {68, 69, 58, 45, 44, 56}, {69, 70, 59, 46, 45, 57}, {70, 71, 60, 47, 46, 58}, {71, 59, 47, 48, 61, 72}, {72, 60, 48, 49}, {73, 63, 51, 50}, {51, 52, 64, 74, 73, 62}, {65, 53, 52, 63, 74, 75}, {64, 53, 54, 66, 76, 75}, {55, 54, 65, 76, 77, 67}, {68, 78, 77, 66, 55, 56}, {69, 57, 56, 67, 78, 79}, {68, 57, 58, 70, 80, 79}, {69, 80, 81, 71, 59, 58}, {70, 81, 82, 72, 60, 59}, {61, 60, 71, 82}, {62, 63, 74, 83}, {84, 83, 73, 63, 64, 75}, {85, 84, 74, 64, 65, 76}, {85, 86, 77, 66, 65, 75}, {87, 86, 76, 66, 67, 78}, {68, 67, 77, 87, 88, 79}, {68, 69, 80, 89, 88, 78}, {69, 70, 81, 90, 89, 79}, {70, 71, 82, 91, 90, 80}, {91, 81, 71, 72}, {73, 74, 84, 92}, {85, 93, 92, 83, 74, 75}, {84, 93, 94, 86, 76, 75}, {85, 76, 77, 87, 95, 94}, {86, 77, 78, 88, 96, 95}, {87, 96, 97, 89, 79, 78}, {98, 97, 88, 79, 80, 90}, {98, 99, 91, 81, 80, 89}, {82, 81, 90, 99}, {83, 84, 93, 100}, {85, 84, 92, 100, 101, 94}, {85, 86, 95, 102, 101, 93}, {102, 103, 96, 87, 86, 94}, {103, 95, 87, 88, 97, 104}, {98, 89, 88, 96, 104, 105}, {99, 106, 105, 97, 89, 90}, {91, 90, 98, 106}, {92, 93, 101}, {100, 93, 94, 102}, {101, 94, 95, 103}, {104, 96, 95, 102}, {105, 97, 96, 103}, {104, 97, 98, 106}, {105, 98, 99}};

    // The edge mask of the edges a position is connected to
    // edges[i] is an edge mask that encodes the set of a edges to which position i is adjacent
    public static final int[] edges = {0, 17, 1, 0, 16, 1, 0, 0, 0, 16, 1, 0, 0, 0, 0, 0, 16, 1, 0, 0, 0, 0, 0, 0, 0, 16, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 24, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 2, 0, 0, 0, 0, 0, 0, 0, 0, 8, 2, 0, 0, 0, 0, 0, 0, 0, 8, 2, 0, 0, 0, 0, 0, 0, 8, 6, 4, 4, 4, 4, 4, 12};

    // The distance of a position from the edge
    // edgeDistance[i] is the smallest number of positions between position i and the edge
    public static final int[] edgeDistance = {0, 0, 0, 1, 0, 0, 1, 2, 1, 0, 0, 1, 2, 3, 2, 1, 0, 0, 1, 2, 3, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 5, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 4, 4, 3, 2, 1, 0, 0, 1, 2, 3, 3, 3, 3, 2, 1, 0, 0, 1, 2, 2, 2, 2, 2, 1, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0};
}
