package io.github.qiu2014;

import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Attributes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

public class ShellRunner {
    private Path currentdir;
    private final Map<String, String> env;
    private Main main;
    private CommandHandler commandHandler;
    private LineReader lineReader;
    private History history;
    private volatile boolean running = true;

    public ShellRunner(Main main) {
        this.main = main;
        this.currentdir = Paths.get(System.getProperty("user.home"));
        this.env = new HashMap<>(System.getenv());

        try {
            initLineReader();
        } catch (IOException e) {
            System.err.println("Failed to initialize console: " + e.getMessage());
        }
    }

    private void initLineReader() throws IOException {
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        // Set up signal handlers
        terminal.handle(Terminal.Signal.INT, signal -> {
            System.out.println("^C");
            // Interrupt any running command
            if (commandHandler != null && commandHandler.isCommandRunning()) {
                commandHandler.interruptCommand();
            } else {
                // If no command is running, just show a new prompt
                System.out.print(createPrompt());
                System.out.flush();
            }
        });

        Parser parser = new DefaultParser();
        history = new DefaultHistory();

        lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .history(history)
                .variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".jsh_history"))
                .variable(LineReader.HISTORY_SIZE, 1000)
                .variable(LineReader.HISTORY_FILE_SIZE, 1000)
                .option(LineReader.Option.HISTORY_BEEP, false)
                .option(LineReader.Option.HISTORY_IGNORE_SPACE, true)
                .option(LineReader.Option.HISTORY_REDUCE_BLANKS, true)
                .option(LineReader.Option.INSERT_TAB, true)
                .build();
    }

    public void setCommandHandler(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    public void run() {
        System.out.println("JSH [v" + main.version + "]");
        System.out.println("(c) qiu2014. Licensed under GNU GENERAL PUBLIC LICENSE");
        System.out.println("Type 'help' for available commands");
        System.out.println("Press Ctrl+C to interrupt command, Ctrl+D to exit");

        while (running) {
            try {
                String prompt = createPrompt();

                // Read line with history support
                String input = lineReader.readLine(prompt).trim();

                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("Bye!");
                    break;
                }

                if (input.isEmpty()) {
                    continue;
                }

                // Add to history (unless it's a duplicate of the last command)
                history.add(input);

                // Process the command
                if (commandHandler != null) {
                    commandHandler.processCommand(input);
                } else {
                    System.err.println("Error: Command handler not initialized");
                }

            } catch (UserInterruptException e) {
                // Ctrl+C pressed during input
                System.out.println("^C");
                continue;
            } catch (EndOfFileException e) {
                // Ctrl+D pressed
                System.out.println();
                System.out.println("Bye!");
                break;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        // Clean up
        if (lineReader != null && lineReader.getTerminal() != null) {
            try {
                lineReader.getTerminal().close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private String createPrompt() {
        String homeDir = System.getProperty("user.home");
        String currentPath = currentdir.toString();

        if (currentPath.startsWith(homeDir)) {
            String relativePath = currentPath.substring(homeDir.length());
            if (relativePath.isEmpty()) {
                return "~$ ";
            } else {
                return "~" + relativePath + "$ ";
            }
        } else {
            return currentPath + "$ ";
        }
    }

    public Path getCurrentdir() {
        return currentdir;
    }

    public void setCurrentdir(Path currentdir) {
        this.currentdir = currentdir;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void shutdown() {
        running = false;
    }
}