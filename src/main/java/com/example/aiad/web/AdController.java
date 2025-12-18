package com.example.aiad.web;

import com.example.aiad.model.AdCreative;
import com.example.aiad.service.AdCreativeService;
import com.example.aiad.service.ImageGenerationService;
import com.example.aiad.web.dto.AdGenerateRequest;
import com.example.aiad.web.dto.AdResponse;
import com.example.aiad.web.dto.ImageGenerateRequest;
import com.example.aiad.web.dto.ImageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ad")
@CrossOrigin
public class AdController {

    private final AdCreativeService adCreativeService;
    private final ImageGenerationService imageGenerationService;

    public AdController(AdCreativeService adCreativeService,
                        ImageGenerationService imageGenerationService) {
        this.adCreativeService = adCreativeService;
        this.imageGenerationService = imageGenerationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<AdResponse> generate(@RequestBody AdGenerateRequest request) {
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AdCreative created = adCreativeService.generateOnly(request.getPrompt());
        return new ResponseEntity<>(toResponse(created), HttpStatus.OK);
    }

    @PostMapping("/save")
    public ResponseEntity<AdResponse> save(@RequestBody AdResponse adResponse) {
        if (adResponse == null || adResponse.getPrompt() == null) {
            return ResponseEntity.badRequest().build();
        }

        AdCreative ad = new AdCreative();
        ad.setPrompt(adResponse.getPrompt());
        ad.setHeadline(adResponse.getHeadline());
        ad.setCta(adResponse.getCta());
        ad.setSurveyQuestion(adResponse.getSurveyQuestion());
        ad.setImageUrl(adResponse.getImageUrl());

        AdCreative saved = adCreativeService.saveAd(ad);
        return new ResponseEntity<>(toResponse(saved), HttpStatus.CREATED);
    }

    @GetMapping("/history")
    public List<AdResponse> history() {
        return adCreativeService.getLastFive()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/search")
    public List<AdResponse> search(@RequestParam(required = false) String q) {
        return adCreativeService.searchAds(q)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/image")
    public ResponseEntity<ImageResponse> generateImage(@RequestBody ImageGenerateRequest request) {
        if (request.getAdId() == null) {
            return ResponseEntity.badRequest().build();
        }
        String imageUrl = imageGenerationService.generateAndSaveImage(request.getAdId(), request.getPrompt());
        return ResponseEntity.ok(new ImageResponse(imageUrl));
    }

    private AdResponse toResponse(AdCreative creative) {
        AdResponse response = new AdResponse();
        response.setId(creative.getId());
        response.setPrompt(creative.getPrompt());
        response.setHeadline(creative.getHeadline());
        response.setCta(creative.getCta());
        response.setSurveyQuestion(creative.getSurveyQuestion());
        response.setCreatedAt(creative.getCreatedAt());
        response.setImageUrl(creative.getImageUrl());
        return response;
    }
}


