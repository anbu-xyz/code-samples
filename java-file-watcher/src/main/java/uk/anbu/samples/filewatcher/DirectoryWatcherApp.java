package uk.anbu.samples.filewatcher;

import com.google.inject.Guice;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class DirectoryWatcherApp {
    private final ConfigManager configManager;
    private WatcherFrame watcherFrame;
    private WatchService watchService;
    private ScheduledExecutorService executor;
    private Future<?> watchTask;
    private List<PathMatcher> pathMatchers;
    private Map<WatchKey, Path> watchKeyToPath = new HashMap<>();

    public static void main(String[] args) {
        var module = Guice.createInjector(new AppModule());
        var app = module.getInstance(DirectoryWatcherApp.class);

        SwingUtilities.invokeLater(app::run);
    }

    @Inject
    public DirectoryWatcherApp(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void run() {
        watcherFrame = new WatcherFrame(this::startWatching, this::stopWatching, this.configManager);
        watcherFrame.show();

        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (Exception ex) {
            log.error("Error creating watch service", ex);
        }

        executor = Executors.newSingleThreadScheduledExecutor();
    }

    private void startWatching() {
        try {
            String directoryPath = watcherFrame.getDirectory();
            List<String> globPatterns = watcherFrame.getGlobPatterns();
            boolean monitorSubdirectories = watcherFrame.isMonitorSubdirectories();

            Path path = Paths.get(directoryPath);

            log.info("Going to start watching {} with patterns {}", path, globPatterns);
            registerDirectory(path);

            if (monitorSubdirectories) {
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        registerDirectory(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            pathMatchers = globPatterns.stream()
                .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern))
                .collect(Collectors.toList());

            watchTask = executor.scheduleWithFixedDelay(() -> {
                try {
                    processWatchEvents(monitorSubdirectories);
                } catch (Exception ex) {
                    log.error("Error processing watch events: ", ex);
                }
            }, 0, 5, TimeUnit.SECONDS);

        } catch (Exception ex) {
            log.error("Error watching", ex);
        }
    }

    private void processWatchEvents(boolean monitorSubdirectories) {
        WatchKey key;
        while ((key = watchService.poll()) != null) {
            Path dir = watchKeyToPath.get(key);
            if (dir != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path eventPath = dir.resolve((Path) event.context());
                    if (pathMatchers.stream().anyMatch(matcher -> matcher.matches(eventPath.getFileName()))) {
                        log.info("Event of kind {} received {} times for file {}", event.kind(), event.count(), eventPath);
                        triggerCommand(eventPath);
                    }

                    // If a new directory is created and we're monitoring subdirectories, register it
                    if (monitorSubdirectories && Files.isDirectory(eventPath)) {
                        registerDirectory(eventPath);
                    }
                }
                if (!key.reset()) {
                    watchKeyToPath.remove(key);
                    log.info("Watch key {} is no longer valid", key);
                }
            }
        }
    }

    private void registerDirectory(Path path) {
        try {
            log.info("Registering directory {}", path);
            WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
            watchKeyToPath.put(watchKey, path);
            log.info("Registered directory {} with watch key {}", path, watchKey);
        } catch (Exception ex) {
            log.error("Error registering directory", ex);
        }
    }

    private void stopWatching() {
        if (watchTask != null && !watchTask.isCancelled()) {
            watchTask.cancel(true);
        }
        watchKeyToPath.clear();
    }

    private void triggerCommand(Path eventPath) {
        try {
            String command = watcherFrame.getCommand();

            // Replace placeholders
            command = command.replace("${file}", eventPath.getFileName().toString())
                    .replace("${file_dir}", eventPath.getParent().toString())
                    .replace("${file_with_dir}", eventPath.toString());

            log.info("Running command: " + command);

            List<String> commandList = new ArrayList<>();

            // Check the operating system
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows
                commandList.add("cmd");
                commandList.add("/c");
            } else {
                // Linux, macOS, and other Unix-like systems
                commandList.add("bash");
                commandList.add("-c");
            }

            // Add the actual command
            commandList.add(command);

            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            Process process = processBuilder.start();

            // Set up StreamGobblers for stdout and stderr
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), System.err::println);

            // Start the StreamGobblers
            new Thread(outputGobbler).start();
            new Thread(errorGobbler).start();

            // Wait for the process to complete
            int exitCode = process.waitFor();
            log.info("Process exited with code: " + exitCode);

        } catch (Exception ex) {
            log.error("Error triggering command", ex);
        }
    }
}