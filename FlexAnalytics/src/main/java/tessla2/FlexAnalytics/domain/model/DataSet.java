package tessla2.FlexAnalytics.domain.model;

import java.util.List;

public record DataSet(String[] headers, List<double[]> inputs, double[] output, int numVars) {
    public double[] extractColumn(int index) {
        if (index < 0 || index >= numVars) {
            throw new IndexOutOfBoundsException("Column index out of bounds");
        }

        double[] col = new double[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            col[i] = inputs.get(i)[index];
        }
        return col;
    }

    public int getRowCount() {
        return inputs.size();
    }

    public String getOutputHeader() {
        return headers[numVars];
    }

    public int getNumVars() {
        return numVars;
    }
}
