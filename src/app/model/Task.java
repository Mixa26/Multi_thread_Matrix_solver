package app.model;

import app.Main;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;

import static app.Main.maximumRowsSize;
import static app.Main.maximumFileChunkSize;

public class Task extends RecursiveTask {

    private Matrix matrixAInfo, matrixBInfo;
    private List<List<BigInteger>> matrixA, matrixB;
    private TaskType taskType;
    public File matrixFile;

    private int start;
    private int end;

    private long fileStart;

    private long fileEnd;

    //private List<List<BigInteger>> check;

    public Task(TaskType taskType) {
        this.taskType = taskType;
    }

    public Task(Matrix matrixAInfo, List<List<BigInteger>> matrixA, List<List<BigInteger>> matrixB, TaskType taskType, int start, int end) {
        this.matrixAInfo = matrixAInfo;
        this.matrixA = matrixA;
        this.matrixB = matrixB;
        this.taskType = taskType;
        this.start = start;
        this.end = end;
    }

    public Task(TaskType taskType, File matrixFile, long fileStart, long fileEnd, List<List<BigInteger>> matrixA) {
        this.taskType = taskType;
        this.matrixFile = matrixFile;
        this.fileStart = fileStart;
        this.fileEnd = fileEnd;
        this.matrixA = matrixA;
    }

    public Task(TaskType taskType, File matrixFile, long fileStart, long fileEnd) {
        this.taskType = taskType;
        this.matrixFile = matrixFile;
        this.fileStart = fileStart;
        this.fileEnd = fileEnd;
        constructMatrix(readFirstLine());
    }

    private void constructMatrix(String line){
        String[] cl = line.split(",");
        int rows = Integer.parseInt(cl[1].split("=")[1]);
        int cols = Integer.parseInt(cl[2].split("=")[1]);

        matrixA = new ArrayList<>();
        //check = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            ArrayList<BigInteger> row = new ArrayList<>();
            //ArrayList<BigInteger> row1 = new ArrayList<>();
            for (int j = 0; j < cols; j++) {
                row.add(BigInteger.ZERO);
                //row1.add(BigInteger.ZERO);
            }
            matrixA.add(row);
            //check.add(row1);
        }
    }

    public Matrix getMatrixInfo(){
        String line = readFirstLine();
        String[] info = line.split(",");
        String multiplicationName = null;
        if (matrixAInfo != null){
            multiplicationName = matrixAInfo.mulName;
        }
        return new Matrix(info[0].split("=")[1], multiplicationName, Integer.parseInt(info[1].split("=")[1]), Integer.parseInt(info[2].split("=")[1]), matrixFile.getAbsolutePath());
    }

    private String readFirstLine() {
        try (BufferedReader reader = new BufferedReader(new FileReader(matrixFile))) {
            String line = reader.readLine();
            fileStart = line.getBytes().length + 1;
            return line;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<List<BigInteger>> addMatrices(List<List<BigInteger>> matrixF, List<List<BigInteger>> matrixS) {
        List<List<BigInteger>> resultMatrix = new ArrayList<>();
        int rows = matrixF.size();
        int cols = matrixF.get(0).size();

        for (int i = 0; i < rows; i++) {
            List<BigInteger> rowA = matrixF.get(i);
            List<BigInteger> rowB = matrixS.get(i);
            ArrayList<BigInteger> resultRow = new ArrayList<>(cols);

            for (int j = 0; j < cols; j++) {
                BigInteger sum = rowA.get(j).add(rowB.get(j));
                resultRow.add(sum);
            }
            resultMatrix.add(resultRow);
        }
        return resultMatrix;
    }

    private long findNearestNewline(long position, boolean forward) {
        try (RandomAccessFile raf = new RandomAccessFile(matrixFile, "r")) {
            long fileLength = raf.length();

            raf.seek(position);
            if (raf.read() == '\n') {
                return position+1;
            }
            if (!forward) {
                while (position > 0) {
                    position--;
                    raf.seek(position);
                    if (raf.read() == '\n') {
                        return position+1;
                    }
                }
            }
            else {
                while (position < fileLength) {
                    position++;
                    raf.seek(position);
                    if (raf.read() == '\n') {
                        return position+1;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return position;
    }

    private List<String> readLinesFromFile(long start, long end) {
        List<String> lines = new ArrayList<>();
        long bytesRead = 0;

        if (!Files.isReadable(Paths.get(matrixFile.toURI()))) {
            System.err.println("File is not readable or does not exist: " + matrixFile);
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(matrixFile))) {
            reader.skip(start);

            String line;

            while ((line = reader.readLine()) != null && bytesRead < (end - start)) {
                lines.add(line);
                bytesRead += line.getBytes().length + 1;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //why - 2, because last line doesn't have \n at the end
        if (bytesRead - 2 > (end - start)) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    @Override
    protected Object compute() {
        if (taskType.equals(TaskType.CREATE)){
            if ((fileEnd - fileStart) <= maximumFileChunkSize){

                List<String> lines = readLinesFromFile(fileStart, fileEnd);
                for (String line : lines) {
                    if (line.equals("")) {
                        continue;
                    }
                    String[] first = line.split(",");
                    String[] second = first[1].split("=");
                    String val;
                    int row;
                    int col;
                    try {
                        val = second[1].trim();
                        row = Integer.parseInt(first[0].trim());
                        col = Integer.parseInt(second[0].trim());
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing row or column: " + e.getMessage());
                        System.err.println("Line causing the error: " + line);
                        continue;
                    }

                    matrixA.get(row).set(col, new BigInteger(val));
                }
                return matrixA;
            }

            long mid = (fileEnd - fileStart) / 2 + fileStart;
            mid = findNearestNewline(mid, false);
            if (mid == fileStart) {
                mid = findNearestNewline(mid, true);
            } else if (mid == fileEnd){
                maximumFileChunkSize = (int) (fileEnd - fileStart);
            }

            Task left = new Task(TaskType.CREATE, matrixFile, fileStart, mid-2, matrixA);
            Task right = new Task(TaskType.CREATE, matrixFile, mid, fileEnd, matrixA);

            left.fork();
            List<List<BigInteger>> rightRes = (List<List<BigInteger>>) right.compute();
            List<List<BigInteger>> leftRes = (List<List<BigInteger>>) left.join();

            return matrixA;
        }
        else if (taskType.equals(TaskType.MULTIPLY)){
            List<List<BigInteger>> res = new ArrayList<>();
            if ((end - start) <= maximumRowsSize){

                for (int i = start; i < end; i++) {
                    List<BigInteger> row = new ArrayList<>();
                    for (int z = 0; z < matrixB.get(0).size(); z++) {
                        BigInteger acc = BigInteger.ZERO;
                        for (int j = 0; j < matrixA.get(start).size(); j++) {
                            acc = acc.add(matrixA.get(i).get(j).multiply(matrixB.get(j).get(z)));
                        }
                        row.add(acc);
                    }
                    res.add(row);
                }

                return res;
            }

            int mid = (end - start) / 2 + start;

            Task left = new Task(matrixAInfo, matrixA, matrixB, TaskType.MULTIPLY, start, mid);
            Task right = new Task(matrixAInfo, matrixA, matrixB, TaskType.MULTIPLY, mid, end);

            left.fork();
            List<List<BigInteger>> rightRes = (List<List<BigInteger>>) right.compute();
            List<List<BigInteger>> leftRes = (List<List<BigInteger>>) left.join();

            res.addAll(leftRes);
            res.addAll(rightRes);

            return res;
        }
        return null;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public Matrix getMatrixAInfo() {
        return matrixAInfo;
    }

}
