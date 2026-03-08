package io.github.qiu2014;

import java.util.NoSuchElementException;

public class Main {
    public final double version = 1.0;
    private CommandHandler commandHandler;
    private ShellRunner shellRunner;

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public void setCommandHandler(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    public ShellRunner getShellRunner() {
        return shellRunner;
    }

    public void setShellRunner(ShellRunner shellRunner) {
        this.shellRunner = shellRunner;
    }

    public static void main(String[] args) {
        Main main = new Main();

        // Create ShellRunner first
        ShellRunner shell = new ShellRunner(main);
        main.setShellRunner(shell);

        // Create CommandHandler
        CommandHandler commandHandler = new CommandHandler(main);
        main.setCommandHandler(commandHandler);

        // Set CommandHandler in ShellRunner
        shell.setCommandHandler(commandHandler);

        // Start the shell
        shell.run();
    }
}