
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Main {
    
    private static void printOrder(List<Integer> order) {
        if (order == null || order.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i : order) {
            sb.append(i).append(' ');
        }

        // Correct way to remove the last character
        if (!sb.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }

        System.out.println(sb);
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

            int nBeams = Integer.parseInt(in.readLine());
            MagicBeamSolver solver = new MagicBeamSolver(nRows, nCols, nBeams);

            for (int j = 1; j <= nBeams; j++) {
                st = new StringTokenizer(in.readLine());
                int row = Integer.parseInt(st.nextToken());
                int col = Integer.parseInt(st.nextToken());
                int length = Integer.parseInt(st.nextToken());
                char direction = st.nextToken().charAt(0);
                solver.addBeam(j, row, col, length, direction);
            }

            MagicBeamSolver.Result result = solver.solve(chosenSize, chosenStart);
            if (!result.success()) {
                System.out.println("Disaster");
            } else if (result.order().isEmpty()) {
                System.out.println("False alarm");
            } else {
                printOrder(result.order());
            }

        }

    }
}
