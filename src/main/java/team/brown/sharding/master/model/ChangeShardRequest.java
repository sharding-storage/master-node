package team.brown.sharding.master.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Запрос для обновления количества шардов.
 */
@Schema(description = "Новая конфигурация количества шардов в кластере")
public record ChangeShardRequest(
        @Schema(description = "Новое количество шардов", example = "4", required = true) Integer shardCount
) {
}
