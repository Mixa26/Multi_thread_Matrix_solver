# Multi_thread_Matrix_solver

A university project for parallel a matrix extraction, multiplication system.

# The concurrent system explanation

![system_architecture](images/Immagine.jpg)
<br>
The system idea is that SystemExplorer, TaskCordinator and Main/MatrixBrain are threads, and<br>
MatrixExtractor, MatrixMultiplier and MatrixBrain have thread pools.<br>
The SystemExplorer is constantly checking for new files in provided directory (one added with dir command), <br>
or for changes in existing ones. When it finds changes it adds tasks to the TaskQueue which sends them off to<br>
the TaskCoordinator which sends of correspondind tasks to MatrixExtractor. The MatrixExtractor concurrently reads<br>
the file and makes another task which gets added to the TaskQueue and that is calculating the square matrix of the newly<br>
added matrix. That gets through TaskCordinator again, then to MatrixMultiplier. Both results from MatrixMultiplier and<br>
MatrixExtractor get send to the MatrixBrain which keeps track of all the extracted and calculated matrices.<br>
When matrix multiplication is requested through the CLI it gets send to the MatrixBrain and the to the<br>
TaskQueue->TaskCoordinator->MatrixMultiplier->back to MatrixBrain. Multiplication can be asynchrounous<br>
(CLI doesn't wait for the result of multiplication) or synchrounous (CLI will wait for the result). How does this<br>
synchrounous call work? CLI requests synchrounous multiplication, MatrixBrain puts a task as follows and waits on the Matrix<br>
MatrixBrain->TaskQueue->TaskCoordinator->MatrixMultiplier->MatrixBrain, MatrixBrain gets notified, now does a get on the<br>
Future object (result of multiplication), receives it and unblocks CLI. <br>
Multiplication and extraction is done concurrently through thread pools. Extraction is done by splitting rows of file<br>
on to threads, and multiplication by splitting multiplication of rows onto threads.<br>
Saveing a file is done through the thread pool as well where one file saveing is given to one thread. The thread pool<br>
for saveing a file is inside MatrixBrain.<br>
Stopping of the system is done through poison pills and is zombie safe.

# How to run

Download the project and run it through IntelliJ IDEA or IDEA of choice.<br>
You can interact with the app through the console. The commands are following: <br>
<br>
1. dir dir_name - adds directory to read matrix (.rix) files, also calculates square matrix of every read matrix <br>
2. info matrix_name - prints out info about the matrix (is case sensitive!)<br>(format of output: [matrix name] RowsxColumns | ready(read from file or calculater) | absolute file path, null if not saved to a file or isnt a matrix read from a file <br>
3. multiply mat1,mat2 -async -name <name> - multiplies matrices with names mat1 and mat2, -async is optional for asynchrounous calculation, -name also optional for giving a name to the matrix (can fetch values with info <mat_name>) <br>
4. save -name mat_name -file file_name - saves matrix into a .rix file with a provided name and provided file_name (add .rix at the end) <br>
5. clear mat_name/file_name - clears data for a matrix with provided name or file name associated with the matrix and all result matrices calculated with this matrix <br>
6. stop - stops the whole system
