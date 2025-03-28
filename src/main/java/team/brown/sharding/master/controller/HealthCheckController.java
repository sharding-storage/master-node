
package team.brown.sharding.master.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Health check
 */
@RestController
@Tag(name = "Health check", description = "Health check")
@RequiredArgsConstructor
public class HealthCheckController {

    @Operation(summary = "Health check", description = "Проверка работоспособности сервиса")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}