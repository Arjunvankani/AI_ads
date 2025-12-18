package com.example.aiad.web.dto;

public class ImageGenerateRequest {

    private Long adId;
    private String prompt;

    public Long getAdId() {
        return adId;
    }

    public void setAdId(Long adId) {
        this.adId = adId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}


