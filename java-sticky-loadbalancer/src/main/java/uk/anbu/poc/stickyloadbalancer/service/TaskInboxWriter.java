package uk.anbu.poc.stickyloadbalancer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.anbu.poc.stickyloadbalancer.entity.TaskInbox;
import uk.anbu.poc.stickyloadbalancer.model.TaskMessage;
import uk.anbu.poc.stickyloadbalancer.repository.TaskInboxRepository;

@Service
@RequiredArgsConstructor
public class TaskInboxWriter {

    private final TaskInboxRepository messageInboxRepository;

    @Transactional
    public void writeMessage(String messageId, TaskMessage message) {
        // Check if message was already processed
        if (messageInboxRepository.findByMessageId(messageId).isPresent()) {
            return; // Message already processed
        }

        // Create inbox entry
        TaskInbox inbox = new TaskInbox();
        inbox.setMessageId(messageId);
        inbox.setPartitionKey(message.getPartitionKey());
        inbox.setWorkNumber(message.getWorkNumber());

        // Save to inbox
        messageInboxRepository.save(inbox);
    }
}
