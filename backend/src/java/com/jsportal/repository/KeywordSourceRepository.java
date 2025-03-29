package com.jsportal.repository;

import com.jsportal.domain.keyword.KeywordSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KeywordSourceRepository extends JpaRepository<KeywordSource, Long> {
    
    List<KeywordSource> findByKeywordId(Long keywordId);
    
    List<KeywordSource> findBySourceName(String sourceName);
    
    List<KeywordSource> findByCollectionDateAfter(LocalDateTime date);
    
    List<KeywordSource> findByKeywordIdAndSourceName(Long keywordId, String sourceName);
} 