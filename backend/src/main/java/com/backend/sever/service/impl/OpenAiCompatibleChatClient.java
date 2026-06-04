package com.backend.sever.service.impl;

import com.backend.sever.config.AiProperties;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.AiChatClient;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
public class OpenAiCompatibleChatClient implements AiChatClient {
    private final AiProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleChatClient(
            AiProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (!properties.enabled()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI assistant is disabled");
        }
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI API key is not configured");
        }
        if (!StringUtils.hasText(properties.baseUrl()) || !StringUtils.hasText(properties.model())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI model configuration is incomplete");
        }

        ChatCompletionResponse response = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build()
                .post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.apiKey())
                .body(new ChatCompletionRequest(
                        properties.model(),
                        properties.temperature(),
                        properties.maxTokens(),
                        List.of(
                                new ChatMessage("system", systemPrompt),
                                new ChatMessage("user", userPrompt)
                        )
                ))
                .retrieve()
                .body(ChatCompletionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI response is empty");
        }
        ChatChoice choice = response.choices().get(0);
        if (choice.message() == null || !StringUtils.hasText(choice.message().content())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI response is empty");
        }
        return choice.message().content();
    }

    @Override
    public String streamChat(String systemPrompt, String userPrompt, Consumer<String> tokenConsumer) {
        validateConfiguration();
        try {
            String requestBody = objectMapper.writeValueAsString(new ChatCompletionStreamRequest(
                    properties.model(),
                    properties.temperature(),
                    properties.maxTokens(),
                    true,
                    List.of(
                            new ChatMessage("system", systemPrompt),
                            new ChatMessage("user", userPrompt)
                    )
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.baseUrl()) + "/chat/completions"))
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<Stream<String>> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI stream request failed");
            }
            StringBuilder answer = new StringBuilder();
            try (Stream<String> lines = response.body()) {
                lines.takeWhile(line -> !line.trim().equals("data: [DONE]")).forEach(line -> {
                    if (!line.startsWith("data:")) {
                        return;
                    }
                    String data = line.substring("data:".length()).trim();
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        JsonNode contentNode = node.path("choices").path(0).path("delta").path("content");
                        if (contentNode.isMissingNode() || contentNode.isNull()) {
                            return;
                        }
                        String token = contentNode.asText();
                        if (!token.isEmpty()) {
                            answer.append(token);
                            tokenConsumer.accept(token);
                        }
                    } catch (Exception exception) {
                        throw new IllegalStateException("Unable to parse AI stream chunk", exception);
                    }
                });
            }
            return answer.toString();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI stream request failed");
        }
    }

    private void validateConfiguration() {
        if (!properties.enabled()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI assistant is disabled");
        }
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI API key is not configured");
        }
        if (!StringUtils.hasText(properties.baseUrl()) || !StringUtils.hasText(properties.model())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI model configuration is incomplete");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record ChatCompletionRequest(
            String model,
            double temperature,
            @JsonProperty("max_tokens") int maxTokens,
            List<ChatMessage> messages
    ) {
    }

    private record ChatCompletionStreamRequest(
            String model,
            double temperature,
            @JsonProperty("max_tokens") int maxTokens,
            boolean stream,
            List<ChatMessage> messages
    ) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatCompletionResponse(List<ChatChoice> choices) {
    }

    private record ChatChoice(ChatMessage message) {
    }
}
