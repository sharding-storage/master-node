package team.brown.sharding.master.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Универсальный ответ с сообщением.
 */
@Schema(description = "Общий ответ сервера")
public record CommonResponse(
        @Schema(description = "Сообщение ответа", example = "done") String answer
) {
}
