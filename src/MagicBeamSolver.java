import java.util.*;

class MagicBeamSolver {
    //comparators used to order beams per row and column
    private final Comparator<Integer> ROW_CMP;
    private final Comparator<Integer> COL_CMP;

    //saves beam indexes in the order they appear in row/column
    List<Integer>[] rowBeams;
    List<Integer>[] colBeams;

    // Array of all Beams
    private final Beam[] beams;

    // Adjacency list for the dependency graph (blocking relationships)
    // Reverse adjacency list for backtracking necessary beams
    private final List<Integer>[] graph;
    private final List<Integer>[] reverseGraph;

    // total number of beams
    private final int nBeams;

    /**
     * Constructor, saves sizes, initializes the grid, beam array, and adjacency lists
     * @param nRows total number of rows
     * @param nCols total number of columns
     * @param nBeams total number of beams
     */
    @SuppressWarnings("unchecked")
    public MagicBeamSolver(int nRows, int nCols, int nBeams) {
        this.nBeams = nBeams;

        //initialize data structure
        this.rowBeams = new ArrayList[nRows];
        this.colBeams = new ArrayList[nCols];
        this.beams = new Beam[nBeams];
        this.graph = new ArrayList[nBeams];
        this.reverseGraph = new ArrayList[nBeams];

        for (int r = 0; r < nRows; r++) {
            rowBeams[r] = new ArrayList<>();
        }
        for (int c = 0; c < nCols; c++) {
            colBeams[c] = new ArrayList<>();
        }
        for (int i = 0; i < nBeams; i++) {
            graph[i] = new ArrayList<>(Math.min(nBeams, 10));
            reverseGraph[i] = new ArrayList<>(Math.min(nBeams, 10));
        }

        ROW_CMP = (a, b) -> Integer.compare(beams[a].getMinCol(), beams[b].getMinCol());
        COL_CMP = (a, b) -> Integer.compare(beams[a].getMinRow(), beams[b].getMinRow());
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
        beams[b.getIndex()] = b;

        //add beam to its row/rows and column/columns
        for (int r = b.getMinRow(); r <= b.getMaxRow(); r++) {
            rowBeams[r].add(b.getIndex());
        }
        for (int c = b.getMinCol(); c <= b.getMaxCol(); c++) {
            colBeams[c].add(b.getIndex());
        }
    }

    /**
     * record that represents the output of magic beams solver
     * @param disaster true if solver found a cycle (no solution), false otherwise
     * @param order iterator of a list of beams in the order of the solution
     */
    record Result(boolean disaster, Iterator<Integer> order) { }

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
        Queue<Integer> pq = new PriorityQueue<>(nBeams);

        // Initial target beams (only go over the beams in the selected columns)
        for (int i = 0; i < chosenSize; i++) {
            for (int bIndex : colBeams[chosenStart + i]) {
                if (!isNecessary.get(bIndex)) {
                    isNecessary.set(bIndex); q.add(bIndex);

                    inDegree[bIndex] = reverseGraph[bIndex].size();
                    if (inDegree[bIndex] == 0)
                        pq.add(bIndex);
                }
            }
        }

        if (q.isEmpty())// False Alarm, early detection
            return new Result(false, Collections.emptyIterator());

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

        // if disaster true then a cycle was detected
        boolean disaster = isNecessary.cardinality() != perm.size();
        return new Result(disaster, order);
    }

    /**
     * topologically sorts the beams we need to remove
     * @param inDegree contains the in degree of each necessary beam
     * @param ready initial priority queue of beams ready to be removed
     * @param isNecessary all beams we need to remove
     * @return permutation of beams, ordered by dependencies and b.num
     */
    private List<Integer> topologicalSort(int[] inDegree, Queue<Integer> ready, BitSet isNecessary) {
        List<Integer> permutation = new ArrayList<>(isNecessary.cardinality());

        while ( !ready.isEmpty() ) {
            int curr = ready.poll();
            permutation.add(beams[curr].getID());
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
        sortBeams();

        for (Beam b : beams) {
            List<Integer> line;
            int pos, start, end;

            // get beam line and its line order/position
            if (b.isHorizontal()) {
                line = rowBeams[b.getRow()];
                pos = b.getRowOrder();
            } else{
                line = colBeams[b.getCol()];
                pos = b.getColOrder();
            }

            // determine blockers
            if (b.pointsForward()) {
                start = pos + 1;
                end = line.size();
            } else {
                start = 0;
                end = pos;
            }

            for (int i = start; i < end; i++) {
                int blockerIndex = line.get(i);
                graph[blockerIndex].add(b.getIndex());
                reverseGraph[b.getIndex()].add(blockerIndex);
            }
        }
    }

    /**
     * sorts each beam of rowBeams and colBeams in order
     * and saves that order in each relevant beam
     */
    private void sortBeams() {
        for (List<Integer> row : rowBeams) {
            if (row.size() > 1) row.sort(ROW_CMP);

            for (int i = 0; i < row.size(); i++) {
                Beam b = beams[row.get(i)];
                if (b.isHorizontal()) //save order
                    b.setRowOrder(i);
            }
        }

        for (List<Integer> col : colBeams) {
            if (col.size() > 1) col.sort(COL_CMP);

            for (int i = 0; i < col.size(); i++) {
                Beam b = beams[col.get(i)];
                if (!b.isHorizontal()) //save order
                    b.setColOrder(i);
            }
        }

    }
}
