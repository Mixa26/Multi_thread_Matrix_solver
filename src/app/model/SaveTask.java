package app.model;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SaveTask implements Runnable{

    private Matrix matrix;

    private String name;

    private String filePath;

    public SaveTask(Matrix matrix, String filePath) {
        this.matrix = new Matrix(matrix);
        this.filePath = filePath;
    }

    @Override
    public void run() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

            List<List<BigInteger>> matrixA = matrix.matrix.get();

            writer.write("matrix_name=" + matrix.name + ", " + "rows=" + matrixA.size() + ", cols=" + (matrixA.isEmpty() ? 0 : matrixA.get(0).size()));
            writer.newLine();

            for (int j = 0; j < matrixA.get(0).size(); j++) {
                for (int i = 0; i < matrixA.size(); i++) {
                    if (matrixA.get(i).get(j).equals(BigInteger.ZERO)){
                        continue;
                    }
                    writer.write(i + "," + j + " = " + matrixA.get(i).get(j));
                }
                writer.newLine();
            }

            System.out.println("Matrix data has been written to " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing to the file: " + e.getMessage());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
