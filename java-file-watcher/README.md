# java-file-watcher

A simple Java application that watches a directory for file changes and executes a command when a file matching a 
glob pattern is created, modified, or deleted.

## Usage
1. Run the application by executing the `DirectoryWatcherApp.java` file.
2. Enter the directory to watch in the "Directory to Monitor" field.
3. Enter the glob pattern(s) to match in the "File Glob Pattern" field. For example, to match all `.txt` files, enter `*.txt`.
4. Enter the command to execute in the "Command to Trigger" field. Following placeholders are supported:
    - `${file}` - the name of the file that was created, modified, or deleted.
    - `${file_dir}` - the directory of the file that was created, modified, or deleted.
    - `${file_with_dir}` - the full path of the file that was created, modified, or deleted.
5. Check the "Monitor Subdirectories" checkbox to monitor subdirectories as well.
6. Click the "Watch Active" checkbox to start watching the directory.
7. To stop watching, uncheck the "Watch Active" checkbox.

## Building
To build the application, run the `build.sh` script.
