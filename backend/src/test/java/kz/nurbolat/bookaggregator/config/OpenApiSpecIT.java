package kz.nurbolat.bookaggregator.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks that every operation is described, has a success response and error responses use ErrorResponse and that the committed spec {@code docs/api/openapi.json}
 * matches the one the application serves. After changing controller annotations or DTOs, re-export with
 * {@code ./mvnw -B verify -Dit.test=OpenApiSpecIT -Dopenapi.update=true}.
 */
class OpenApiSpecIT extends AbstractIntegrationTest {

    private static final Path SPEC_FILE = Path.of("..", "docs", "api", "openapi.json");
    private static final Set<String> METHODS = Set.of("get", "post", "put", "patch", "delete");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void everyOperationHasSummaryAndTag() throws Exception {
        List<String> problems = new ArrayList<>();
        for (String url : List.of("/v3/api-docs", "/v3/api-docs/public", "/v3/api-docs/user", "/v3/api-docs/admin")) {
            checkOperations(url, fetch(url), problems);
        }

        assertThat(problems).isEmpty();
    }

    private static void checkOperations(String url, JsonNode spec, List<String> problems) {
        spec.get("paths").fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(op -> {
            if (!METHODS.contains(op.getKey())) {
                return;
            }
            String id = url + " " + op.getKey().toUpperCase() + " " + path.getKey();
            if (op.getValue().path("summary").asText().isBlank()) {
                problems.add(id + ": no summary");
            }
            if (op.getValue().path("tags").isEmpty()) {
                problems.add(id + ": no tag");
            }
            checkResponses(id, op.getValue().path("responses"), problems);
        }));
    }

    private static void checkResponses(String id, JsonNode responses, List<String> problems) {
        boolean hasSuccess = false;
        for (var it = responses.fields(); it.hasNext(); ) {
            var response = it.next();
            String code = response.getKey();
            if (code.startsWith("2")) {
                hasSuccess = true;
                if (response.getValue().path("description").asText().isBlank()) {
                    problems.add(id + ": " + code + " has no description");
                }
            } else if (code.startsWith("4") || code.startsWith("5")) {
                response.getValue().path("content").elements().forEachRemaining(media -> {
                    String ref = media.path("schema").path("$ref").asText();
                    if (!ref.equals("#/components/schemas/ErrorResponse")) {
                        problems.add(id + ": " + code + " references " + ref + " instead of ErrorResponse");
                    }
                });
            }
        }
        if (!hasSuccess) {
            problems.add(id + ": no 2xx response");
        }
    }

    @Test
    void groupsSplitPathsByAudience() throws Exception {
        Set<String> adminPaths = paths(fetch("/v3/api-docs/admin"));
        Set<String> publicPaths = paths(fetch("/v3/api-docs/public"));
        Set<String> userPaths = paths(fetch("/v3/api-docs/user"));

        assertThat(adminPaths).isNotEmpty().allMatch(p -> p.startsWith("/api/v1/admin/"));
        assertThat(publicPaths).isNotEmpty()
                .noneMatch(p -> p.startsWith("/api/v1/admin/"))
                .doesNotContain("/api/v1/me");
        assertThat(userPaths).contains("/api/v1/me");
    }

    @Test
    void committedSpecMatchesServedSpec() throws Exception {
        JsonNode actual = fetch("/v3/api-docs");

        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(SPEC_FILE.getParent());
            Files.writeString(SPEC_FILE, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual) + "\n",
                    StandardCharsets.UTF_8);
            return;
        }

        JsonNode expected = readCommitted();
        assertThat(actual)
                .as("docs/api/openapi.json is stale; re-export with "
                        + "./mvnw -B verify -Dit.test=OpenApiSpecIT -Dopenapi.update=true")
                .isEqualTo(expected);
    }

    private JsonNode readCommitted() throws IOException {
        assertThat(SPEC_FILE).as("docs/api/openapi.json is missing; export it with "
                + "./mvnw -B verify -Dit.test=OpenApiSpecIT -Dopenapi.update=true").exists();
        return objectMapper.readTree(Files.readString(SPEC_FILE, StandardCharsets.UTF_8));
    }

    private JsonNode fetch(String url) throws Exception {
        String body = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body);
    }

    private static Set<String> paths(JsonNode spec) {
        Set<String> result = new java.util.TreeSet<>();
        spec.get("paths").fieldNames().forEachRemaining(result::add);
        return result;
    }
}
