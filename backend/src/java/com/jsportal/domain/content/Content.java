package com.jsportal.domain.content;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 콘텐츠 엔티티
 * 시스템에서 생성된 모든 콘텐츠를 저장합니다.
 */
@Data
@Entity
@Table(name = "contents")
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(length = 255)
    private String slug;
    
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    
    @Column(length = 1000)
    private String excerpt;
    
    @Column(nullable = false, length = 50)
    private String type; // FUN, INFO, NEWS 등
    
    @Column(nullable = false, length = 50)
    private String status; // DRAFT, PUBLISHED, ARCHIVED 등
    
    @Column(nullable = false, length = 10)
    private String language; // ko, en, ja 등
    
    @Column(length = 100)
    private String source; // 콘텐츠 생성 소스 (OpenAI, 수동 등)
    
    @Column(name = "original_source", length = 100)
    private String originalSource; // 원본 콘텐츠 출처 (크롤링 소스 등)
    
    @Column(length = 50)
    private String category; // 콘텐츠 카테고리 (general, tech, health 등)
    
    @Column(length = 100)
    private String keyword; // 콘텐츠 생성에 사용된 키워드
    
    @Column(length = 255)
    private String thumbnail;
    
    @Column(length = 100)
    private String author;
    
    @Column(name = "view_count")
    private Integer viewCount = 0;
    
    @Column(name = "like_count")
    private Integer likeCount = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    /**
     * 기본 생성자
     */
    public Content() {
    }
    
    /**
     * 생성 시 호출되는 메서드
     */
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status != null && this.status.equals("PUBLISHED") && this.publishedAt == null) {
            this.publishedAt = this.createdAt;
        }
    }
    
    /**
     * 업데이트 시 호출되는 메서드
     */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status != null && this.status.equals("PUBLISHED") && this.publishedAt == null) {
            this.publishedAt = this.updatedAt;
        }
    }
} 