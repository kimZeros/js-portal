package com.jsportal.repository;

import com.jsportal.domain.content.ContentSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentSourceRepository extends JpaRepository<ContentSource, Long> {
    
    List<ContentSource> findByContentId(Long contentId);
    
    List<ContentSource> findBySourceName(String sourceName);
    
    List<ContentSource> findBySourceNameAndSourceUrlContaining(String sourceName, String sourceUrlPart);
} 