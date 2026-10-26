package kz.nurbolat.bookaggregator.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import kz.nurbolat.bookaggregator.common.error.ErrorResponse;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("KitapRadar API")
                        .description("Book price aggregator for stores in Kazakhstan")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/v1/auth/**", "/api/v1/search/**", "/api/v1/books/**", "/api/v1/editions/**",
                        "/api/v1/home/**", "/api/v1/stores/**", "/api/v1/beacons/unsubscribe")
                .build();
    }

    @Bean
    GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .pathsToMatch("/api/v1/me/**", "/api/v1/favorites/**", "/api/v1/beacons/**")
                .pathsToExclude("/api/v1/beacons/unsubscribe")
                .build();
    }

    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }

    /**
     * Explicit {@code @ApiResponse} annotations make springdoc drop the implicit 200 and attach the return type to
     * every declared code. This moves the success body to a 200 where no 2xx is declared and points every 4xx/5xx
     * at {@link ErrorResponse}.
     */
    @Bean
    GlobalOpenApiCustomizer responseSchemas() {
        return openApi -> {
            Components components = openApi.getComponents();
            ModelConverters.getInstance().readAll(ErrorResponse.class).forEach(components::addSchemas);
            Schema<?> errorRef = new Schema<>().$ref("#/components/schemas/ErrorResponse");
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(item -> item.readOperations().forEach(op -> fixResponses(op, errorRef)));
        };
    }

    private static void fixResponses(Operation operation, Schema<?> errorRef) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            return;
        }
        boolean hasSuccess = responses.keySet().stream().anyMatch(code -> code.startsWith("2"));
        Content successContent = null;
        for (var entry : responses.entrySet()) {
            String code = entry.getKey();
            if (code.startsWith("4") || code.startsWith("5")) {
                if (successContent == null) {
                    successContent = entry.getValue().getContent();
                }
                entry.getValue().setContent(new Content().addMediaType("application/json", new MediaType().schema(errorRef)));
            }
        }
        if (!hasSuccess) {
            ApiResponse ok = new ApiResponse().description("Успешный ответ");
            if (successContent != null) {
                ok.setContent(successContent);
            }
            ApiResponses reordered = new ApiResponses();
            reordered.addApiResponse("200", ok);
            responses.forEach(reordered::addApiResponse);
            operation.setResponses(reordered);
        }
    }
}
