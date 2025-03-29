package com.jsportal.repository;

import com.jsportal.domain.crawling.CommunitySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 커뮤니티 소스 엔티티를 위한 저장소 인터페이스
 */
@Repository
public interface CommunitySourceRepository extends JpaRepository<CommunitySource, Long> {

    /**
     * 이름으로 커뮤니티 소스 찾기
     */
    Optional<CommunitySource> findByName(String name);
    
    /**
     * 활성화된 모든 커뮤니티 소스 조회
     */
    List<CommunitySource> findByActiveTrue();
    
    /**
     * 특정 언어의 활성화된 커뮤니티 소스 조회
     */
    List<CommunitySource> findByLanguageAndActiveTrue(String language);
    
    /**
     * 크롤링이 필요한 커뮤니티 소스 조회
     * 마지막 크롤링 시간이 없거나 설정된 간격 이상 지난 소스를 찾습니다.
     */
    @Query("SELECT cs FROM CommunitySource cs WHERE cs.active = true " +
           "AND (cs.lastCrawledAt IS NULL OR cs.lastCrawledAt < :timeThreshold) " +
           "ORDER BY cs.priority DESC")
    List<CommunitySource> findSourcesNeedingCrawl(LocalDateTime timeThreshold);
} 