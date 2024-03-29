package app;

import app.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskQueue {

    public static final List<Task> tasks = new ArrayList<>();
    public static void addTask(Task task) {
        synchronized (tasks) {
            tasks.add(task);
            tasks.notify();
        }
    }

    public static Task waitForTask() throws InterruptedException {
        synchronized (tasks) {
            if (tasks.isEmpty()) {
                tasks.wait();
            }
            return tasks.remove(0);
        }
    }
}

