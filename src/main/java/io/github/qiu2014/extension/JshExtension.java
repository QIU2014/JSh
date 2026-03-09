package io.github.qiu2014.extension;

import java.util.Map;

/**
 * Interface that all JSH extensions must implement
 */
public interface JshExtension {
    /**
     * @return Name of the extension
     */
    String getName();

    /**
     * @return Version of the extension
     */
    String getVersion();

    /**
     * @return Description of what the extension does
     */
    String getDescription();

    /**
     * Get all commands provided by this extension
     * @return Map of command name to command executor
     */
    Map<String, CommandExecutor> getCommands();

    /**
     * Initialize the extension
     * @param environment Shell environment variables
     */
    void init(Map<String, String> environment);

    /**
     * Clean up when extension is unloaded
     */
    void shutdown();
}