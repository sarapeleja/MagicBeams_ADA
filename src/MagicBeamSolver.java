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

    record Result(boolean success, List<Integer> order) { }

    public Result solve(int chosenSize, int chosenStart) {
        int numBeams = beams.size();

        // Dependency graph construction
        buildGraph();

        boolean[] isNecessary = new boolean[numBeams];
        Queue<Integer> requiredQueue = new LinkedList<>();
        int necessaryCount = 0;

        for (int i = 0; i < numBeams; i++) {
            // Check if beam 'i' needs to be removed to clear the path for the chosen beam. If so, mark it as necessary and add to the queue.
            if (beams.get(i).needsRemoval(chosenStart, chosenSize)) {
                isNecessary[i] = true;
                requiredQueue.add(i);
                necessaryCount++;
            }
        }

        // Propagate necessity backwards: if 'curr' is necessary and 'blocker' blocks 'curr', 'blocker' is necessary.
        while (!requiredQueue.isEmpty()) {
            int curr = requiredQueue.poll();
            // For each beam that blocks 'curr', if it's not already marked as necessary, mark it and add to the queue.
            for (int blocker : reverseGraph[curr]) {
                if (!isNecessary[blocker]) {
                    isNecessary[blocker] = true;
                    requiredQueue.add(blocker);
                    necessaryCount++;
                }
            }
        }


        // Calculate inDegree ONLY for necessary beams
        PriorityQueue<Integer> q = new PriorityQueue<>();
        int[] inDegree = new int[numBeams];
        for (int i = 0; i < numBeams; i++) {
            if (isNecessary[i]) {
                int degree = reverseGraph[i].size();
                if (degree == 0) {
                    q.add(i);
                }
                inDegree[i] = degree;
            }
        }

        // Topological sort using a priority queue to ensure lexicographical order
        List<Integer> order = new LinkedList<>();
        while (!q.isEmpty()) {
            int curr = q.poll();
            order.add(beams.get(curr).num); // Use the 1-based index from the Beam object

            for (int next : graph[curr]) {
                if (isNecessary[next]) {
                    inDegree[next]--;
                    if (inDegree[next] == 0) {
                        q.add(next);
                    }
                }
            }
        }

        if (necessaryCount == 0) {
            return new Result(true, order); // "False Alarm"
        }
        if (order.size() != necessaryCount) {
            return new Result(false, order); // "Disaster" (Cycle detected)
        }

        return new Result(true, order);
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
