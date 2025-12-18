package com.example.aiad.web.dto;

public class ImageResponse {

    private String imageUrl;

    public ImageResponse() {
    }

    public ImageResponse(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}


