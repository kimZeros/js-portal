package com.jsportal.domain.crawling;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 커뮤니티 사이트 정보 엔티티
 * 크롤링할 커뮤니티 사이트의 정보와 설정을 저장합니다.
 */
@Data
@Entity
@Table(name = "community_sources")
public class CommunitySource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 500)
    private String url;
    
    @Column(nullable = false, length = 10)
    private String language;
    
    @Column(name = "selector_config", nullable = false, columnDefinition = "TEXT")
    private String selectorConfig;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(name = "max_posts_per_crawl", nullable = false)
    private Integer maxPostsPerCrawl = 10;
    
    @Column(name = "crawl_interval_minutes")
    private Integer crawlIntervalMinutes = 180; // 기본값 3시간
    
    @Column(name = "last_crawled_at")
    private LocalDateTime lastCrawledAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private Integer priority = 1;
    
    @Column(length = 1000)
    private String notes;
    
    /**
     * 기본 생성자
     */
    public CommunitySource() {
    }
    
    /**
     * 이름과 언어만 사용하는 간단한 생성자
     */
    public CommunitySource(String name, String language) {
        this.name = name;
        this.language = language;
        this.url = "https://" + name + ".com";
        this.selectorConfig = "{}"; // 기본 설정
    }
    
    /**
     * 생성 시 호출되는 메서드
     */
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * 업데이트 시 호출되는 메서드
     */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
} 