package uk.anbu.poc.filewatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.io.*;

@RequiredArgsConstructor
@Slf4j
public class ConfigManager {
    private static String configFilePath;

    static {
        configFilePath = System.getProperty("user.home") + File.separator + "watcherapp.properties";
    }

    public void saveConfig(Config config) {
        Properties configPropertiesFile = new Properties();
        configPropertiesFile.setProperty("directory", config.watchedDirectory);
        configPropertiesFile.setProperty("command", config.command);
        configPropertiesFile.setProperty("globPatterns", String.join(",", config.globPatterns));
        configPropertiesFile.setProperty("monitorSubdirectories", String.valueOf(config.monitorSubdirectories));

        try (OutputStream output = new FileOutputStream(configFilePath)) {
            configPropertiesFile.store(output, "Directory Watcher Configuration");
        } catch (IOException ioException) {
            log.warn("Error saving config", ioException);
        }
    }

    public Config loadConfig() {
        Properties configPropertiesFile = new Properties();
        var defaultConfig = new Config(System.getProperty("user.home"), "echo 'hello'", List.of("*"), false);
        if (!Path.of(configFilePath).toFile().exists()) {
            saveConfig(defaultConfig);
        }

        try (InputStream inputStream = new FileInputStream(configFilePath)) {
            configPropertiesFile.load(inputStream);

            var watchedDirectory = configPropertiesFile.getProperty("directory", System.getProperty("user.home"));
            var command = configPropertiesFile.getProperty("command", "");
            var globPatterns = Arrays.asList(configPropertiesFile.getProperty("globPatterns", "*").split(","));
            var monitorSubdirectories = Boolean.parseBoolean(configPropertiesFile.getProperty("monitorSubdirectories", "false"));
            return new Config(watchedDirectory, command, globPatterns, monitorSubdirectories);
        } catch (IOException io) {
            log.error("Error reading config file", io);
            return defaultConfig;
        }
    }

    public record Config(String watchedDirectory, String command, List<String> globPatterns, boolean monitorSubdirectories) {
    }
}
