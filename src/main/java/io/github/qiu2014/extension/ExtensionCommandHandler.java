package io.github.qiu2014.extension;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExtensionCommandHandler {
    private final Map<String, CommandExecutor> commands = new ConcurrentHashMap<>();
    private final List<JshExtension> loadedExtensions = new ArrayList<>();
    private Map<String, String> environment;

    public ExtensionCommandHandler() {
        this.environment = System.getenv();
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
        // Update all extensions with new environment
        for (JshExtension ext : loadedExtensions) {
            ext.init(environment);
        }
    }

    public void registerExtension(JshExtension extension) {
        if (extension == null) return;

        // Initialize the extension
        extension.init(environment);

        // Register all its commands
        Map<String, CommandExecutor> extCommands = extension.getCommands();
        if (extCommands != null) {
            commands.putAll(extCommands);
        }

        loadedExtensions.add(extension);

        System.out.println("Loaded extension: " + extension.getName() +
                " v" + extension.getVersion());
    }

    public void processCommand(String command) {
        String[] args = command.split("\\s+");
        if (args.length == 0) return;

        String cmdName = args[0];
        CommandExecutor executor = commands.get(cmdName);

        if (executor != null) {
            try {
                boolean handled = executor.execute(args);
                if (!handled) {
                    System.out.println("Command failed to execute: " + cmdName);
                }
            } catch (Exception e) {
                System.err.println("Error executing extension command '" + cmdName + "': " + e.getMessage());
            }
        } else {
            System.out.println("jsh: command not found: " + cmdName);
        }
    }

    public void printExtendedHelp() {
        if (loadedExtensions.isEmpty()) {
            System.out.println("\nNo extensions loaded.");
            return;
        }

        System.out.println("\nLoaded Extensions:");
        System.out.println("===================");

        for (JshExtension ext : loadedExtensions) {
            System.out.println(ext.getName() + " v" + ext.getVersion());
            System.out.println("  " + ext.getDescription());

            Map<String, CommandExecutor> extCommands = ext.getCommands();
            if (extCommands != null && !extCommands.isEmpty()) {
                System.out.println("  Commands:");
                for (Map.Entry<String, CommandExecutor> entry : extCommands.entrySet()) {
                    String help = entry.getValue().getHelp();
                    System.out.println("    " + entry.getKey() + " - " + help);
                }
            }
            System.out.println();
        }
    }

    public List<JshExtension> getLoadedExtensions() {
        return Collections.unmodifiableList(loadedExtensions);
    }

    public void shutdown() {
        for (JshExtension ext : loadedExtensions) {
            try {
                ext.shutdown();
            } catch (Exception e) {
                System.err.println("Error shutting down extension " + ext.getName() + ": " + e.getMessage());
            }
        }
        commands.clear();
        loadedExtensions.clear();
    }
}