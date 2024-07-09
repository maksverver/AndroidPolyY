package ch.verver.poly_y;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable description of a Poly-Y game board, which consists of a central vertex surrounded
 * by concentric polygons (with an equal number of sides). The number of corners necessarily
 * equals the number of sides.
 */
public final class BoardGeometry {

    public static class Vertex {

        /** 0-based index of this vertex in the {@link BoardGeometry#vertices} array */
        public final int id;

        // 2D location inside a unit circle
        public final float x, y;

        /** Bitmask of which sides of the board this field touches. */
        public final int sidesMask;

        private final ArrayList<Vertex> modifiableNeighbors = new ArrayList<>();

        public final List<Vertex> neighbors = Collections.unmodifiableList(modifiableNeighbors);

        Vertex(int id, float x, float y, int sidesMask) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.sidesMask = sidesMask;
        }

        static Vertex create(int id, int size, int side, int index, int boardSize, int sides) {
            final double roundness = 0.4f;
            // The board consists of vertices that lie on concentric polygons, such that each
            // polygon of size S has S vertices per side. Vertices can be identified by the triple
            // (size, side, index) where size is the size of the polygon, side is the side of the
            // pentagon (0 <= side < sides) and index is the index of the vertex on this side
            // (0 <= index < size).
            //
            // To make the board look more aesthetically pleasing, we calculate not just the
            // position of vertices on a regular pentagon, but also on a circle with radius
            // `size` that has 5*size vertices on it, and then we take a weighted average
            // using the parameter `roundness` (0 <= roundness <= 1). The board shouldn't
            // be perfectly round, because then the edges of the board are unclear!
            //
            double x = 0, y = 0;
            if (size > 0) {
                // (x1, y1) is the first endpoint of the side of the polygon
                double a1 = 1.0 * side / sides * 2 * Math.PI - Math.PI / 2;
                double x1 = Math.cos(a1);
                double y1 = Math.sin(a1);
                // (x2, y2) is the second endpoint of the side of the polygon
                double a2 = 1.0 * (side + 1) / sides * 2 * Math.PI - Math.PI / 2;
                double x2 = Math.cos(a2);
                double y2 = Math.sin(a2);
                // (x3, y3) is the point on side of the polygon corresponding to `index`
                double x3 = ((size - index)*x1 + index*x2)/boardSize;
                double y3 = ((size - index)*y1 + index*y2)/boardSize;
                // (x4, y4) is the point on a circle with 5*size points spread around evenly.
                double a4 = 1.0 * (side * size + index) / (sides * size) * 2 * Math.PI - Math.PI / 2;
                double x4 = Math.cos(a4)*((double) size / boardSize);
                double y4 = Math.sin(a4)*((double) size / boardSize);
                // the final point (x, y) is calculated as a weighted average of (x3, y3) and (x4, y4)
                x = (1 - roundness)*x3 + roundness*x4;
                y = (1 - roundness)*y3 + roundness*y4;
            }
            int sidesMask = 0;
            if (size + 1 == boardSize) {
                // Vertex is at the edge of the board.
                sidesMask |= 1 << side;
                if (index == 0) sidesMask |= 1 << (side == 0 ? sides - 1 : side - 1);
            }
            return new Vertex(id, (float) x, (float) y, sidesMask);
        }

        @NonNull
        @Override
        public String toString() {
            return "Vertex{id=" + id + "}";
        }
    }

    public static class Edge {
        Edge(Vertex v, Vertex w) {
            this.v = v;
            this.w = w;
        }

        public final Vertex v, w;

        @NonNull
        @Override
        public String toString() {
            return "Edge{v=" + v.id + ", w=" + w.id + "}";
        }
    }

    /** The smallest possible board, which has just 1 vertex and no edges. */
    public static final BoardGeometry DUMMY_GEOMETRY = new BoardGeometry(1, 3);

    /**
     * The Poly-Y board used for the CodeCup, which has 5 sides and size 7 (1 central vertex +
     * 6 pentagons), for a total of 106 vertices and 285 edges.
     */
    public static final BoardGeometry DEFAULT_GEOMETRY = new BoardGeometry(7, 5);

    public static final int MIN_BOARD_SIZE = 1;
    public static final int MAX_BOARD_SIZE = 31;  // arbitrary upper bound
    public static final int MIN_SIDES = 3;
    public static final int MAX_SIDES = 31;  // because we use int bitmasks (without sign bit)

    public final int boardSize;
    public final int sides;
    public final List<Vertex> vertices;
    public final List<Edge> edges;

    private final int[] codeCupIds;
    private final int[] inverseCodeCupIds;

    public BoardGeometry(int boardSize, int sides) {
        if (boardSize < MIN_BOARD_SIZE || boardSize > MAX_BOARD_SIZE) {
            throw new IllegalArgumentException("boardSize must be between " + MIN_BOARD_SIZE + " and " + MAX_BOARD_SIZE);
        }
        if (sides < MIN_SIDES || sides > MAX_SIDES) {
            throw new IllegalArgumentException("sides must be between " + MIN_SIDES + " and " + MAX_SIDES);
        }

        this.boardSize = boardSize;
        this.sides = sides;

        int vertexCount = 1 + sides*(boardSize - 1)*boardSize/2;
        ArrayList<Vertex> vertices = new ArrayList<>(vertexCount);
        vertices.add(Vertex.create(0, 0, 0, 0, boardSize, sides));

        for (int size = 1; size < boardSize; ++size) {
            for (int side = 0; side < sides; ++side) {
                for (int index = 0; index < size; ++index) {
                    vertices.add(Vertex.create(vertices.size(), size, side, index, boardSize, sides));
                }
            }
        }
        if (vertices.size() != vertexCount) {
            throw new RuntimeException("Unexpected number of vertices (" + vertices.size() + " instead of " + vertexCount + ")");
        }

        int edgeCount = sides*(boardSize - 1)*(3*boardSize - 2)/2;
        ArrayList<Edge> edges = new ArrayList<>(edgeCount);
        int vertexId = 1;
        for (int size = 1; size < boardSize; ++size) {
            for (int side = 0; side < sides; ++side) {
                for (int index = 0; index < size; ++index) {
                    edges.add(new Edge(vertices.get(vertexId), vertices.get(coordsToId(size, side, index + 1, sides))));
                    edges.add(new Edge(vertices.get(vertexId), vertices.get(coordsToId(size - 1, side, index, sides))));
                    if (index > 0) {
                        edges.add(new Edge(vertices.get(vertexId), vertices.get(coordsToId(size - 1, side, index - 1, sides))));
                    }
                    ++vertexId;
                }
            }
        }
        if (edges.size() != edgeCount) {
            throw new RuntimeException("Unexpected number of edges (" + edges.size() + " instead of " + edgeCount + ")");
        }

        for (Edge edge : edges) {
            edge.v.modifiableNeighbors.add(edge.w);
            edge.w.modifiableNeighbors.add(edge.v);
        }

        this.vertices = Collections.unmodifiableList(vertices);
        this.edges = Collections.unmodifiableList(edges);

        // Calculate CodeCup vertex ids, which are used by the Lynx AI. Vertices are numbered
        // starting with 1 at the top, then moving down row by row, as shown here:
        // https://archive.codecup.nl/2014/images/poly-y_board.png
        // (The code below starts counting from 0 for consistency with the inverse ids.)
        codeCupIds = new int[vertices.size()];
        inverseCodeCupIds = new int[vertices.size()];
        Arrays.fill(codeCupIds, -1);
        Arrays.fill(inverseCodeCupIds, -1);
        {
            ArrayList<Vertex> todo = new ArrayList<>();
            ArrayList<Vertex> nextTodo = new ArrayList<>();
            boolean[] seen = new boolean[vertices.size()];
            {
                Vertex topVertex = vertices.get(0);
                for (Vertex v : vertices) if (v.y < topVertex.y) topVertex = v;
                seen[topVertex.id] = true;
                todo.add(topVertex);
            }

            int nextCodeCupId = 0;
            while (!todo.isEmpty()) {
                nextTodo.clear();
                for (Vertex v : todo) {
                    assert codeCupIds[v.id] == -1;
                    assert inverseCodeCupIds[nextCodeCupId] == -1;
                    codeCupIds[v.id] = nextCodeCupId;
                    inverseCodeCupIds[nextCodeCupId] = v.id;
                    ++nextCodeCupId;
                    for (Vertex w : v.neighbors) {
                        if (!seen[w.id]) {
                            seen[w.id] = true;
                            nextTodo.add(w);
                        }
                    }
                }
                Collections.sort(nextTodo, (v, w) -> Float.compare(v.x, w.x));
                ArrayList<Vertex> tmp = todo;
                todo = nextTodo;
                nextTodo = tmp;
            }
            assert nextCodeCupId == vertices.size();
        }
    }

    Vertex codeCupIdToVertex(int codeCupId) {
        return vertices.get(inverseCodeCupIds[codeCupId - 1]);
    }

    int vertexToCodeCupId(Vertex v) {
        return codeCupIds[v.id] + 1;
    }

    // Assumes 0 <= size, 0 <= side < sides, 0 <= index <= side.
    static private int coordsToId(int size, int side, int index, int sides) {
        if (size < 0) throw new IllegalArgumentException();
        if (size == 0) {
            return 0;  // origin
        }
        if (index >= size) {
            side += index / size;
            index = index % size;
        }
        if (side >= sides) {
            side %= sides;
        }
        return 1 + sides*(size - 1)*size/2 + size*side + index;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof BoardGeometry)) return false;
        BoardGeometry other = (BoardGeometry) obj;
        return boardSize == other.boardSize && sides == other.sides;
    }

    @Override
    public int hashCode() {
        return sides + 31*boardSize;
    }
}
