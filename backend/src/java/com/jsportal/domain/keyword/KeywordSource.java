package com.jsportal.domain.keyword;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 키워드의 수집 출처 정보를 저장하는 엔티티 클래스
 */
@Data
@Entity
@Table(name = "keyword_sources")
public class KeywordSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword_id", nullable = false)
    private Long keywordId;

    @Column(name = "source_name", nullable = false, length = 50)
    private String sourceName;

    @Column(name = "collection_date", nullable = false)
    private LocalDateTime collectionDate;

    @Column(length = 500)
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
} 