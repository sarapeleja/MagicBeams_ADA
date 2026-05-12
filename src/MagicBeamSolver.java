import java.util.*;

class MagicBeamSolver {
    // Array of all the Beams
    private final Beam[] beams;
    // Grid of coordinates, 0 for empty positions
    // and 1-based indexes for beam filled positions
    private final int[][] grid;

    // Adjacency list for the dependency graph (blocking relationships)
    private final List<Integer>[] graph;
    // Reverse adjacency list for backtracking necessary beams
    private final List<Integer>[] reverseGraph;

    // total number of rows, columns and beams
    private final int nRows, nCols, nBeams;

    /**
     * Constructor, saves sizes, initializes the grid, beam array, and adjacency lists
     * @param nRows total number of rows
     * @param nCols total number of columns
     * @param nBeams total number of beams
     */
    @SuppressWarnings("unchecked")
    public MagicBeamSolver(int nRows, int nCols, int nBeams) {
        this.nRows = nRows;
        this.nCols = nCols;
        this.nBeams = nBeams;

        this.grid = new int[nRows][nCols];
        this.beams = new Beam[nBeams];

        this.graph = new ArrayList[nBeams];
        this.reverseGraph = new ArrayList[nBeams];

        for (int i = 0; i < nBeams; i++) { //maintain default capacity
            graph[i] = new ArrayList<>(10);
            reverseGraph[i] = new ArrayList<>(10);
        }
    }

    /**
     * Adds a beam to the beam array and marks its occupied grid cells.
     * @param j beam number
     * @param row beam initial row position
     * @param col beam initial column position
     * @param length beam length
     * @param direction beams direction
     */
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

    /**
     * record that represents the output of magic beams solver
     * @param success true if solver found a solution, false otherwise
     * @param order iterator of a list of beams in the order of the solution
     */
    record Result(boolean success, Iterator<Integer> order) { }

    /**
     * magic beams solver, if possible finds the beams we need to remove, and their order
     *
     * @param chosenSize amount of columns to remove
     * @param chosenStart left-most column to remove
     * @return Result record that contains a boolean stating of the problem is solvable,
     * and an Iterator over the found solution
     */
    public Result solve(int chosenSize, int chosenStart) {

        // Dependency graph construction
        buildGraph();

        // Find necessary beams and in degrees
        BitSet isNecessary = new BitSet(nBeams);
        Queue<Integer> q = new ArrayDeque<>();
        int[] inDegree = new int[nBeams];
        // priority queue of necessary beams with NO dependencies
        PriorityQueue<Integer> pq = new PriorityQueue<>();

        // Initial target beams
        for (int i = 0; i < nBeams; i++) {
            if (beams[i].needsRemoval(chosenStart, chosenSize)) {
                isNecessary.set(i); q.add(i);

                inDegree[i] = reverseGraph[i].size();
                if (inDegree[i] == 0)
                    pq.add(i);
            }
        }

        if (q.isEmpty())// False Alarm, early detection
            return new Result(true, Collections.emptyIterator());

        // Reverse BFS propagation
        while (!q.isEmpty()) {
            int curr = q.poll();
            for (int blocker : reverseGraph[curr]) {
                if (!isNecessary.get(blocker)) {
                    isNecessary.set(blocker); q.add(blocker);

                    inDegree[blocker] = reverseGraph[blocker].size();
                    if (inDegree[blocker] == 0)
                        pq.add(blocker);
                }
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

    /**
     * topologically sorts the beams we need to remove
     * @param inDegree contains the in degree of each necessary beam
     * @param ready initial priority queue of beams ready to be removed
     * @param isNecessary all beams we need to remove
     * @return permutation of beams, ordered by dependencies and b.num
     */
    private List<Integer> topologicalSort(int[] inDegree, PriorityQueue<Integer> ready, BitSet isNecessary) {
        List<Integer> permutation = new ArrayList<>();

        while ( !ready.isEmpty() ) {
            int curr = ready.poll();
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

    /**
     * builds adjacency lists from the completed grid (grid has all the beams already inserted)
     * uses 0-based indexes
     */
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
