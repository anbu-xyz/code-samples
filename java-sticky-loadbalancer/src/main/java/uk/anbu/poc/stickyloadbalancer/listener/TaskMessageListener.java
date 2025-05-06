package uk.anbu.poc.stickyloadbalancer.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.anbu.poc.stickyloadbalancer.model.TaskMessage;
import uk.anbu.poc.stickyloadbalancer.service.TaskInboxReader;
import uk.anbu.poc.stickyloadbalancer.service.TaskInboxWriter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
@RequiredArgsConstructor
public class TaskMessageListener {

    private final TaskInboxWriter taskInboxWriter;
    private final TaskInboxReader taskInboxReader;
    private final JmsTemplate jmsTemplate;
    private final LockRegistry lockRegistry;

    private static final String LOCK_KEY = "task-queue-poll-lock";
    private static final long LOCK_TIMEOUT = 30; // seconds

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedDelay = 1000) // Polls every 1 second
    public void pollMessages() {
        Lock lock = lockRegistry.obtain(LOCK_KEY);

        try {
            if (!lock.tryLock(LOCK_TIMEOUT, TimeUnit.SECONDS)) {
                log.debug("Could not acquire lock for polling messages");
                return;
            }

            log.debug("Acquired lock for polling messages");

            while (true) {
                Message message = jmsTemplate.receive("task-queue");
                if (message == null) {
                    break; // No more messages in queue
                }

                processMessage(message);
            }

        } catch (Exception e) {
            log.error("Error during message polling", e);
        } finally {
            try {
                lock.unlock();
                log.debug("Released lock for polling messages");
            } catch (Exception e) {
                log.warn("Error releasing lock", e);
            }
        }
    }

    private void processMessage(Message message) {
        try {
            String messageId = message.getJMSMessageID();
            TextMessage textMessage = ((jakarta.jms.TextMessage) message);
            // parse text message as JSON
            log.info("Processing message: {} with payload: {}", messageId, textMessage.getText());
            // convert JSON to TaskMessage using Jackson
            var payload = objectMapper.readValue(textMessage.getText(), TaskMessage.class);

            log.info("Processing message: {} with payload: {}", messageId, payload);

            taskInboxWriter.writeMessage(messageId, payload);
            taskInboxReader.checkForNewTasks();
        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }
}