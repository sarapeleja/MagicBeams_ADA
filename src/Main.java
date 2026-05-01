import UnionFind.UnionFind;
import UnionFind.UnionFindInArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;


public class Main {
    record Beam(int row, int col, int length, char direction){

    }
    
    static class Solver{
       
        static List<Integer> solve(int nRows, int nCols, int chosenSize, int chosenStart, List<Beam> beams){
            List<Integer> order = new LinkedList<>();
            // Change this upon determining it is impossible to solve.
            boolean isImpossible = false;
            UnionFind u = new UnionFindInArray(beams.size());

            if(isImpossible) // so we know when to print "Disaster" or "False Alarm"
                return null; 
            else
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
            List<Beam> beams = new LinkedList<>();
            for (int j = 0; j < nBeams; j++) {
                line = in.readLine().split(" ");
                 int row = Integer.parseInt(line[0]);
                 int col = Integer.parseInt(line[1]);
                 int length = Integer.parseInt(line[2]);
                 char direction = line[3].charAt(0);
                 Beam b = new Beam(row, col, length, direction);
                 beams.add(b);
            }
            System.out.println(Solver.solve(nRows,nCols,chosenSize,chosenStart,beams));
        }
        
    }
}
