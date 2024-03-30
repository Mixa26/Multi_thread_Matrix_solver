package app.threadPools;

import app.model.Matrix;
import app.model.Task;
import app.model.TaskType;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class MatrixMultiplier {

    static final ForkJoinPool threadPool = new ForkJoinPool();

    public static void addTask(Task task) {
        if (task.getTaskType().equals(TaskType.SHUT_DOWN)){
            threadPool.shutdown();
            System.out.println("Matrix multiplier has stopped.");
            return;
        }
        Matrix res = task.getMatrixAInfo();
        Future<List<List<BigInteger>>> matrix = (Future<List<List<BigInteger>>>) threadPool.submit(task);
        res.matrix = matrix;
        MatrixBrain.addMatrixMultiplicationResult(res);
    }
}
