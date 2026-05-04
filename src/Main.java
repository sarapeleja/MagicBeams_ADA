
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

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
            final List<Entry<Integer, Integer>> cells; // Lazy initialization of the cells this beam occupies.
            private List<Entry<Integer, Integer>> inLineCells;

            Beam(int num, int row, int col, int length, char direction, int nRows, int nCols) {
                this.num = num;
                this.row = row;
                this.col = col;
                this.length = length;
                this.direction = direction;
                this.cells = getCells();
                this.inLineCells = getInLineCells(nRows, nCols); // Lazy initialization of the cells in the line of this beam.

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
                List<Entry<Integer, Integer>> otherCells = other.getCells();
                // Check if any cell of the other beam is in the path of this beam.
                switch(this.direction){
                    case NORTH->{
                        for(Entry<Integer, Integer> cell : otherCells) {
                            int r = cell.getKey();
                            int c = cell.getValue();
                            if (c == this.col && r >= this.minRow) {
                                return true;
                            }
                        }
                    }
                    case SOUTH->{
                        for(Entry<Integer, Integer> cell : otherCells) {
                            int r = cell.getKey();
                            int c = cell.getValue();
                            if (c == this.col && r <= this.maxRow) {
                                return true;
                            }
                        }
                    }
                    case WEST->{
                        for(Entry<Integer, Integer> cell : otherCells) {
                            int r = cell.getKey();
                            int c = cell.getValue();
                            if (r == this.row && c >= this.minCol) {
                                return true;
                            }
                        }
                    }
                    case EAST->{
                        for(Entry<Integer, Integer> cell : otherCells) {
                            int r = cell.getKey();
                            int c = cell.getValue();
                            if (r == this.row && c <= this.maxCol) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }

            boolean needsRemoval(int chosenStart, int chosenSize) {
                int targetMinCol = chosenStart;
                int targetMaxCol = chosenStart + chosenSize - 1;
                return this.maxCol >= targetMinCol && this.minCol <= targetMaxCol;
            }

            // Cells occupied by this beam, calculated lazily and stored for future reference.
            List<Entry<Integer, Integer>> getCells() {
                if (this.cells != null) {
                    return this.cells;
                }
                List<Entry<Integer, Integer>> tiles = new ArrayList<>(length);
                int r = row, c = col;
                int i = 0, j = 0;
                switch (this.direction) {
                    case NORTH ->
                        i = - 1;
                    case SOUTH ->
                        i = 1;
                    case WEST ->
                        j = - 1;
                    case EAST ->
                        j = 1;
                }

                for (int k = 0; k < length; k++) {
                    tiles.add(new AbstractMap.SimpleEntry<>(r, c));
                    r += i;
                    c += j;
                }
                return tiles;
            }

            // cells in path of this beam, calculated lazily and stored for future reference.
            List<Entry<Integer,Integer>> getInLineCells(int nRows, int nCols) {
                if (this.inLineCells != null) {
                    return this.inLineCells;
                }
                this.inLineCells = new LinkedList<>();
                switch (this.direction) {
                    case NORTH -> {
                        for (int r = minRow; r >= 0 ; r--) {
                            inLineCells.add(new AbstractMap.SimpleEntry<>(r, col));
                        }
                        return inLineCells;
                    }
                    case SOUTH -> {
                        for (int r = minRow; r < nRows ; r++) {
                            inLineCells.add(new AbstractMap.SimpleEntry<>(r, col));
                        }
                        return inLineCells;
                    }
                    case WEST -> {
                        for (int c = minCol; c >= 0 ; c--) {
                            inLineCells.add(new AbstractMap.SimpleEntry<>(row, c));
                        }
                        return inLineCells;
                    }
                    case EAST -> {

                        for (int c = minCol; c < nCols ; c++) {
                            inLineCells.add(new AbstractMap.SimpleEntry<>(row, c));
                        }
                        return inLineCells;
                    }
                    default -> throw new IllegalStateException("Unexpected direction: " + direction);
                }
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
            int[][] grid = new int[nRows][nCols];
            int numBeams = beams.size();
            List<List<Integer>> graph = new ArrayList<>(numBeams);
            List<List<Integer>> reverseGraph = new ArrayList<>(numBeams);

            for (int i = 0; i < numBeams; i++) {
                graph.add(new LinkedList<>());
                reverseGraph.add(new LinkedList<>());
            }

            for (int i = 0; i < numBeams; i++) {
                Beam b = beams.get(i);
                List<Entry<Integer, Integer>> cells = b.getCells();
                for (Entry<Integer, Integer> cell : cells) {
                    int r = cell.getKey();
                    int c = cell.getValue();
                    if (r >= 0 && r < nRows && c >= 0 && c < nCols) {
                        grid[r][c] = b.num; // Mark the grid with the beam number (1-based index)
                    }
                }
                
            }

            for (int j = 0; j < numBeams; j++) {
                Beam b2 = beams.get(j);

                // If b is blocked by b2, b2 must move FIRST.
                for(Entry<Integer, Integer> cell : b2.getInLineCells(nRows, nCols)) {
                    int r = cell.getKey();
                    int c = cell.getValue();
                    if (r >= 0 && r < nRows && c >= 0 && c < nCols) {
                        int blockingBeamNum = grid[r][c];
                        if (blockingBeamNum != 0) {                          
                            graph.get(j).add(blockingBeamNum - 1); // Edge: b2 -> blockingBeam
                            reverseGraph.get(blockingBeamNum - 1).add(j); // blockingBeam -> b2 for tracing dependencies backwards
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

            List<Integer> order = new ArrayList<>(necessaryCount);
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
                Solver.Beam b = new Solver.Beam(j, row, col, length, direction,nRows, nCols);
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
