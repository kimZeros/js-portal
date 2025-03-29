package com.jsportal.domain.keyword;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 키워드 정보를 저장하는 엔티티 클래스
 */
@Data
@Entity
@Table(name = "keywords", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"keyword", "language"})
})
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 20)
    private String source; // api, manual, etc.

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_generated_at")
    private LocalDateTime lastGeneratedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
} 