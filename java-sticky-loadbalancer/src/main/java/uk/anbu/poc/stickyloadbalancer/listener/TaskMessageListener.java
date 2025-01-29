package uk.anbu.poc.stickyloadbalancer.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import uk.anbu.poc.stickyloadbalancer.model.TaskMessage;
import uk.anbu.poc.stickyloadbalancer.service.TaskInboxReader;
import uk.anbu.poc.stickyloadbalancer.service.TaskInboxWriter;

@Component
@Slf4j
@RequiredArgsConstructor
public class TaskMessageListener {

    private final TaskInboxWriter taskInboxWriter;
    private final TaskInboxReader taskInboxReader;

    @JmsListener(destination = "task-queue")
    public void onMessage(Message<TaskMessage> message) {
        MessageHeaders headers = message.getHeaders();
        String messageId = headers.getId().toString();
        TaskMessage payload = message.getPayload();

        log.info("Received message: {} with payload: {}", messageId, payload);

        try {
            taskInboxWriter.writeMessage(messageId, payload);
            taskInboxReader.checkForNewTasks();
        } catch (Exception e) {
            log.error("Error inboxing message: " + messageId, e);
            throw e; // Rethrowing to trigger JMS retry if configured
        }
    }
}
