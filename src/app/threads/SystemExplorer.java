package app.threads;

import app.TaskQueue;
import app.model.Matrix;
import app.model.Task;
import app.model.TaskType;
import app.threadPools.MatrixBrain;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static app.Main.dirsToSearch;
import static app.Main.sysExplorerSleepTime;
import static app.Main.startDir;
import static app.Main.running;

public class SystemExplorer implements Runnable {

    private static List<String> files = new ArrayList<>();
    private static List<Long> filesLastModified = new ArrayList<>();
    private static List<File> filesToScan = new ArrayList<>();

    private static final CopyOnWriteArrayList<String> toRemove = new CopyOnWriteArrayList<>();

    private static boolean shouldDelete = false;

    public static void searchFiles(String directoryPath, String dirToRem, String extension) {

        Path dir = Paths.get(directoryPath);

        if (!Files.exists(dir)) {
            dirsToSearch.remove(dirToRem);
            System.err.println("Directory does not exist: " + directoryPath);
            return;
        }

        if (!Files.isDirectory(dir)){
            dirsToSearch.remove(dirToRem);
            System.err.println(directoryPath + " is not a directory.");
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                if (Files.isDirectory(file)) {
                    System.out.println("Exploring folder|" + file.getFileName());
                    searchFiles(file.toString(), dirToRem, extension);
                } else if (file.getFileName().toString().endsWith(extension)) {

                    String filePath = file.toAbsolutePath().toString();

                    if (!files.contains(filePath)) {
                        System.out.println("Found matrix file|" + file.getFileName());
                        filesToScan.add(file.toFile());
                    }
                    else if (files.contains(filePath) && !filesLastModified.get(files.indexOf(filePath)).equals(file.toFile().lastModified())) {
                        shouldDelete = true;
                        System.out.println("Changes in matrix file|" + file.getFileName());
                        filesLastModified.remove(files.indexOf(filePath));
                        files.remove(filePath);
                        filesToScan.add(file.toFile());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error searching files: " + e.getMessage());
        }
    }

    public static void removeFile(String file){
        synchronized (toRemove){
            toRemove.add(file);
        }
    }

    @Override
    public void run() {
        while (running) {
            for (String dir : dirsToSearch) {
                String workingDir = System.getProperty("user.dir") + startDir;
                searchFiles(workingDir + dir, dir, ".rix");
            }

            for (File file : filesToScan){

                if (shouldDelete) {
                    MatrixBrain.deleteMatrix(file.getAbsolutePath(), false);
                    shouldDelete = false;
                }

                Task newTask = new Task(TaskType.CREATE, file, 0, file.length());
                TaskQueue.addTask(newTask);

                files.add(file.getAbsolutePath());
                filesLastModified.add(file.lastModified());
            }

            filesToScan.clear();

            for (String file : toRemove){
                if (!files.contains(file)){
                    //Can inform the user that the file was a multiplied matrices result save file
                    break;
                }
                filesLastModified.remove(files.indexOf(file));
                files.remove(file);
            }
            toRemove.clear();

            try {
                Thread.sleep(sysExplorerSleepTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        Task shutdownTask = new Task(TaskType.SHUT_DOWN);
        TaskQueue.addTask(shutdownTask);
        System.out.println("System Explorer has stopped.");
    }
}
