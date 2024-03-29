package app.model;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Future;

public class Matrix {
    public String name;

    public String mulName;
    public int rows;
    public int cols;
    public String filePath;

    public Future<List<List<BigInteger>>> matrix;

    public Matrix(String name, String mulName, int rows, int cols, String filePath) {
        this.name = name;
        this.mulName = mulName;
        this.rows = rows;
        this.cols = cols;
        this.filePath = filePath;
    }

    public Matrix(Matrix matrix) {
        this.name = matrix.name;
        this.mulName = matrix.mulName;
        this.rows = matrix.rows;
        this.cols = matrix.cols;
        this.filePath = matrix.filePath;
        this.matrix = matrix.matrix;
    }

}
