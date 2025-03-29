package com.jsportal.repository;

import com.jsportal.domain.content.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 콘텐츠 엔티티를 위한 리포지토리 인터페이스
 */
@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {

    /**
     * 특정 타입의 콘텐츠 목록을 검색합니다.
     */
    Page<Content> findByType(String type, Pageable pageable);
    
    /**
     * 특정 상태의 콘텐츠 목록을 검색합니다.
     */
    Page<Content> findByStatus(String status, Pageable pageable);
    
    /**
     * 특정 언어의 콘텐츠 목록을 검색합니다.
     */
    Page<Content> findByLanguage(String language, Pageable pageable);
    
    /**
     * 타입과 상태로 콘텐츠 목록을 검색합니다.
     */
    Page<Content> findByTypeAndStatus(String type, String status, Pageable pageable);
    
    /**
     * 타입, 상태, 언어로 콘텐츠 목록을 검색합니다.
     */
    Page<Content> findByTypeAndStatusAndLanguage(String type, String status, String language, Pageable pageable);
    
    /**
     * 슬러그로 콘텐츠를 검색합니다.
     */
    Optional<Content> findBySlug(String slug);
    
    /**
     * 특정 기간 내에 생성된 콘텐츠 수를 계산합니다.
     */
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 현재 날짜에 게시된 콘텐츠 목록을 검색합니다.
     */
    @Query("SELECT c FROM Content c WHERE c.status = 'PUBLISHED' AND CAST(c.publishedAt AS date) = CURRENT_DATE")
    List<Content> findTodayPublishedContents();
    
    /**
     * 제목이나 본문에 특정 키워드가 포함된 콘텐츠를 검색합니다.
     */
    @Query("SELECT c FROM Content c WHERE (LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.body) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND c.status = 'PUBLISHED'")
    Page<Content> searchByKeyword(String keyword, Pageable pageable);
} 