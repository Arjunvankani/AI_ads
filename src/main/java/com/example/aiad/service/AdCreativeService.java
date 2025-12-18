package com.example.aiad.service;

import com.example.aiad.model.AdCreative;
import com.example.aiad.repository.AdCreativeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdCreativeService {

    private final OpenAiAdService openAiAdService;
    private final AdCreativeRepository repository;

    public AdCreativeService(OpenAiAdService openAiAdService, AdCreativeRepository repository) {
        this.openAiAdService = openAiAdService;
        this.repository = repository;
    }

    @Transactional
    public AdCreative generateAndSave(String prompt) {
        AdCreative creative = openAiAdService.generateAd(prompt);
        return repository.save(creative);
    }

    public AdCreative generateOnly(String prompt) {
        return openAiAdService.generateAd(prompt);
    }

    @Transactional
    public AdCreative saveAd(AdCreative ad) {
        return repository.save(ad);
    }

    public List<AdCreative> getLastFive() {
        return repository.findTop5ByOrderByCreatedAtDesc();
    }

    public List<AdCreative> searchAds(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getLastFive(); // Return recent ads if no search term
        }
        return repository.searchByTerm(searchTerm.trim());
    }
}


