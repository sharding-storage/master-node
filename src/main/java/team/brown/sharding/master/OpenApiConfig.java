package team.brown.sharding.master;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация OpenAPI для генерации Swagger документации.
 */
@Configuration
public class OpenApiConfig {
    /**
     * Настраивает OpenAPI.
     * @return объект OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sharding Service API")
                        .version("1.0.0")
                        .description("API для мастер-ноды шардированного хранилища"));
    }
}
