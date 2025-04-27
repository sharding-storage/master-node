package team.brown.sharding.master.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MigrationRequest {
    private String targetAddress;
    private Long startHash;
    private Long endHash;
}