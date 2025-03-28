package team.brown.sharding.master.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Запрос для обновления количества шардов.
 */
@Schema(description = "Новая конфигурация количества шардов в кластере")
public record NodeRequest(
        @Schema(description = "Сетевой адрес узла", example = "http://192.168.1.10:8000", required = true) String address
) {
}
