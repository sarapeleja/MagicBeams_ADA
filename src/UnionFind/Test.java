import java.util.*;

public class Test {
    public int minDuration(Digraph graph) {
        int n = graph.numVertices();
        if (n == 0) return 0;

        int[] inDegree = new int[n];
        int[] duration = new int[n];
        
        // 1. Calcular o grau de entrada de todos os vértices
        for (int v = 0; v < n; v++) {
            for (int neighbor : graph.adj(v)) {
                inDegree[neighbor]++;
            }
        }

        // 2. Fila para processar vértices cujas dependências foram satisfeitas
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                queue.add(i);
                duration[i] = 1; // Primeiro semestre
            }
        }

        int maxSemesters = 0;

        // 3. Processamento por níveis
        while (!queue.isEmpty()) {
            int v = queue.poll();
            maxSemesters = Math.max(maxSemesters, duration[v]);

            for (int neighbor : graph.adj(v)) {
                // O vizinho só pode ser feito após v
                duration[neighbor] = Math.max(duration[neighbor], duration[v] + 1);
                
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    queue.add(neighbor);
                }
            }
        }

        return maxSemesters;
    }
}
