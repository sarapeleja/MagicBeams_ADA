import java.util.*;

class MagicBeamSolver {
    private final Beam[] beams;
    private final int[][] grid;
    // Adjacency list for the dependency graph (blocking relationships)
    private final List<Integer>[] graph;
    // Reverse adjacency list for backtracking necessary beams
    private final List<Integer>[] reverseGraph;

    private final int nRows, nCols, nBeams;

    @SuppressWarnings("unchecked")
    public MagicBeamSolver(int nRows, int nCols, int nBeams) {
        this.nRows = nRows;
        this.nCols = nCols;
        this.nBeams = nBeams;

        this.grid = new int[nRows][nCols];
        this.beams = new Beam[nBeams];

        this.graph = new ArrayList[nBeams];
        this.reverseGraph = new ArrayList[nBeams];

        for (int i = 0; i < nBeams; i++) {
            graph[i] = new ArrayList<>(nBeams);
            reverseGraph[i] = new ArrayList<>(nBeams);
        }
    }

    public void addBeam(int j, int row, int col, int length, char direction) {
        Beam b = new Beam(j, row, col, length, direction);
        beams[j-1] = b;

        //update grid
        int[] escapeVector = b.getEscapeVector();
        int dr = escapeVector[0], dc = escapeVector[1];
        int r = b.row, c = b.col;

        for (int i = 0; i < b.length; i++) {
            if (r < 0 || r >= nRows || c < 0 || c >= nCols)
                break;
            if (grid[r][c] != 0)
                throw new IllegalStateException("Overlapping beams at (" + r + ", " + c + ") between beam " + grid[r][c] + " and beam " + b.num);
            // Mark the cell with the beam's 1-based index
            grid[r][c] = b.num;
            r += dr;
            c += dc;
        }
    }

    record Result(boolean success, Iterator<Integer> order) { }

    public Result solve(int chosenSize, int chosenStart) {

        // Dependency graph construction
        buildGraph();

        // Find necessary beams and in degrees
        BitSet isNecessary = new BitSet(nBeams);
        Queue<Integer> q = new ArrayDeque<>();

        // Initial target beams
        for (int i = 0; i < nBeams; i++) {
            if (beams[i].needsRemoval(chosenStart, chosenSize)) {
                isNecessary.set(i);
                q.add(i);
            }
        }

        // Reverse BFS propagation
        while (!q.isEmpty()) {
            int curr = q.poll();
            for (int blocker : reverseGraph[curr]) {
                if (!isNecessary.get(blocker)) {
                    isNecessary.set(blocker);
                    q.add(blocker);
                }
            }
        }

        if (isNecessary.isEmpty()) { // False Alarm, early detection
            return new Result(true, Collections.emptyIterator());
        }

        // Calculate inDegree ONLY for necessary beams
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        int[] inDegree = new int[nBeams];
        for (int i = 0; i < nBeams; i++) {
            if (isNecessary.get(i)) {
                int degree = reverseGraph[i].size();
                inDegree[i] = degree;

                if (degree == 0)
                    pq.add(i);
            }
        }

        // Topological sort using a priority queue to ensure lexicographical order
        List<Integer> perm = topologicalSort(inDegree, pq, isNecessary);
        Iterator<Integer> order = perm.iterator();

        // "Disaster" (Cycle detected)
        if (isNecessary.cardinality() != perm.size())
            return new Result(false, order);

        return new Result(true, order);
    }

    private List<Integer> topologicalSort(int[] inDegree, PriorityQueue<Integer> ready, BitSet isNecessary) {
        List<Integer> permutation = new LinkedList<>();

        while ( !ready.isEmpty() ) {
            int curr = ready.remove();
            permutation.add(beams[curr].num);
            for (int v : graph[curr]) {
                if (isNecessary.get(v)) {
                    inDegree[v]--;
                    if (inDegree[v] == 0)
                        ready.add(v);
                }
            }
        }
        return permutation;
    }


    private void buildGraph() {
        for (int i = 0; i < nBeams; i++) {
            Beam b1 = beams[i];
            int[] vec = b1.getEscapeVector();
            BitSet seen = new BitSet(nBeams);

            // Start checking from the first cell OUTSIDE the beam's current body
            int r = b1.row + b1.length * vec[0];
            int c = b1.col + b1.length * vec[1];

            while (r >= 0 && r < nRows && c >= 0 && c < nCols) {
                int blockerId = grid[r][c];
                int bIndex = blockerId - 1;
                // Avoid self-blocking and duplicates
                if (blockerId != 0 && bIndex != i && !seen.get(bIndex)) {
                        seen.set(bIndex);
                        // blockerIdx must be freed BEFORE i
                        graph[bIndex].add(i);
                        reverseGraph[i].add(bIndex);
                }
                r += vec[0];
                c += vec[1];
            }
        }
    }
}
