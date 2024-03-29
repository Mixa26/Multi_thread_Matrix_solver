package app.threadPools;

import app.TaskQueue;
import app.model.Matrix;
import app.model.Task;
import app.model.TaskType;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class MatrixExtractor {

    static final ForkJoinPool threadPool = new ForkJoinPool();

    public static void addTask(Task task){
        Matrix res = task.getMatrixInfo();
        Future<List<List<BigInteger>>> matrix = (Future<List<List<BigInteger>>>) threadPool.submit(task);
        res.filePath = task.matrixFile.getAbsolutePath();
        res.matrix = matrix;
        MatrixBrain.addMatrixSearchResult(res);
        /*
        try {
            TaskQueue.addTask(new Task(res, matrix.get(), matrix.get(), TaskType.MULTIPLY, 0, matrix.get().size()));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        */
    }
}
