package uk.anbu.poc.stickyloadbalancer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.anbu.poc.stickyloadbalancer.entity.TaskInbox;

import java.util.List;
import java.util.Optional;

public interface TaskInboxRepository extends JpaRepository<TaskInbox, Long> {
    Optional<TaskInbox> findByMessageId(String messageId);

    @Query("SELECT DISTINCT partitionKey FROM TaskInbox")
    List<Integer> findDistinctPartitionKeys();

    List<TaskInbox> findByPartitionKeyOrderById(int partitionKey);

}
