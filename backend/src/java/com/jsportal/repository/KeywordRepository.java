package com.jsportal.repository;

import com.jsportal.domain.keyword.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    
    Optional<Keyword> findByKeywordAndLanguage(String keyword, String language);
    
    List<Keyword> findByLanguageAndActive(String language, boolean active);
    
    List<Keyword> findByLanguageAndActiveAndUpdatedAtAfter(String language, boolean active, LocalDateTime date);
    
    List<Keyword> findByLanguageAndCategoryAndActive(String language, String category, boolean active);
    
    List<Keyword> findTop20ByLanguageAndActiveOrderByPriorityDesc(String language, boolean active);
    
    long countByLanguageAndCreatedAtAfter(String language, LocalDateTime date);
    
    @Query("SELECT k FROM Keyword k WHERE k.language = :language AND k.active = :active AND (k.lastGeneratedAt IS NULL OR k.lastGeneratedAt < :cutoffDate) ORDER BY k.priority DESC")
    List<Keyword> findByLanguageAndActiveAndLastGeneratedAtIsNullOrLastGeneratedAtBefore(
        @Param("language") String language, 
        @Param("active") boolean active, 
        @Param("cutoffDate") LocalDateTime cutoffDate);
} 