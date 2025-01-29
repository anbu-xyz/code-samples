package uk.anbu.poc.stickyloadbalancer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.anbu.poc.stickyloadbalancer.entity.TaskInbox;
import uk.anbu.poc.stickyloadbalancer.repository.TaskInboxRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProcessor {

    private final TaskInboxRepository taskInboxRepository;
    private final LockRegistry lockRegistry;
    @Transactional
    public void processTasks(int partitionKey) {

        Thread.ofVirtual().name("P-"+ partitionKey).start(() -> {
            var lock = lockRegistry.obtain("P-"+ partitionKey);
            if (lock.tryLock()) {
                try {
                    var tasks = taskInboxRepository.findByPartitionKeyOrderById(partitionKey);
                    for (TaskInbox task : tasks) {
                        log.info("Processing task: MessageId={}, PartitionKey={}, WorkNumber={}",
                            task.getMessageId(),
                            task.getPartitionKey(),
                            task.getWorkNumber());

                        // TODO: Process task
                        taskInboxRepository.delete(task);
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                log.info("Could not obtain lock for partition key: {}", partitionKey);
            }
        });
    }

}
