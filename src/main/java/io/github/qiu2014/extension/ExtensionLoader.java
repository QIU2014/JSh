package io.github.qiu2014.extension;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ExtensionLoader {
    private static final String EXTENSIONS_DIR = "extensions";
    private static final String EXTENSION_CLASS_ATTRIBUTE = "Jsh-Extension-Class";

    private final ExtensionCommandHandler handler;
    private final List<URLClassLoader> classLoaders = new ArrayList<>();

    public ExtensionLoader(ExtensionCommandHandler handler) {
        this.handler = handler;
    }

    /**
     * Load all extensions from the extensions directory
     */
    public void loadExtensions() {
        Path extensionsPath = Paths.get(EXTENSIONS_DIR);

        // Create extensions directory if it doesn't exist
        try {
            if (!Files.exists(extensionsPath)) {
                Files.createDirectories(extensionsPath);
                System.out.println("Created extensions directory: " + extensionsPath.toAbsolutePath());
                return;
            }
        } catch (Exception e) {
            System.err.println("Failed to create extensions directory: " + e.getMessage());
            return;
        }

        // Find all JAR files in the extensions directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsPath, "*.jar")) {
            for (Path jarPath : stream) {
                loadExtensionFromJar(jarPath);
            }
        } catch (Exception e) {
            System.err.println("Error scanning extensions directory: " + e.getMessage());
        }
    }

    /**
     * Load a specific extension JAR file
     */
    public boolean loadExtensionFromJar(Path jarPath) {
        try {
            System.out.println("Loading extension: " + jarPath.getFileName());

            // Read the manifest to find the main extension class
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                Manifest manifest = jarFile.getManifest();
                if (manifest == null) {
                    System.err.println("  No manifest found in " + jarPath.getFileName());
                    return false;
                }

                String extensionClass = manifest.getMainAttributes().getValue(EXTENSION_CLASS_ATTRIBUTE);
                if (extensionClass == null || extensionClass.trim().isEmpty()) {
                    System.err.println("  Missing " + EXTENSION_CLASS_ATTRIBUTE + " in manifest");
                    return false;
                }

                // Create class loader for this JAR
                URLClassLoader classLoader = new URLClassLoader(
                        new URL[]{jarPath.toUri().toURL()},
                        Thread.currentThread().getContextClassLoader()
                );
                classLoaders.add(classLoader);

                // Load the extension class
                Class<?> clazz = Class.forName(extensionClass, true, classLoader);

                // Verify it implements JshExtension
                if (!JshExtension.class.isAssignableFrom(clazz)) {
                    System.err.println("  Class " + extensionClass + " does not implement JshExtension");
                    return false;
                }

                // Create instance and register
                JshExtension extension = (JshExtension) clazz.getDeclaredConstructor().newInstance();
                handler.registerExtension(extension);

                System.out.println("Successfully loaded: " + extension.getName());
                return true;

            } catch (Exception e) {
                System.err.println("  Failed to load extension: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error loading JAR " + jarPath + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Load an extension from a specific class
     */
    public boolean loadExtensionFromClass(Class<? extends JshExtension> extensionClass) {
        try {
            JshExtension extension = extensionClass.getDeclaredConstructor().newInstance();
            handler.registerExtension(extension);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to load extension class: " + e.getMessage());
            return false;
        }
    }

    /**
     * Unload all extensions
     */
    public void unloadAll() {
        handler.shutdown();

        for (URLClassLoader classLoader : classLoaders) {
            try {
                classLoader.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        classLoaders.clear();
    }
}