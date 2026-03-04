package tessla2.FlexAnalytics.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;


@Data
@AllArgsConstructor
public class DataSet {
    private final String[] headers;
    private final int numVars;
    private final List<double[]> inputs;
    private final double[] output;



    // Extracts an entire column from an entry variable
    public double[] extractColumn(int index) {
        if (index < 0 || index >= numVars)
            throw new IndexOutOfBoundsException("Column index out of bounds"); // IndexOutOfBoundsException para evitar acesso a colunas inexistentes

        double[] col = new double[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            col[i] = inputs.get(i)[index];
        }
        return col;
    }

    // Returns DataSet line numbers
    public int getRowCount() {
        return inputs.size();
    }

    // Returns output variables
    public String getOutputHeader() {
        return headers[numVars];
    }


}
