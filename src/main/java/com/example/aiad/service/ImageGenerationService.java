package com.example.aiad.service;

import com.example.aiad.model.AdCreative;
import com.example.aiad.repository.AdCreativeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class ImageGenerationService {

    private final AdCreativeRepository repository;
    private final WebClient geminiClient;
    private final String geminiApiKey;

    public ImageGenerationService(
            AdCreativeRepository repository,
            @Value("${gemini.api.key:AIzaSyAC2rvV50peaAi-CRCkEnpYATBwUb_wymA}") String geminiApiKey
    ) {
        this.repository = repository;
        this.geminiApiKey = geminiApiKey.trim(); // Remove any whitespace

        if (this.geminiApiKey.isEmpty() || this.geminiApiKey.equals("AIzaSyAC2rvV50peaAi-CRCkEnpYATBwUb_wymA")) {
            System.out.println("WARNING: Using default Gemini API key. Please set GEMINI_API_KEY environment variable.");
        }

        // Configure WebClient with larger buffer size to handle large image responses
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB buffer
                .build();

        this.geminiClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(exchangeStrategies)
                .build();
    }

    /**
     * Calls Google Gemini API directly to generate image and saves the result as a data URL.
     */
    public String generateAndSaveImage(Long adId, String prompt) {
        if (adId == null) {
            throw new IllegalArgumentException("adId is required");
        }
        Optional<AdCreative> optional = repository.findById(adId);
        AdCreative ad = optional.orElseThrow(() -> new IllegalArgumentException("Ad not found: " + adId));

        String safePrompt = prompt == null || prompt.trim().isEmpty()
                ? "Generate a high-quality advertising image: " + ad.getHeadline() + ". " + ad.getPrompt()
                : "Generate a high-quality advertising image: " + prompt.trim();

        try {
            // Build Gemini API request
            Map<String, Object> contents = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", safePrompt);
            parts.add(textPart);
            contents.put("parts", parts);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(contents));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("response_modalities", List.of("TEXT", "IMAGE"));
            requestBody.put("generationConfig", generationConfig);

            // Call Gemini API using query parameter (as per Gemini API docs)
            Map<String, Object> response = geminiClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/gemini-2.0-flash-exp:generateContent")
                            .queryParam("key", geminiApiKey.trim()) // Trim any whitespace
                            .build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .onErrorResume(e -> Mono.error(new RuntimeException("Failed to call Gemini API", e)))
                    .block();

            if (response == null) {
                throw new RuntimeException("Gemini API returned empty response");
            }

            // Extract image from response
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException("No candidates in Gemini response");
            }

            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            List<Map<String, Object>> responseParts = (List<Map<String, Object>>) content.get("parts");

            String base64Image = null;
            for (Map<String, Object> part : responseParts) {
                if (part.containsKey("inlineData")) {
                    Map<String, Object> inlineData = (Map<String, Object>) part.get("inlineData");
                    base64Image = (String) inlineData.get("data");
                    break;
                }
            }

            if (base64Image == null) {
                throw new RuntimeException("No image found in Gemini response");
            }

            String imageUrl = "data:image/png;base64," + base64Image;

            ad.setImageUrl(imageUrl);
            repository.save(ad);

            return imageUrl;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to generate image with Gemini API: " + e.getMessage());
            e.printStackTrace();

            // Fallback: Generate a placeholder image using via.placeholder.com
            try {
                String encodedPrompt = java.net.URLEncoder.encode(safePrompt, "UTF-8");
                String fallbackImageUrl = "https://via.placeholder.com/600x400.png?text=" + encodedPrompt;
                ad.setImageUrl(fallbackImageUrl);
                repository.save(ad);
                System.out.println("DEBUG: Using fallback placeholder image: " + fallbackImageUrl);
                return fallbackImageUrl;
            } catch (Exception fallbackError) {
                throw new RuntimeException("Failed to generate image with both Gemini API and fallback: " + e.getMessage(), e);
            }
        }
    }
}