package uk.anbu.poc.stickyloadbalancer.model;

import lombok.Data;

@Data
public class TaskMessage {
    private Integer partitionKey;
    private Integer workNumber;
}
