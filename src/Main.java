
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

                // Auxiliary variables
                int minR = row, maxR = row, minC = col, maxC = col;
                switch (direction) {
                    case NORTH -> minR = row - length + 1;
                    case SOUTH -> maxR = row + length - 1;
                    case WEST -> minC = col - length + 1;
                    case EAST -> maxC = col + length - 1;
                }
                // Ensure min <= max for rows and columns, makes it easier to check if a cell is taken by this beam.
                this.minRow = minR;
                this.maxRow = maxR;
                this.minCol = minC;
                this.maxCol = maxC;
            }

            boolean isBlockedBy(Beam other) {
                return switch (this.direction) {
                    case NORTH -> this.col >= other.minCol && this.col <= other.maxCol && other.minRow < this.row;
                    case SOUTH -> this.col >= other.minCol && this.col <= other.maxCol && other.maxRow > this.row;
                    case WEST -> this.row >= other.minRow && this.row <= other.maxRow && other.minCol < this.col;
                    case EAST -> this.row >= other.minRow && this.row <= other.maxRow && other.maxCol > this.col;
                    default -> false;
                };
            }

            boolean needsRemoval(int chosenStart, int chosenSize) {
                int targetMinCol = chosenStart;
                int targetMaxCol = chosenStart + chosenSize - 1;
                return this.maxCol >= targetMinCol && this.minCol <= targetMaxCol;
            }

        }

        
        static final class Result{
            final boolean success;
            final List<Integer> order;
            Result(boolean success, List<Integer> order){
                this.success = success;
                this.order = order;
            }
        }

        static Result solve2(int nRows, int nCols, int chosenSize, int chosenStart, List<Beam> beams) {
            int numBeams = beams.size();
            List<List<Integer>> graph = new ArrayList<>(numBeams);
            List<List<Integer>> reverseGraph = new ArrayList<>(numBeams);

            for (int i = 0; i < numBeams; i++) {
                graph.add(new ArrayList<>());
                reverseGraph.add(new ArrayList<>());
            }

            for (int i = 0; i < numBeams; i++) {
                Beam b = beams.get(i);
                for (int j = 0; j < numBeams; j++) {
                    if (i == j) {
                        continue;
                    }
                    Beam b2 = beams.get(j);

                    // If b is blocked by b2, b2 must move FIRST. 
                    if (b.isBlockedBy(b2)) {
                        graph.get(j).add(i); // Edge: b2 -> b
                        reverseGraph.get(i).add(j); // b -> b2 for tracing dependencies backwards
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

            // Propagate necessity backwards: if 'curr' is necessary and 'j' blocks 'curr', 'j' is necessary.
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

            //Calculate inDegree ONLY for edges between necessary beams
            int[] inDegree = new int[numBeams];
            for (int i = 0; i < numBeams; i++) {
                if (!isNecessary[i]) {
                    continue;
                }
                for (int next : graph.get(i)) {
                    if (isNecessary[next]) {
                        inDegree[next]++;
                    }
                }
            }

            PriorityQueue<Integer> q = new PriorityQueue<>();
            for (int i = 0; i < numBeams; i++) {
                if (isNecessary[i] && inDegree[i] == 0) {
                    q.add(i);
                }
            }

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
        if (order== null || order.isEmpty()) return;

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
            String[] line = in.readLine().split(" ");
            int nRows = Integer.parseInt(line[0]);
            int nCols = Integer.parseInt(line[1]);
            line = in.readLine().split(" ");
            int chosenSize = Integer.parseInt(line[0]);
            int chosenStart = Integer.parseInt(line[1]);
            int nBeams = Integer.parseInt(in.readLine());
            List<Solver.Beam> beams = new ArrayList<>(nBeams);
            for (int j = 1; j <= nBeams; j++) {
                line = in.readLine().split(" ");
                int row = Integer.parseInt(line[0]);
                int col = Integer.parseInt(line[1]);
                int length = Integer.parseInt(line[2]);
                char direction = line[3].charAt(0);
                Solver.Beam b = new Solver.Beam(j, row, col, length, direction);
                beams.add(b);
            }
            Solver.Result result = Solver.solve2(nRows, nCols, chosenSize, chosenStart, beams);
            if (!result.success) {
                printOrder(result.order);
                System.out.println("Disaster");
            } else if (result.order.isEmpty()) {
                System.out.println("False Alarm");
            } else {
                printOrder(result.order);
            }
        }

    }
}
