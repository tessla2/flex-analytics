package tessla2.FlexAnalytics.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public record DataSet(String[] headers, List<double[]> inputs, double[] output, int numVars) {
    // Extrai uma coluna inteira de uma variavel de entrada
    public double[] extractColumn(int index) {
        if (index < 0 || index >= numVars)
            throw new IndexOutOfBoundsException("Column index out of bounds"); // IndexOutOfBoundsException para evitar acesso a colunas inexistentes

        double[] col = new double[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            col[i] = inputs.get(i)[index];
        }
        return col;
    }

    // Retorna o número de linhas do dataset
    public int getRowCount() {
        return inputs.size();
    }

    // Retorna o número de variáveis de saída
    public String getOutputHeader() {
        return headers[numVars];
    }
    public int getNumVars() {
        return numVars;
    }

}
