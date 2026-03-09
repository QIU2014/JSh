package io.github.qiu2014;

import io.github.qiu2014.extension.ExtensionCommandHandler;
import io.github.qiu2014.extension.ExtensionLoader;

public class Main {
    public final double version = 1.1;
    private CommandHandler commandHandler;
    private ShellRunner shellRunner;
    private ExtensionCommandHandler extensionCommandHandler;
    private ExtensionLoader extensionLoader;

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

    public ExtensionCommandHandler getExtensionCommandHandler() {
        return extensionCommandHandler;
    }

    public void setExtensionCommandHandler(ExtensionCommandHandler extensionCommandHandler) {
        this.extensionCommandHandler = extensionCommandHandler;
    }

    public ExtensionLoader getExtensionLoader() {
        return extensionLoader;
    }

    public static void main(String[] args) {
        Main main = new Main();

        // Create ShellRunner first
        ShellRunner shell = new ShellRunner(main);
        main.setShellRunner(shell);

        // Create ExtensionCommandHandler
        ExtensionCommandHandler extensionHandler = new ExtensionCommandHandler();
        main.setExtensionCommandHandler(extensionHandler);

        // Set environment for extensions
        extensionHandler.setEnvironment(shell.getEnv());

        // Create CommandHandler
        CommandHandler commandHandler = new CommandHandler(main);
        main.setCommandHandler(commandHandler);

        // Set CommandHandler in ShellRunner
        shell.setCommandHandler(commandHandler);

        // Initialize and load extensions
        ExtensionLoader extensionLoader = new ExtensionLoader(extensionHandler);
        main.extensionLoader = extensionLoader;

        System.out.println("JSH [v" + main.version + "]");
        System.out.println("(c) qiu2014. Licensed under GNU GENERAL PUBLIC LICENSE");
        System.out.println("Loading extensions from extensions directory...");

        // Load all extensions
        extensionLoader.loadExtensions();

        // Start the shell
        shell.run();

        // Clean up extensions on exit
        extensionLoader.unloadAll();
    }
}