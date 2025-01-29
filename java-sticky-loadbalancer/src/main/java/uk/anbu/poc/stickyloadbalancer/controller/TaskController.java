package uk.anbu.poc.stickyloadbalancer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.anbu.poc.stickyloadbalancer.model.TaskMessage;

import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final JmsTemplate jmsTemplate;
    private static final String QUEUE_NAME = "task-queue";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskController.class);

    @Autowired
    public TaskController(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendTask(@RequestBody TaskMessage taskMessage) {
        log.info("Sending task {}", taskMessage);
        jmsTemplate.convertAndSend(QUEUE_NAME, taskMessage);
        return ResponseEntity.ok("Message sent successfully");
    }

    @PostMapping("/dump")
    public ResponseEntity<String> dump() {
        for(int workNumber = 0; workNumber < 100; workNumber++) {
            TaskMessage taskMessage = new TaskMessage();
            taskMessage.setPartitionKey(ThreadLocalRandom.current().nextInt(1, 11));
            taskMessage.setWorkNumber(workNumber);
            jmsTemplate.convertAndSend(QUEUE_NAME, taskMessage);
        }
        return ResponseEntity.ok("Messages dumped successfully");
    }
}
