
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Main {

    static class Solver {

        private static final char NORTH = 'N';
        private static final char SOUTH = 'S';
        private static final char WEST = 'W';
        private static final char EAST = 'E';

        static final class Beam {

            final int num;
            final int row;
            final int col;
            final int length;
            final char direction;
            final int minRow, maxRow, minCol, maxCol;

            Beam(int num, int row, int col, int length, char direction) {
                this.num = num;
                this.row = row;
                this.col = col;
                this.length = length;
                this.direction = direction;
                //this.cells = getCells();

                // Auxiliary variables
                int minR = row, maxR = row, minC = col, maxC = col;
                switch (direction) {
                    case NORTH ->
                        minR = row - length + 1;
                    case SOUTH ->
                        maxR = row + length - 1;
                    case WEST ->
                        minC = col - length + 1;
                    case EAST ->
                        maxC = col + length - 1;
                    default ->
                        throw new IllegalArgumentException("Invalid direction: " + direction);
                }

                // Ensure min <= max for rows and columns, makes it easier to check if a cell is taken by this beam.
                this.minRow = minR; // top most
                this.maxRow = maxR; // bottom most
                this.minCol = minC; // left most
                this.maxCol = maxC; // right most
            }

            boolean needsRemoval(int chosenStart, int chosenSize) {
                int targetMinCol = chosenStart;
                int targetMaxCol = chosenStart + chosenSize - 1;
                return this.maxCol >= targetMinCol && this.minCol <= targetMaxCol;
            }

            int[] getEscapeVector() {
                return switch (this.direction) {
                    case NORTH ->
                        new int[]{-1, 0};
                    case SOUTH ->
                        new int[]{1, 0};
                    case WEST ->
                        new int[]{0, -1};
                    case EAST ->
                        new int[]{0, 1};
                    default ->
                        null;
                };
            }

        }

        static final class Result {

            final boolean success;
            final List<Integer> order;

            Result(boolean success, List<Integer> order) {
                this.success = success;
                this.order = order;
            }
        }

        static Result solve2(int nRows, int nCols, int chosenSize, int chosenStart, List<Beam> beams) {
            int[][] grid = new int[nRows][nCols];
            int numBeams = beams.size();
            List<List<Integer>> graph = new ArrayList<>(numBeams);
            List<List<Integer>> reverseGraph = new ArrayList<>(numBeams);

            // Grid initialization
            for (int i = 0; i < numBeams; i++) {
                graph.add(new LinkedList<>());
                reverseGraph.add(new LinkedList<>());
                Beam b = beams.get(i);
                int[] escapeVector = b.getEscapeVector();
                int r = b.row, c = b.col;
                int rv = escapeVector[0], cv = escapeVector[1];

                for (int k = 0; k < b.length; k++) {
                    r += rv;
                    c += cv;
                    if (grid[r][c] != 0) {
                        throw new IllegalStateException("Overlapping beams at (" + r + ", " + c + ") between beam " + grid[r][c] + " and beam " + b.num);
                    }
                    grid[r][c] = b.num; // Mark the cell with the beam's 1-based index
                }
            }

            // Dependency graph construction
            List<Set<Integer>> blocking = new ArrayList<>(numBeams);
            for (int i = 0; i < numBeams; i++) {
                Beam b1 = beams.get(i);
                blocking.add(new HashSet<>());
                int[] escapeVector = b1.getEscapeVector();
                for (int k = 0; k < Math.max(nRows, nCols); k++) {
                    int r = b1.row + b1.length * escapeVector[0] + escapeVector[0] * k;
                    int c = b1.col + b1.length * escapeVector[1] + escapeVector[1] * k;
                    if (r < 0 || r >= nRows || c < 0 || c >= nCols) {
                        break; // Out of bounds, stop checking further in this direction
                    }
                    if (grid[r][c] != 0 && blocking.get(i).add(grid[r][c])) {
                        int blockingBeamIndex = grid[r][c] - 1; // Convert to 0-based index
                        if (blockingBeamIndex != i) { // Avoid self-blocking
                            graph.get(blockingBeamIndex).add(i); // blockingBeam -> b1
                            reverseGraph.get(i).add(blockingBeamIndex); // b1 -> blockingBeam
                        }
                    }
                }
            }

            boolean[] isNecessary = new boolean[numBeams];
            Queue<Integer> requiredQueue = new LinkedList<>();
            int necessaryCount = 0;

            for (int i = 0; i < numBeams; i++) {
                if (beams.get(i).needsRemoval(chosenStart, chosenSize)) {
                    isNecessary[i] = true;
                    requiredQueue.add(i);
                    necessaryCount++;
                }
            }

            // Propagate necessity backwards: if 'curr' is necessary and 'blocker' blocks 'curr', 'blocker' is necessary.
            while (!requiredQueue.isEmpty()) {
                int curr = requiredQueue.poll();
                for (int blocker : reverseGraph.get(curr)) {
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
                    int degree = reverseGraph.get(i).size();
                    if (degree == 0) {
                        q.add(i);
                    }
                    inDegree[i] = degree;
                }
            }
            
            // Topological sort using a priority queue to ensure lexicographical order
            List<Integer> order = new ArrayList<>();
            while (!q.isEmpty()) {
                int curr = q.poll();
                order.add(beams.get(curr).num); // Use the 1-based index from the Beam object

                for (int next : graph.get(curr)) {
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
    }

    private static void printOrder(List<Integer> order) {
        if (order == null || order.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i : order) {
            sb.append(i).append(' ');
        }

        // Correct way to remove the last character
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }

        System.out.println(sb.toString());
    }

    public static void main(String[] args) throws IOException {

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        int nTests = Integer.parseInt(in.readLine());

        for (int i = 0; i < nTests; i++) {
            StringTokenizer st = new StringTokenizer(in.readLine());
            int nRows = Integer.parseInt(st.nextToken());
            int nCols = Integer.parseInt(st.nextToken());

            st = new StringTokenizer(in.readLine());
            int chosenSize = Integer.parseInt(st.nextToken());
            int chosenStart = Integer.parseInt(st.nextToken());

            st = new StringTokenizer(in.readLine());
            int nBeams = Integer.parseInt(st.nextToken());
            List<Solver.Beam> beams = new ArrayList<>(nBeams);

            for (int j = 1; j <= nBeams; j++) {
                st = new StringTokenizer(in.readLine());
                int row = Integer.parseInt(st.nextToken());
                int col = Integer.parseInt(st.nextToken());
                int length = Integer.parseInt(st.nextToken());
                char direction = st.nextToken().charAt(0);
                Solver.Beam b = new Solver.Beam(j, row, col, length, direction);
                beams.add(b);
            }

            Solver.Result result = Solver.solve2(nRows, nCols, chosenSize, chosenStart, beams);
            if (!result.success) {
                System.out.println("Disaster");
            } else if (result.order.isEmpty()) {
                System.out.println("False alarm");
            } else {
                printOrder(result.order);
            }

        }

    }
}
