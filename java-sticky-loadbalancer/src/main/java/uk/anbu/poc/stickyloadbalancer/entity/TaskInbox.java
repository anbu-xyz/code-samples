package uk.anbu.poc.stickyloadbalancer.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "task_inbox")
@Data
public class TaskInbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", unique = true, nullable = false)
    private String messageId;

    @Column(name = "partition_key", nullable = false)
    private Integer partitionKey;

    @Column(name = "work_number", nullable = false)
    private Integer workNumber;

    @Column(name = "created_at_utc", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("UTC"));
}
