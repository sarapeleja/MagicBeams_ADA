import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;


public class Main {

    static class Solver{
        private static final char NORTH = 'N';
        private static final char SOUTH = 'S';
        private static final char WEST = 'W';
        private static final char EAST = 'E';

        static final class Beam{
            private int num;
            private int row;
            private int col;
            private int length;
            private char direction;
            private List<Entry<Integer, Integer>> cells;

            Beam(int num, int row, int col, int length, char direction){
                this.num = num;
                this.row = row;
                this.col = col;
                this.length = length;
                this.direction = direction;
                this.cells = getCells();
            }

            List<Entry<Integer, Integer>> getCells(){
                if(this.cells != null)
                    return this.cells;
                List<Entry<Integer, Integer>> tiles = new ArrayList<>(length);
                int r = row, c = col;
                int i = 0, j = 0;
                switch(this.direction){
                    case NORTH -> i = - 1;
                    case SOUTH -> i = 1;
                    case WEST -> j = - 1;
                    case EAST -> j = 1;
                }

                for (int k = 0; k < length; k++) {
                    tiles.add(new AbstractMap.SimpleEntry<>(r, c));
                    r += i;
                    c += j;
                }
                return tiles;
            }

            boolean isTaken(int c, int r) {
                return switch (this.direction) {
                    case NORTH -> this.col == c && r <= this.row && r > this.row - this.length;
                    case SOUTH -> this.col == c && r >= this.row && r < this.row + this.length;
                    case WEST  -> this.row == r && c <= this.col && c > this.col - this.length;
                    case EAST  -> this.row == r && c >= this.col && c < this.col + this.length;
                    default -> false;
                };
            }

            /* Checks if cells/tiles in the direction of this beam are blocked by another beam */
            boolean isBlockedBy(Beam other, int nRows, int nCols) {
                int r = this.row, c = this.col;

                int i = 0, j = 0;
                switch(this.direction){
                    case NORTH -> i = - 1;
                    case SOUTH -> i = 1;
                    case WEST -> j = - 1;
                    case EAST -> j = 1;
                }

                while (r >= 0 && r < nRows && c >= 0 && c < nCols) {
                    r += i;
                    c += j;
                    if(other.isTaken(c, r)){
                        return true;
                    }
                }
                return false;
            }

            boolean isInChosen(int chosenStart, int chosenSize, int nRows, int nCols) {
                return switch (this.direction) {
                    case NORTH, SOUTH -> chosenStart <= this.col && this.col < chosenStart + chosenSize - 1;
                    case WEST, EAST -> {
                        for (int i = chosenStart; i < chosenStart + chosenSize - 1; i++) {
                            Beam imaginaryBeam = new Beam(i, 0, i, nRows, SOUTH);
                            if (this.isBlockedBy(imaginaryBeam, nRows, nCols)) {
                                yield true;
                            }
                        }
                        yield false;
                    }
                    default -> false;
                };
                /* return switch (this.direction) {
                    case NORTH, SOUTH -> this.col >= chosenStart && this.col < chosenStart + chosenSize;
                    case WEST, EAST -> this.row >= chosenStart && this.row < chosenStart + chosenSize;
                    default -> false;
                }; */
            }
            boolean needsRemoval(int chosenStart, int chosenSize) {
                for (Entry<Integer, Integer> cell : getCells()) {
                    int c = cell.getValue();
                    if (c >= chosenStart && c < chosenStart + chosenSize) {
                        return true;
                    }
                }
                return false;
            }
        }

        static List<Beam> findDependecies( List<List<Integer>> graph, List<Beam> beams, List<Beam> mustRemove ) {
            boolean[] found = new boolean[ graph.size() ];
            for (int i = 0; i < graph.size(); i++) {
                found[i] = false;
            }
            for (int i = 0; i < graph.size(); i++) {
                if ( !found[i] )
                    mustRemove = bfsExplore(graph, found, i, beams, mustRemove);
            }
            return mustRemove;
        }

        static List<Beam> bfsExplore( List<List<Integer>> graph, boolean[] found, int root, List<Beam> beams,List<Beam> mustRemove ) {
            Queue<Integer> q = new LinkedList<>();
            q.add(root);
            found[root] = true;
            do {
                int node = q.poll();
                mustRemove.add(beams.get(node));
                // PROCESS(node)
                for (int v : graph.get(node)) {
                    if ( !found[v] ) {
                        q.add(v);
                        found[v] = true;
                    }
                }
            }
            while (!q.isEmpty());
            return mustRemove;
        }

        static List<Integer> solve(int nRows, int nCols, int chosenSize, int chosenStart, List<Beam> beams){
            // Change this upon determining it is impossible to solve.
            boolean isImpossible = false;

            List<List<Integer>> graph = new ArrayList<>(beams.size());
            int[] inDegree = new int[beams.size()];

            List<Beam> mustRemove = new ArrayList<>(beams.size());

            for (int i = 0; i < beams.size(); i++) {
                Beam b = beams.get(i);
                graph.add(i,new LinkedList<>());
                if (b.isInChosen(chosenStart, chosenSize, nRows, nCols)) {
                    mustRemove.add(b);
                }

                for (int j = 0; j < beams.size(); j++) {
                    Beam b2 = beams.get(j);

                    if (i != j && b.isBlockedBy(b2, nRows, nCols)) {
                        graph.get(j).add(i);
                        inDegree[i]++;
                    }

                }
            }
            for (Beam b : mustRemove) {
                mustRemove = findDependecies(graph, beams, mustRemove);
            }

            PriorityQueue<Integer> q = new PriorityQueue<>();
            for (int i = 0; i < beams.size(); i++) {
                if (inDegree[i] == 0 && mustRemove.contains(beams.get(i))) {
                    q.add(i);
                }
            }
            List<Integer> order = new LinkedList<>();
            while (!q.isEmpty()) {
                int curr = q.poll();
                order.add(beams.get(curr).num);
                for (int next : graph.get(curr)) {
                    inDegree[next]--;
                    if (inDegree[next] == 0) {
                        q.add(next);
                    }
                }
            }

            // I guess it makes sense?
            if(order.size() != mustRemove.size())
                isImpossible = true;

            if(isImpossible) // so we know when to print "Disaster" or "False Alarm"
                return null;
            else
                return order;
        }
        static List<Integer> solve2(int nRows, int nCols, int chosenSize, int chosenStart, List<Beam> beams) {
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
                    if (i == j) continue;
                    Beam b2 = beams.get(j);

                    // If b is blocked by b2, b2 must move FIRST.
                    // Edge: b2 -> b
                    if (b.isBlockedBy(b2, nRows, nCols)) {
                        graph.get(j).add(i);
                        reverseGraph.get(i).add(j); // For tracing dependencies backwards
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

            // Propagate necessity backwards: if 'curr' is necessary, and 'j' blocks 'curr', 'j' is necessary.
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
                if (!isNecessary[i]) continue;
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
                return new ArrayList<>(); // "False Alarm"
            }
            if (order.size() != necessaryCount) {
                return null; // "Disaster" (Cycle detected)
            }

            return order;
        }
    }

    public static void main(String[] args) throws IOException{

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        int nTests = Integer.parseInt(in.readLine());

        for(int i = 0; i < nTests; i++){
            String[] line = in.readLine().split(" ");
            int nRows = Integer.parseInt(line[0]);
            int nCols = Integer.parseInt(line[1]);
            line = in.readLine().split(" ");
            int chosenSize = Integer.parseInt(line[0]);
            int chosenStart = Integer.parseInt(line[1]);
            int nBeams =  Integer.parseInt(in.readLine());
            List<Solver.Beam> beams = new ArrayList<>(nBeams);
            for (int j = 1; j <= nBeams; j++) {
                line = in.readLine().split(" ");
                int row = Integer.parseInt(line[0]);
                int col = Integer.parseInt(line[1]);
                int length = Integer.parseInt(line[2]);
                char direction = line[3].charAt(0);
                Solver.Beam b = new Solver.Beam(j,row, col, length, direction);
                beams.add(b);
            }
            List<Integer> order = Solver.solve2(nRows,nCols,chosenSize,chosenStart,beams);
            if(order == null)
                System.out.println("Disaster");
            else if(order.isEmpty())
                System.out.println("False Alarm");
            else
                System.out.println(order);
        }

    }
}
