package com.yassinecharit.budget.categorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Categorizes a transaction with the Mistral chat completion API.
 * Disabled (returns empty) when no API key is configured, so the app also works fully offline.
 */
@Component
public class MistralCategorizer implements LlmCategorizer {

    private static final Logger log = LoggerFactory.getLogger(MistralCategorizer.class);

    private final RestClient client;
    private final String apiKey;
    private final String model;

    public MistralCategorizer(RestClient.Builder builder,
                              @Value("${app.mistral.api-key:}") String apiKey,
                              @Value("${app.mistral.model:mistral-small-latest}") String model,
                              @Value("${app.mistral.base-url:https://api.mistral.ai}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public Optional<Category> categorize(String label) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        String categories = Arrays.stream(Category.values()).map(Enum::name).collect(Collectors.joining(", "));
        var body = Map.of(
                "model", model,
                "temperature", 0,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You classify French bank transactions. Answer with exactly one word among: "
                                        + categories + "."),
                        Map.of("role", "user", "content", label)));
        try {
            ChatResponse response = client.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return Optional.empty();
            }
            return parse(response.choices().get(0).message().content());
        } catch (RestClientException e) {
            log.warn("Mistral categorization failed, falling back to default category: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Extracts a known category from the model answer (tolerant to punctuation or extra words). */
    static Optional<Category> parse(String answer) {
        if (answer == null) {
            return Optional.empty();
        }
        String upper = answer.toUpperCase();
        return Arrays.stream(Category.values()).filter(c -> upper.contains(c.name())).findFirst();
    }

    record ChatResponse(List<Choice> choices) {
    }

    record Choice(Message message) {
    }

    record Message(String role, String content) {
    }
}
