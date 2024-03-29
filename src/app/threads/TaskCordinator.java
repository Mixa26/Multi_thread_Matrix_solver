package app.threads;

import app.TaskQueue;
import app.model.Task;
import app.model.TaskType;
import app.threadPools.MatrixExtractor;
import app.threadPools.MatrixMultiplier;

public class TaskCordinator implements Runnable {

    @Override
    public void run() {
        while (true){
            try {
                Task task = TaskQueue.waitForTask();
                if (task.getTaskType().equals(TaskType.MULTIPLY)){
                    MatrixMultiplier.addTask(task);
                } else if (task.getTaskType().equals(TaskType.CREATE)) {
                    MatrixExtractor.addTask(task);
                }
                else {
                    throw new IllegalArgumentException("Invalid task type");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
