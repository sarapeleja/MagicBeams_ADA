import java.util.*;

class MagicBeamSolver {
    private final List<Beam> beams;
    private final int[][] grid;
    // Adjacency list for the dependency graph (blocking relationships)
    private final List<Integer>[] graph;
    // Reverse adjacency list for backtracking necessary beams
    private final List<Integer>[] reverseGraph;

    private final int nRows;
    private final int nCols;
    private final int nBeams;

    @SuppressWarnings("unchecked")
    public MagicBeamSolver(int nRows, int nCols, int nBeams) {
        this.nRows = nRows;
        this.nCols = nCols;
        this.nBeams = nBeams;

        this.grid = new int[nRows][nCols];
        this.beams = new ArrayList<>(nBeams);

        this.graph = new ArrayList[nBeams];
        this.reverseGraph = new ArrayList[nBeams];

        for (int i = 0; i < nBeams; i++) {
            graph[i] = new ArrayList<>(nBeams);
            reverseGraph[i] = new ArrayList<>(nBeams);
        }
    }

    public void addBeam(int j, int row, int col, int length, char direction) {
        Beam b = new Beam(j, row, col, length, direction);
        beams.add(b);

        //update grid
        int[] escapeVector = b.getEscapeVector();
        int r = b.row, c = b.col;

        for (int k = 0; k < b.length; k++) {

            if (r < 0 || r >= nRows || c < 0 || c >= nCols) break;
            if (grid[r][c] != 0) {
                throw new IllegalStateException("Overlapping beams at (" + r + ", " + c + ") between beam " + grid[r][c] + " and beam " + b.num);
            }
            grid[r][c] = b.num; // Mark the cell with the beam's 1-based index
            r += escapeVector[0];
            c += escapeVector[1];
        }
    }

    record Result(boolean success, Iterator<Integer> order) { }

    public Result solve(int chosenSize, int chosenStart) {

        // Dependency graph construction : time complexity
        buildGraph();

        // Find necessary beams and in degrees : time complexity
        int necessaryCount = 0;
        BitSet isNecessary = new BitSet(nBeams);
        Queue<Integer> requiredQueue = new LinkedList<>();

        // Initial target beams
        for (Beam b : beams) {
            if (b.needsRemoval(chosenStart, chosenSize)) {
                isNecessary.set(b.num-1);
                requiredQueue.add(b.num-1);
                necessaryCount++;
            }
        }

        // Reverse BFS propagation
        while (!requiredQueue.isEmpty()) {
            int curr = requiredQueue.poll();

            for (int blocker : reverseGraph[curr]) {
                if (!isNecessary.get(blocker)) {
                    isNecessary.set(blocker);
                    requiredQueue.add(blocker);
                    necessaryCount++;
                }
            }
        }

        // Calculate inDegree ONLY for necessary beams
        PriorityQueue<Integer> q = new PriorityQueue<>();
        int[] inDegree = new int[nBeams];
        for (int i = 0; i < nBeams; i++) {
            if (isNecessary.get(i)) {
                int degree = reverseGraph[i].size();
                if (degree == 0) {
                    q.add(i);
                }
                inDegree[i] = degree;
            }
        }

        // Topological sort using a priority queue to ensure lexicographical order
        List<Integer> perm = topologicalSort(inDegree, q, isNecessary);
        Iterator<Integer> order = perm.iterator();

        // "Disaster" (Cycle detected)
        if (necessaryCount != 0 && perm.size() != necessaryCount)
            return new Result(false, order);

        return new Result(true, order);
    }

    private List<Integer> topologicalSort(int[] inDegree, PriorityQueue<Integer> ready, BitSet isNecessary) {
        List<Integer> permutation = new LinkedList<>();

        while ( !ready.isEmpty() ) {
            int curr = ready.remove();
            permutation.add(beams.get(curr).num);
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
        Set<Integer> blocking;
        for (int i = 0; i < nBeams; i++) {
            Beam b1 = beams.get(i);

            blocking = new HashSet<>(nBeams);
            int[] escapeVector = b1.getEscapeVector();

            for (int k = 0; k < Math.max(nRows, nCols); k++) {
                int r = b1.row + (b1.length + k) * escapeVector[0];
                int c = b1.col + (b1.length + k) * escapeVector[1];
                if (r < 0 || r >= nRows || c < 0 || c >= nCols) {
                    break; // Out of bounds, stop checking further in this direction
                }
                if (grid[r][c] != 0 && blocking.add(grid[r][c])) {
                    int blockingBeamIndex = grid[r][c] - 1; // Convert to 0-based index
                    if (blockingBeamIndex != i) { // Avoid self-blocking
                        graph[blockingBeamIndex].add(i); // blockingBeam -> b1 | blockingBeam must be removed before b1 can be removed.
                        reverseGraph[i].add(blockingBeamIndex); // b1 -> blockingBeam  | for backtracking necessary beams. If b1 is necessary, blockingBeam is also necessary.
                    }
                }
            }
        }
    }
}
