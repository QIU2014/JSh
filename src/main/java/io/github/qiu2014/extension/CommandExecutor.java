package io.github.qiu2014.extension;

/**
 * Interface for command execution
 */
public interface CommandExecutor {
    /**
     * Execute a command
     * @param args Full command arguments (including command name)
     * @return true if command was handled, false otherwise
     */
    boolean execute(String[] args);

    /**
     * @return Help text for this command
     */
    String getHelp();
}