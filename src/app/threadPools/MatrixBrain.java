package app.threadPools;

import app.TaskQueue;
import app.model.Matrix;
import app.model.SaveTask;
import app.model.Task;
import app.model.TaskType;
import app.threads.SystemExplorer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MatrixBrain {

    private static final List<Matrix> matrices = new ArrayList<>();

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public static void addMatrixSearchResult(Matrix matrix){
        synchronized (matrices) {
            matrices.add(matrix);
        }
        System.out.println("Matrix " + matrix.name + " has been created!");
    }

    public static void addMatrixMultiplicationResult(Matrix matrix){
        synchronized (matrices) {
            for (Matrix mat : matrices){
                if (mat.name.equals(matrix.name)){
                    mat.matrix = matrix.matrix;
                    synchronized (mat){
                        mat.notify();
                    }
                    return;
                }
            }
            //Square matrix calculation result
            matrices.add(matrix);
        }
    }

    public static Matrix getMatrixResults(String name){
        synchronized (matrices) {
            for (Matrix matrix : matrices){
                if (matrix.name.equals(name)){
                    return new Matrix(matrix);
                }
            }
        }
        return null;
    }

    public static Matrix requireMultiplication(String name, String mat1, String mat2, boolean async){
        Matrix toBeCalculated = null;
        List<List<BigInteger>> matrix1 = null, matrix2 = null;
        synchronized (matrices) {
            for (Matrix matrix : matrices) {
                if (matrix.mulName != null && matrix.mulName.equals(mat1 + mat2)) {
                    return matrix;
                }
                if (matrix.name.equals(mat1)) {
                    try {
                        matrix1 = new ArrayList<>(matrix.matrix.get());
                    } catch (InterruptedException | ExecutionException e) {
                        System.err.println("Matrix " + matrix.name + " hasn't been extracted yet!");
                        return null;
                    }
                } else if (matrix.name.equals(mat2)) {
                    try {
                        matrix2 = new ArrayList<>(matrix.matrix.get());
                    } catch (InterruptedException | ExecutionException e) {
                        System.err.println("Matrix " + matrix.name + " hasn't been extracted yet!");
                        return null;
                    }
                }
            }
        }

        if (matrix1 == null) {
            System.out.println("Matrix " + mat1 + " doesn't exist!");
            return null;
        }

        if (matrix2 == null) {
            System.out.println("Matrix " + mat2 + " doesn't exist!");
            return null;
        }

        if (name == null) {
            name = mat1 + mat2;
        }

        if (matrix1.get(0).size() != matrix2.size()){
            System.err.println("Matrix " + mat1 + " and " + mat2 + " can't be multiplied!\n" +
                    "Matrix " + mat1 + " is of dimensions " + matrix1.size() + "x" + matrix1.get(0).size() + "\n" +
                    "Matrix " + mat2 + " is of dimensions " + matrix2.size() + "x" + matrix2.get(0).size());
            return null;
        }

        toBeCalculated = new Matrix(name, mat1+mat2, matrix1.size(), matrix1.get(0).size(), null);
        Task task = new Task(toBeCalculated, matrix1, matrix2, TaskType.MULTIPLY, 0, matrix1.size());

        synchronized (matrices) {
            matrices.add(toBeCalculated);
        }

        System.out.println("Calculating " + mat1 + " x " + mat2 + " ...");

        synchronized (toBeCalculated) {
            TaskQueue.addTask(task);
            if (!async) {
                try {
                    toBeCalculated.wait();
                    toBeCalculated.matrix.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
            return toBeCalculated;
        }
    }

    public static List<Matrix> getAllMatrices(boolean asc, boolean desc, boolean s, boolean e){

        List<Matrix> result;
        synchronized (matrices) {
            result = new ArrayList<>(matrices);
        }

        if (asc){
            result.sort(Comparator.comparing(o -> o.name));
        } else if (desc) {
            result.sort(Comparator.comparing(o -> o.name));
            for (int i = 0; i < result.size() / 2; i++) {
                int j = result.size() - 1 - i;
                Matrix temp = result.get(i);
                result.set(i, result.get(j));
                result.set(j, temp);
            }
        }

        if (s){
            if (result.size() > 10) {
                return result.subList(0, 10);
            }
        } else if (e) {
            if (result.size() > 5) {
                return result.subList(result.size() - 5, result.size());
            }
        }

        return result;
    }

    public static void saveMatrix(String name, String file){
        synchronized (matrices) {
            for (Matrix matrix : matrices) {
                if (matrix.name.equals(name)) {
                    matrix.filePath = System.getProperty("user.dir") + "\\saved\\" + file;
                    SaveTask task = new SaveTask(matrix, matrix.filePath);
                    System.out.println("Saving matrix " + name + " to " + file);
                    threadPool.submit(task);
                    return;
                }
            }
        }
        System.out.println("Matrix " + name + " doesn't exist!");
    }

    public static void deleteMatrix(String nameOrFile){
        String fileOfMatrix = null;
        boolean err = true;

        if (nameOrFile.endsWith(".rix")){
            fileOfMatrix = nameOrFile;

            synchronized (matrices) {
                String matrixName = null;
                Iterator<Matrix> iterator = matrices.iterator();
                while (iterator.hasNext()) {
                    Matrix matrix = iterator.next();
                    if ((matrix.filePath != null && matrix.filePath.endsWith(fileOfMatrix))) {
                        fileOfMatrix = new String(matrix.filePath);
                        matrixName = new String(matrix.name);
                        System.out.println("Deleting matrix " + matrix.name);
                        iterator.remove();
                        err = false;
                    }
                }
                if (err){
                    System.out.println("Matrix with file" + nameOrFile + " doesn't exist!");
                    return;
                }
                iterator = matrices.iterator();
                while (iterator.hasNext()) {
                    Matrix matrix = iterator.next();
                    if ((matrix.mulName != null && matrix.mulName.contains(matrixName)) || matrix.name.equals(matrixName)) {
                        System.out.println("Deleting matrix " + matrix.name);
                        iterator.remove();
                    }
                }
            }
        }
        else{
            synchronized (matrices) {
                Iterator<Matrix> iterator = matrices.iterator();
                while (iterator.hasNext()) {
                    Matrix matrix = iterator.next();
                    if ((matrix.mulName != null && matrix.mulName.contains(nameOrFile)) || matrix.name.equals(nameOrFile)) {
                        if (matrix.name.equals(nameOrFile) && matrix.filePath != null){
                            fileOfMatrix = new String(matrix.filePath);
                        }
                        System.out.println("Deleting matrix " + matrix.name);
                        iterator.remove();
                        err = false;
                    }
                }
                if (err){
                    System.out.println("Matrix with name" + nameOrFile + " doesn't exist!");
                    return;
                }
            }
        }

        if (fileOfMatrix != null) {
            SystemExplorer.removeFile(fileOfMatrix);
        }
    }

    public static void shutDown(){
        threadPool.shutdown();
        System.out.println("Matrix brain has stopped.");
    }
}
