package app.threads;

import app.TaskQueue;
import app.model.Task;
import app.model.TaskType;
import app.threadPools.MatrixExtractor;
import app.threadPools.MatrixMultiplier;

public class TaskCordinator implements Runnable {

    private boolean running = true;

    @Override
    public void run() {
        while (running){
            try {
                Task task = TaskQueue.waitForTask();
                if (task.getTaskType().equals(TaskType.MULTIPLY)){
                    MatrixMultiplier.addTask(task);
                } else if (task.getTaskType().equals(TaskType.CREATE)) {
                    MatrixExtractor.addTask(task);
                }
                else if (task.getTaskType().equals(TaskType.SHUT_DOWN)){
                    MatrixMultiplier.addTask(task);
                    MatrixExtractor.addTask(task);
                    running = false;
                }
                else {
                    throw new IllegalArgumentException("Invalid task type");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Task coordinator has stopped.");
    }
}
