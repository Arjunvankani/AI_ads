package com.example.aiad.repository;

import com.example.aiad.model.AdCreative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdCreativeRepository extends JpaRepository<AdCreative, Long> {

    @Query("SELECT a FROM AdCreative a ORDER BY a.createdAt DESC LIMIT 5")
    List<AdCreative> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT a FROM AdCreative a WHERE " +
           "LOWER(a.prompt) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.headline) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.cta) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.surveyQuestion) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY a.createdAt DESC")
    List<AdCreative> searchByTerm(@Param("searchTerm") String searchTerm);
}


