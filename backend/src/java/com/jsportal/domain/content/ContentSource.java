package com.jsportal.domain.content;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 콘텐츠의 원본 출처 정보를 저장하는 엔티티 클래스
 */
@Data
@Entity
@Table(name = "content_sources")
public class ContentSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
} 