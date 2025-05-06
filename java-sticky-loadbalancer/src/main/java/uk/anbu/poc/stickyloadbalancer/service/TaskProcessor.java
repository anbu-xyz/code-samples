package uk.anbu.poc.stickyloadbalancer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.anbu.poc.stickyloadbalancer.entity.TaskInbox;
import uk.anbu.poc.stickyloadbalancer.repository.TaskInboxRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProcessor {

    private final TaskInboxRepository taskInboxRepository;
    private final AtomicInteger threadCount = new AtomicInteger();
    private final LockRegistry lockRegistry;
    @Transactional
    public void processTasks(int partitionKey) {

        Thread.ofVirtual().name("P-"+ partitionKey + "-" + threadCount.incrementAndGet()).start(() -> {
            var lock = lockRegistry.obtain("P-"+ partitionKey);
            if (lock.tryLock()) {
                try {
                    List<TaskInbox> tasks = taskInboxRepository.findByPartitionKeyOrderById(partitionKey);
                    while(!tasks.isEmpty()) {
                        for (TaskInbox task : tasks) {
                            log.info("Processing task: MessageId={}, PartitionKey={}, WorkNumber={}",
                                task.getMessageId(),
                                task.getPartitionKey(),
                                task.getWorkNumber());

                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                // ignore.
                            }
                            log.info("Done task: MessageId={}, PartitionKey={}, WorkNumber={}",
                                task.getMessageId(),
                                task.getPartitionKey(),
                                task.getWorkNumber());
                            taskInboxRepository.delete(task);
                        }
                        tasks = taskInboxRepository.findByPartitionKeyOrderById(partitionKey);
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
