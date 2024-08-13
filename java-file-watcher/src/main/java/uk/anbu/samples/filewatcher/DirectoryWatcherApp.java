package uk.anbu.samples.filewatcher;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
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
    private DirectoryWatcherUI watcherUI;
    private WatchService watchService;
    private ScheduledExecutorService executor;
    private Future<?> watchTask;
    private List<PathMatcher> pathMatchers;
    private Map<WatchKey, Path> watchKeyToPath = new HashMap<>();


    @Inject
    public DirectoryWatcherApp(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void run() {
        watcherUI = new DirectoryWatcherUI(this::startWatching, this::stopWatching, this.configManager);
        watcherUI.show();

        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (Exception ex) {
            log.error("Error creating watch service", ex);
        }

        executor = Executors.newSingleThreadScheduledExecutor();
    }

    private void startWatching() {
        try {
            String directoryPath = watcherUI.config().watchedDirectory();
            List<String> globPatterns = watcherUI.config().globPatterns();
            boolean monitorSubdirectories = watcherUI.config().monitorSubdirectories();
            boolean consolidateChanges = watcherUI.config().consolidateChanges();

            Path path = Paths.get(directoryPath);

            log.info("Going to start watching {} with patterns {}", path, globPatterns);
            registerDirectory(path);

            if (monitorSubdirectories) {
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
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
                    processWatchEvents(monitorSubdirectories, consolidateChanges);
                } catch (Exception ex) {
                    log.error("Error processing watch events: ", ex);
                }
            }, 0, 5, TimeUnit.SECONDS);

        } catch (Exception ex) {
            log.error("Error watching", ex);
        }
    }

    private void processWatchEvents(boolean monitorSubdirectories, boolean consolidateChanges) {
        WatchKey key;
        while ((key = watchService.poll()) != null) {
            Path dir = watchKeyToPath.get(key);
            if (dir == null) {
                log.debug("Watch key {} is no longer valid", key);
                continue;
            }
            var polledEvents = key.pollEvents();
            if (polledEvents == null || polledEvents.isEmpty()) {
                continue;
            }
            var firstEvent = polledEvents.get(0);
            var restOfEvents = polledEvents.stream().skip(1).toList();
            Path firstEventPath = dir.resolve((Path) firstEvent.context());
            if (pathMatchers.stream().anyMatch(matcher -> matcher.matches(firstEventPath.getFileName()))) {
                if (consolidateChanges) {
                    triggerCommand(firstEvent, firstEventPath);
                    restOfEvents.forEach(fi ->
                            log.info("Event of kind {} received {} times for file {}. Ignored due to change consolidation.",
                                    fi.kind(), fi.count(), dir.resolve((Path) fi.context()))
                    );
                } else {
                    polledEvents.forEach(fi -> triggerCommand(fi, dir.resolve((Path) fi.context())));
                }
            }

            // If a new directory is created and we're monitoring subdirectories, register it
            if (monitorSubdirectories && Files.isDirectory(firstEventPath)) {
                registerDirectory(firstEventPath);
            }
            if (!key.reset()) {
                watchKeyToPath.remove(key);
                log.info("Watch key {} is no longer valid", key);
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

    private void triggerCommand(WatchEvent<?> event, Path eventPath) {
        log.info("Event of kind {} received {} times for file {}. Triggering command.", event.kind(), event.count(), eventPath);
        try {
            String command = watcherUI.config().command();
            String workingDirectory = watcherUI.config().workingDirectory();

            command = command.replace("${file}", eventPath.getFileName().toString())
                    .replace("${file_dir}", eventPath.getParent().toString())
                    .replace("${file_with_dir}", eventPath.toString());

            log.info("Running command: " + command);
            log.info("Working directory: {}", workingDirectory);

            List<String> commandList = new ArrayList<>();
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

            commandList.add(command);

            ProcessBuilder processBuilder = new ProcessBuilder(commandList);

            if (workingDirectory != null && !workingDirectory.isEmpty()) {
                processBuilder.directory(new File(workingDirectory));
            }
            Process process = processBuilder.start();

            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), System.err::println);

            new Thread(outputGobbler).start();
            new Thread(errorGobbler).start();

            int exitCode = process.waitFor();
            log.info("Process exited with code: " + exitCode);

        } catch (Exception ex) {
            log.error("Error triggering command", ex);
        }
    }
}
