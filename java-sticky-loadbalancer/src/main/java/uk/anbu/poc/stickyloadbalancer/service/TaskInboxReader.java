package uk.anbu.poc.stickyloadbalancer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.anbu.poc.stickyloadbalancer.repository.TaskInboxRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskInboxReader {

    private final TaskInboxRepository taskInboxRepository;
    private final TaskProcessor taskProcessor;

    public void checkForNewTasks() {
        var partitioningKeys = taskInboxRepository.findDistinctPartitionKeys();
        for (int partitionKey : partitioningKeys) {
            taskProcessor.processTasks(partitionKey);
        }
    }
}
