package app;

import app.model.Matrix;
import app.threadPools.MatrixBrain;
import app.threads.SystemExplorer;
import app.threads.TaskCordinator;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

public class Main {
    public static int sysExplorerSleepTime;
    public static int maximumFileChunkSize;
    public static int maximumRowsSize;

    public static String startDir;

    public static boolean running = true;

    public static final CopyOnWriteArrayList<String> dirsToSearch = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        SystemExplorer systemExplorer = new SystemExplorer();
        Thread systemExplorerThread = new Thread(systemExplorer);
        TaskCordinator taskCordinator = new TaskCordinator();
        Thread taskCordinatorThread = new Thread(taskCordinator);

        Properties properties = new Properties();
        FileInputStream fis = null;

        try{
            fis = new FileInputStream("app.properties");
            properties.load(fis);

            sysExplorerSleepTime = Integer.parseInt(properties.getProperty("sys_explorer_sleep_time"));
            maximumFileChunkSize = Integer.parseInt(properties.getProperty("maximum_file_chunk_size"));
            maximumRowsSize = Integer.parseInt(properties.getProperty("maximum_rows_size"));
            //dirsToSearch.add(properties.getProperty("start_dir"));
            startDir = properties.getProperty("start_dir");
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    System.err.println("Error closing FileInputStream: " + e.getMessage());
                }
            }
        }

        systemExplorerThread.start();
        taskCordinatorThread.start();

        while (running) {
            String[] command = scanner.nextLine().split(" ");

            switch (command[0].toLowerCase()) {
                case "dir":
                    if (command.length > 1) {
                        System.out.println("Adding dir " + command[1]);
                        dirsToSearch.add(command[1]);
                    } else {
                        System.out.println("Please provide a directory path");
                    }
                    break;
                case "info":
                    if (command.length > 1){
                        if (command[1].equals("-all")) {
                            boolean asc = false, desc = false, s = false, e = false;
                            for (int i = 2; i < command.length; i++) {
                                if (command[i].equals("-asc")) {
                                    asc = true;
                                } else if (command[i].equals("-desc")) {
                                    desc = true;
                                } else if (command[i].equals("-s")) {
                                    s = true;
                                } else if (command[i].equals("-e")) {
                                    e = true;
                                } else {
                                    System.out.println("Usage: info <matrix_name>/-all -asc (sorts by ascending order)/-desc (sorts by descending order) -s (show first 10 matrices)/-e (show last 10 matrices)");
                                    break;
                                }
                            }
                            MatrixBrain.getAllMatrices(asc, desc, s, e).forEach(matrix -> {
                                if (matrix.matrix.isDone()) {
                                    System.out.println("[" + matrix.name + "] " + matrix.rows + "x" + matrix.cols + " | " + "ready" + " | " + matrix.filePath);
                                }
                                else{
                                    System.out.println("[" + matrix.name + "] " + matrix.rows + "x" + matrix.cols + " | " + "not ready" + " | " + matrix.filePath);
                                }
                            });
                        } else {
                            if (command.length == 2){
                                Matrix matrix = MatrixBrain.getMatrixResults(command[1]);
                                if (matrix == null){
                                    System.out.println("Matrix not found");
                                } else {
                                    if (matrix.matrix.isDone()) {
                                        System.out.println("[" + matrix.name + "] " + matrix.rows + "x" + matrix.cols + " | " + "ready" + " | " + matrix.filePath);
                                    }
                                    else{
                                        System.out.println("[" + matrix.name + "] " + matrix.rows + "x" + matrix.cols + " | " + "not ready" + " | " + matrix.filePath);
                                    }
                                }
                            }
                            else {
                                System.out.println("Usage: info <matrix_name>/-all -asc (sorts by ascending order)/-desc (sorts by descending order) -s (show first 10 matrices)/-e (show last 10 matrices)");
                                break;
                            }
                        }
                    }
                    else {
                        System.out.println("Usage: info <matrix_name>/-all -asc (sorts by ascending order)/-desc (sorts by descending order) -s (show first 10 matrices)/-e (show last 10 matrices)");
                    }
                    break;
                case "multiply":
                    if (command.length >= 2) {
                        String[] matrices = command[1].split(",");
                        if (matrices.length != 2) {
                            System.out.println("Usage: multiply <matrix_name>,<matrix_name> -async -name <result_matrix_name>");
                            break;
                        }
                        boolean async = false;
                        String name = null;
                        boolean err = false;
                        for (int i = 2; i < command.length; i++) {
                            if (command[i].equals("-async")) {
                                async = true;
                            } else if (command[i].equals("-name") && i + 1 < command.length && !command[i + 1].equals("-async")) {
                                name = command[i + 1];
                                i += 1;
                            }
                            else {
                                System.out.println("Usage: multiply <matrix_name>,<matrix_name> -async -name <result_matrix_name>");
                                err = true;
                                break;
                            }
                        }
                        if (err){
                            break;
                        }
                        Matrix matrix = MatrixBrain.requireMultiplication(name, matrices[0], matrices[1], async);
                        if (matrix != null) {
                            if (matrix.matrix != null) {
                                if (matrix.matrix.isDone()){
                                    System.out.println("[" + matrix.name + "] " +  matrices[0] + " x " + matrices[1] + " calculation completed");
                                } else {
                                    System.out.println("Task not finished yet.");
                                }
                            } else {
                                System.out.println("Task not finished yet.");
                            }
                        } else {
                            System.out.println("Error in matrix multiplication");
                        }
                    } else {
                        System.out.println("Usage: multiply <matrix_name>,<matrix_name> -async -name <result_matrix_name>");
                    }
                    break;
                case "save":
                    if (command.length == 5) {
                        String name = null, file = null;
                        for (int i = 1; i < command.length; i++) {
                            if (command[i].equals("-name") && i + 1 < command.length && !command[i + 1].equals("-file")) {
                                name = command[i + 1];
                                i += 1;
                            } else if (command[i].equals("-file") && i + 1 < command.length && !command[i + 1].equals("-name")) {
                                file = command[i + 1];
                                i += 1;
                            }
                            else{
                                System.out.println("Usage: save -name <matrix_name> -file <file_name>");
                            }
                        }
                        MatrixBrain.saveMatrix(name, file);
                    } else {
                        System.out.println("Usage: save -name <matrix_name> -file <file_name>");
                    }
                    break;
                case "clear":
                    if (command.length == 2) {
                        MatrixBrain.deleteMatrix(command[1], true);
                    } else {
                        System.out.println("Usage: clear <matrix_name>/<file_name>");
                    }
                    break;
                case "stop":
                    System.out.println("Stopping...");
                    running = false;
                    MatrixBrain.shutDown();
                    break;
                default:
                    System.out.println("Unknown command");
            }
        }

        try {
            systemExplorerThread.join();
            taskCordinatorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Main/CLI has stopped.");
    }
}