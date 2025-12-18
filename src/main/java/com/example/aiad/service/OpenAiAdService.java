package com.example.aiad.service;

import com.example.aiad.model.AdCreative;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpenAiAdService {

    private final WebClient webClient;
    private final String modelName;

    public OpenAiAdService(
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}") String apiUrl,
            @Value("${model.name:openai/gpt-oss-120b}") String modelName
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY environment variable must be set");
        }
        this.modelName = modelName;
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public AdCreative generateAd(String prompt) {
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are an ad copywriter. Given a campaign description, output exactly one JSON object with three fields: headline, cta, survey_question. Do not include any other text.");

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", "Campaign description: " + prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", new Object[]{systemMessage, userMessage});
        requestBody.put("temperature", 0.7);

        Map<String, Object> response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to call OpenAI API", e)))
                .block();

        if (response == null) {
            throw new RuntimeException("Empty response from OpenAI API");
        }

        Map<String, Object> choice0 = ((java.util.List<Map<String, Object>>) response.get("choices")).get(0);
        Map<String, Object> message = (Map<String, Object>) choice0.get("message");
        String content = (String) message.get("content");

        Map<String, Object> parsed = safeParseJsonContent(content);

        String headline = stringField(parsed.get("headline"));
        String cta = stringField(parsed.get("cta"));
        String surveyQuestion = stringField(parsed.get("survey_question"));

        if (headline.isBlank() || cta.isBlank() || surveyQuestion.isBlank()) {
            throw new RuntimeException("OpenAI response missing required fields");
        }

        AdCreative creative = new AdCreative();
        creative.setPrompt(prompt);
        creative.setHeadline(headline);
        creative.setCta(cta);
        creative.setSurveyQuestion(surveyQuestion);
        return creative;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeParseJsonContent(String content) {
        String trimmed = content.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new RuntimeException("OpenAI content is not valid JSON object: " + content);
        }
        String json = trimmed.substring(start, end + 1);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI JSON content", e);
        }
    }

    private String stringField(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}


