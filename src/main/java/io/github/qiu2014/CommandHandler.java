package io.github.qiu2014;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class CommandHandler {
    private Main main;
    private Map<String, String> environment;
    private AtomicBoolean commandRunning = new AtomicBoolean(false);
    private Thread commandThread;

    public CommandHandler(Main main) {
        this.main = main;
        if (main.getShellRunner() != null) {
            this.environment = main.getShellRunner().getEnv();
        } else {
            this.environment = System.getenv();
        }
    }

    public boolean isCommandRunning() {
        return commandRunning.get();
    }

    public void interruptCommand() {
        if (commandRunning.get() && commandThread != null) {
            commandThread.interrupt();
            System.out.println("\nCommand interrupted");
            commandRunning.set(false);
        }
    }

    public void processCommand(String command) {
        if (main.getShellRunner() != null) {
            this.environment = main.getShellRunner().getEnv();
        }

        // Don't process if a command is already running
        if (commandRunning.get()) {
            System.out.println("Command already running, please wait or press Ctrl+C to interrupt");
            return;
        }

        commandRunning.set(true);
        commandThread = Thread.currentThread();

        try{
            if(command.startsWith("cd ")) {
                changeDir(command.substring(3).trim());
            } else if(command.equalsIgnoreCase("pwd")){
                System.out.println(main.getShellRunner().getCurrentdir().toString());
            } else if(command.startsWith("echo ")) {
                echo(command.substring(5).trim());
            } else if(command.equalsIgnoreCase("env")) {
                printEnvironment();
            } else if(command.equalsIgnoreCase("help") || command.equals("?")) {
                printHelp();
            } else if(command.equals("ls") || command.startsWith("ls ")) {
                listDirectory(command);
            } else {
                System.out.println("jsh: command not found: " + command);
            }

            System.out.flush();

        } catch (Exception e) {
            if (Thread.interrupted()) {
                System.out.println("Command interrupted");
            } else {
                System.out.println("Unexpected error occurred: " + e.getMessage());
            }
            System.out.flush();
        } finally {
            commandRunning.set(false);
            commandThread = null;
        }
    }

    private void changeDir(String path) {
        // Check for interruption
        if (Thread.interrupted()) {
            return;
        }

        try {
            Path newPath;

            // Handle ~ (home directory)
            if (path.startsWith("~")) {
                String homeDir = System.getProperty("user.home");
                if (path.equals("~")) {
                    newPath = Paths.get(homeDir);
                } else {
                    // Handle ~/something
                    String subPath = path.substring(1); // Remove ~
                    newPath = Paths.get(homeDir + subPath);
                }
            }
            // Handle absolute paths (Windows and Unix)
            else if (path.startsWith("/") || path.matches("^[A-Za-z]:.*")) {
                newPath = Paths.get(path);
            }
            // Handle parent directory
            else if (path.equals("..")) {
                newPath = main.getShellRunner().getCurrentdir().getParent();
                if(newPath == null) {
                    newPath = main.getShellRunner().getCurrentdir();
                }
            }
            // Handle relative paths
            else {
                newPath = main.getShellRunner().getCurrentdir().resolve(path);
            }

            newPath = newPath.normalize();

            if (Files.isDirectory(newPath)) {
                main.getShellRunner().setCurrentdir(newPath);
                System.setProperty("user.dir", main.getShellRunner().getCurrentdir().toString());
            } else {
                System.out.println("cd: " + path + ": No such directory");
            }

        } catch (InvalidPathException e) {
            System.out.println("cd: " + path + ": Invalid path");
        }

        System.out.flush();
    }

    private void listDirectory(String command) {
        // Check for interruption
        if (Thread.interrupted()) {
            return;
        }

        try {
            // Parse ls options
            String[] parts = command.split("\\s+");
            boolean showAll = false;
            boolean longFormat = false;
            Path targetPath = main.getShellRunner().getCurrentdir();

            // Parse options
            for (int i = 1; i < parts.length; i++) {
                // Check for interruption during parsing
                if (Thread.interrupted()) {
                    return;
                }

                String part = parts[i];
                if (part.startsWith("-")) {
                    if (part.contains("a")) showAll = true;
                    if (part.contains("l")) longFormat = true;
                } else {
                    // It's a path argument
                    targetPath = main.getShellRunner().getCurrentdir().resolve(part);
                }
            }

            // Check if path exists
            if (!Files.exists(targetPath)) {
                System.out.println("ls: " + targetPath + ": No such file or directory");
                System.out.flush();
                return;
            }

            // If it's a file, just show that file
            if (!Files.isDirectory(targetPath)) {
                if (longFormat) {
                    printLongFormat(targetPath);
                } else {
                    System.out.println(targetPath.getFileName());
                }
                System.out.flush();
                return;
            }

            // List directory contents
            try (Stream<Path> stream = Files.list(targetPath)) {
                boolean finalShowAll = showAll;
                boolean finalLongFormat = longFormat;
                stream.sorted().forEach(path -> {
                    // Check for interruption for each file
                    if (Thread.interrupted()) {
                        return;
                    }

                    String fileName = path.getFileName().toString();

                    // Skip hidden files if not showAll
                    if (!finalShowAll && fileName.startsWith(".")) {
                        return;
                    }

                    if (finalLongFormat) {
                        printLongFormat(path);
                    } else {
                        // Add indicator based on file type
                        if (Files.isDirectory(path)) {
                            System.out.print(fileName + "/  ");
                        } else if (Files.isExecutable(path)) {
                            System.out.print(fileName + "*  ");
                        } else {
                            System.out.print(fileName + "  ");
                        }
                    }
                });

                if (!longFormat) {
                    System.out.println(); // New line after short format
                }
                System.out.flush();
            }

        } catch (IOException e) {
            if (!Thread.interrupted()) {
                System.out.println("ls: Error reading directory: " + e.getMessage());
            }
            System.out.flush();
        }
    }

    private void printLongFormat(Path path) {
        // Check for interruption
        if (Thread.interrupted()) {
            return;
        }

        try {
            // File permissions
            String permissions = getPermissions(path);

            // Number of links (not easily available in Java, use placeholder)
            String links = "1";

            // Owner and group
            String owner = "unknown";
            String group = "unknown";
            try {
                owner = Files.getOwner(path).getName();
                // Group is more complex, use placeholder
            } catch (Exception e) {
                // Ignore
            }

            // File size
            long size = Files.size(path);
            String sizeStr = formatSize(size);

            // Last modified time
            FileTime lastModified = Files.getLastModifiedTime(path);
            String modifiedTime = formatTime(lastModified);

            // File name
            String name = path.getFileName().toString();
            if (Files.isDirectory(path)) {
                name += "/";
            } else if (Files.isExecutable(path)) {
                name += "*";
            }

            System.out.printf("%s %2s %-8s %-8s %8s %s %s%n",
                    permissions, links, owner, group, sizeStr, modifiedTime, name);

        } catch (IOException e) {
            if (!Thread.interrupted()) {
                System.out.println("Error getting file info: " + e.getMessage());
            }
        }
    }

    private String getPermissions(Path path) {
        StringBuilder perms = new StringBuilder();

        // File type
        if (Files.isDirectory(path)) {
            perms.append('d');
        } else if (Files.isSymbolicLink(path)) {
            perms.append('l');
        } else {
            perms.append('-');
        }

        // Owner permissions
        perms.append(Files.isReadable(path) ? 'r' : '-');
        perms.append(Files.isWritable(path) ? 'w' : '-');
        perms.append(Files.isExecutable(path) ? 'x' : '-');

        // Group permissions (simplified - assume same as owner)
        perms.append(Files.isReadable(path) ? 'r' : '-');
        perms.append(Files.isWritable(path) ? 'w' : '-');
        perms.append(Files.isExecutable(path) ? 'x' : '-');

        // Others permissions (simplified)
        perms.append(Files.isReadable(path) ? 'r' : '-');
        perms.append(Files.isWritable(path) ? 'w' : '-');
        perms.append(Files.isExecutable(path) ? 'x' : '-');

        return perms.toString();
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", size / Math.pow(1024, exp), pre);
    }

    private String formatTime(FileTime fileTime) {
        long currentTime = System.currentTimeMillis();
        long fileTimeMs = fileTime.toMillis();
        long sixMonthsAgo = currentTime - (180L * 24 * 60 * 60 * 1000);

        SimpleDateFormat format;
        if (fileTimeMs > sixMonthsAgo) {
            // Recent files: show time
            format = new SimpleDateFormat("MMM dd HH:mm");
        } else {
            // Older files: show year
            format = new SimpleDateFormat("MMM dd  yyyy");
        }

        return format.format(new Date(fileTimeMs));
    }

    private void echo(String text) {
        if (Thread.interrupted()) {
            return;
        }

        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        } else if (text.startsWith("'") && text.endsWith("'")) {
            text = text.substring(1, text.length() - 1);
        }

        text = expandEnvVar(text);

        System.out.println(text);
        System.out.flush();
    }

    private String expandEnvVar(String text) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '$') {
                i++;
                StringBuilder varName = new StringBuilder();
                while (i < text.length() && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_')) {
                    varName.append(text.charAt(i));
                    i++;
                }
                String value = environment.getOrDefault(varName.toString(), "");
                result.append(value);
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }
        return result.toString();
    }

    private void printEnvironment() {
        if (Thread.interrupted()) {
            return;
        }

        environment.forEach((key, value) -> System.out.println(key + "=" + value));
        System.out.flush();
    }

    private void printHelp() {
        if (Thread.interrupted()) {
            return;
        }

        System.out.println("JSH - Java Shell v" + main.version);
        System.out.println("================");
        System.out.println("Built-in commands only (no external commands):");
        System.out.println("  cd <dir>     - Change directory (supports ~ for home)");
        System.out.println("  pwd          - Print working directory");
        System.out.println("  ls [options] [path] - List directory contents");
        System.out.println("     Options:");
        System.out.println("       -a       Show all files (including hidden)");
        System.out.println("       -l       Long format with details");
        System.out.println("  echo <text>  - Display text");
        System.out.println("  env          - Show environment variables");
        System.out.println("  help         - Show this help");
        System.out.println("  exit         - Exit the shell");
        System.out.println("\nPress Ctrl+C to interrupt a running command");
        System.out.flush();
    }
}