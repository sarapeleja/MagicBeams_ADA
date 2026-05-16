import java.util.*;

class MagicBeamSolver {
    //comparators used to order beams per row and column
    private final Comparator<Integer> ROW_CMP;
    private final Comparator<Integer> COL_CMP;

    // Adjacency list: for each entry, saves the beams blocked by the entry
    private final List<Integer>[] graph;

    //saves beam indexes in the order they appear in row/column
    List<Integer>[] rowBeams;
    List<Integer>[] colBeams;

    // Array of all Beams
    private final Beam[] beams;

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

        for (int r = 0; r < nRows; r++) {
            rowBeams[r] = new ArrayList<>(Math.min(nCols, nBeams));
        }
        for (int c = 0; c < nCols; c++) {
            colBeams[c] = new ArrayList<>(Math.min(nRows, nBeams));
        }
        for (int i = 0; i < nBeams; i++) {
            graph[i] = new ArrayList<>(Math.min(nBeams, 10));
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
        // auxiliary data structures
        BitSet isNecessary = new BitSet(nBeams);
        Queue<Integer> q = new ArrayDeque<>();

        // Find immediately necessary beams
        for (int i = 0; i < chosenSize; i++) {
            for (int b : colBeams[chosenStart + i]) {
                if (!isNecessary.get(b)) {
                    isNecessary.set(b);
                    q.add(b);
                }
            }
        }

        if (q.isEmpty()) // False Alarm, early detection
            return new Result(false, Collections.emptyIterator());

        // Dependency graph construction and in degree's of said graph
        int[] inDeg = buildGraph(isNecessary, q);

        // Topological sort using a priority queue to ensure lexicographical order
        List<Integer> perm = topologicalSort(inDeg, isNecessary);

        // if disaster true then a cycle was detected
        boolean disaster = isNecessary.cardinality() != perm.size();
        return new Result(disaster, perm.iterator());
    }

    /**
     * topologically sorts the beams we need to remove
     *
     * @param inDeg contains the in degree of each necessary beam
     * @param isNecessary all beams we need to remove
     * @return permutation of beams, ordered by dependencies and b.num
     */
    private List<Integer> topologicalSort(int[] inDeg, BitSet isNecessary) {
        int necessary = isNecessary.cardinality();
        List<Integer> permutation = new ArrayList<>(necessary);
        Queue<Integer> ready = new PriorityQueue<>(necessary);

        //initialize ready with beams with no dependencies
        for (int i = 0; i < nBeams; i++) {
            if (isNecessary.get(i) && inDeg[i] == 0)
                ready.add(i);
        }

        while ( !ready.isEmpty() ) {
            int curr = ready.poll();
            permutation.add(beams[curr].getID());
            for (int v : graph[curr]) {
                if (isNecessary.get(v)) {
                    if (--inDeg[v] == 0)
                        ready.add(v);
                }
            }
        }
        return permutation;
    }

    /**
     * builds adjacency lists and in degrees for beams we need to remove
     * updates isNecessary (dependency propagation)
     * uses 0-based indexes
     *
     * @param isNecessary keeps track of necessary beams (helps avoid duplicate entries in q)
     * @param q queue of beams we want to check for dependencies
     * @return the in degree of each beam considering their found dependencies
     */
    private int[] buildGraph(BitSet isNecessary, Queue<Integer> q) {
        //ensure beams are ordered in rowBeams and colBeams
        sortBeams();

        int[] inDeg = new int[nBeams];
        while (!q.isEmpty()) {
            // get next beam
            int bIndex = q.poll();
            Beam b = beams[bIndex];

            // get beam line and its line order/position
            boolean horizontal = b.isHorizontal();
            List<Integer> line = horizontal ? rowBeams[b.getRow()] : colBeams[b.getCol()];
            int pos = horizontal ? b.getRowOrder() : b.getColOrder();

            // determine blockers
            boolean forward = b.pointsForward();
            int start = forward ? pos + 1 : 0;
            int end = forward ? line.size() : pos;

            for (int i = start; i < end; i++) {
                int blocker = line.get(i);
                int blocked = b.getIndex();
                graph[blocker].add(blocked);
                inDeg[blocked]++; //number of dependencies go up

                if (!isNecessary.get(blocker)) {
                    isNecessary.set(blocker);
                    q.add(blocker);
                }
            }
        }

        return inDeg;
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
