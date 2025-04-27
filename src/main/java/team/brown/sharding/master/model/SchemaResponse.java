package team.brown.sharding.master.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Ответ, содержащий схему узлов (адреса серверов).
 */
@Schema(description = "Схема узлов в порядке хеширования")
public record SchemaResponse(
        @Schema(description = "Список адресов узлов", example = "[\"NODE1 ADDRESS\", \"NODE2 ADDRESS\"]") List<String> nodes,
        Integer virtualNodes, Integer version
) {
}
