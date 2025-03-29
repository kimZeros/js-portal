package com.jsportal.service.batch;

import com.jsportal.domain.content.Content;
import com.jsportal.domain.content.ContentStatus;
import com.jsportal.domain.keyword.Keyword;
import com.jsportal.repository.ContentRepository;
import com.jsportal.repository.KeywordRepository;
import com.jsportal.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 키워드 기반으로 자동으로 콘텐츠를 생성하는 배치 서비스
 */
@Service
public class ContentGenerationBatchService {

    private static final Logger logger = LoggerFactory.getLogger(ContentGenerationBatchService.class);
    
    @Value("${content.generation.daily.limit:10}")
    private int dailyGenerationLimit;
    
    @Value("${content.generation.interval.seconds:60}")
    private int generationIntervalSeconds;
    
    private final OpenAiService openAiService;
    private final KeywordRepository keywordRepository;
    private final ContentRepository contentRepository;
    
    public ContentGenerationBatchService(OpenAiService openAiService,
                                        KeywordRepository keywordRepository,
                                        ContentRepository contentRepository) {
        this.openAiService = openAiService;
        this.keywordRepository = keywordRepository;
        this.contentRepository = contentRepository;
    }
    
    /**
     * 모든 지원 언어에 대해 콘텐츠를 생성하는 스케줄링된 작업
     * 매일 오전 8시에 실행
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void generateContentForAllLanguages() {
        logger.info("Starting scheduled content generation for all languages");
        
        // 지원하는 언어 목록
        List<String> languages = List.of("ko", "en", "ja");
        
        for (String language : languages) {
            try {
                int count = generateContentForLanguage(language, dailyGenerationLimit);
                logger.info("Generated {} contents for language {}", count, language);
            } catch (Exception e) {
                logger.error("Error generating content for language {}: {}", language, e.getMessage(), e);
            }
        }
        
        logger.info("Completed scheduled content generation for all languages");
    }
    
    /**
     * 특정 언어에 대한 콘텐츠 생성
     * @param language 언어 코드
     * @param limit 생성할 최대 콘텐츠 수
     * @return 생성된 콘텐츠 수
     */
    public int generateContentForLanguage(String language, int limit) {
        // 최근 7일 동안 수집된 우선순위가 높은 키워드 가져오기
        List<Keyword> keywords = keywordRepository.findTopKeywordsByLanguageAndCreatedAfter(
                language, 
                LocalDateTime.now().minusDays(7),
                "ACTIVE",
                limit
        );
        
        logger.info("Found {} keywords for content generation in {}", keywords.size(), language);
        
        int generatedCount = 0;
        
        for (Keyword keyword : keywords) {
            // 이미 이 키워드로 콘텐츠가 생성되었는지 확인
            if (contentRepository.existsByKeywordId(keyword.getId())) {
                logger.debug("Content already exists for keyword: {}", keyword.getTerm());
                continue;
            }
            
            try {
                // OpenAI API를 통해 콘텐츠 생성
                Content content = openAiService.generateInfoContent(keyword, language);
                if (content != null) {
                    generatedCount++;
                    logger.info("Generated content for keyword: {} ({})", keyword.getTerm(), language);
                    
                    // API 호출 간격 조절을 위한 대기
                    TimeUnit.SECONDS.sleep(generationIntervalSeconds);
                }
                
                // 제한에 도달하면 중단
                if (generatedCount >= limit) {
                    break;
                }
            } catch (Exception e) {
                logger.error("Error generating content for keyword {}: {}", keyword.getTerm(), e.getMessage());
            }
        }
        
        return generatedCount;
    }
    
    /**
     * 특정 키워드에 대한 콘텐츠 수동 생성
     * @param keywordId 키워드 ID
     * @return 생성된 콘텐츠
     */
    public Content generateContentForKeyword(Long keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("Keyword not found with ID: " + keywordId));
        
        // 이미 이 키워드로 콘텐츠가 생성되었는지 확인
        if (contentRepository.existsByKeywordId(keyword.getId())) {
            throw new IllegalStateException("Content already exists for this keyword");
        }
        
        try {
            // OpenAI API를 통해 콘텐츠 생성
            Content content = openAiService.generateInfoContent(keyword, keyword.getLanguage());
            if (content != null) {
                logger.info("Manually generated content for keyword: {}", keyword.getTerm());
                return content;
            } else {
                throw new RuntimeException("Failed to generate content");
            }
        } catch (Exception e) {
            logger.error("Error generating content for keyword {}: {}", keyword.getTerm(), e.getMessage());
            throw new RuntimeException("Error generating content: " + e.getMessage(), e);
        }
    }
    
    /**
     * 기존 콘텐츠 재생성
     * @param contentId 콘텐츠 ID
     * @return 재생성된 콘텐츠
     */
    public Content regenerateContent(Long contentId) {
        Content existingContent = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found with ID: " + contentId));
        
        Keyword keyword = keywordRepository.findById(existingContent.getKeywordId())
                .orElseThrow(() -> new IllegalArgumentException("Related keyword not found"));
        
        try {
            // 기존 콘텐츠 상태 업데이트
            existingContent.setStatus(ContentStatus.ARCHIVED.name());
            contentRepository.save(existingContent);
            
            // 새로운 콘텐츠 생성
            Content newContent = openAiService.generateInfoContent(keyword, keyword.getLanguage());
            if (newContent != null) {
                logger.info("Regenerated content for keyword: {}", keyword.getTerm());
                return newContent;
            } else {
                // 새 콘텐츠 생성에 실패한 경우 기존 콘텐츠 복구
                existingContent.setStatus(ContentStatus.PUBLISHED.name());
                contentRepository.save(existingContent);
                throw new RuntimeException("Failed to regenerate content");
            }
        } catch (Exception e) {
            // 오류 발생 시 기존 콘텐츠 복구
            existingContent.setStatus(ContentStatus.PUBLISHED.name());
            contentRepository.save(existingContent);
            
            logger.error("Error regenerating content: {}", e.getMessage());
            throw new RuntimeException("Error regenerating content: " + e.getMessage(), e);
        }
    }
    
    /**
     * 지정된 기간 동안 생성된 콘텐츠 수를 계산
     * @param startDate 시작 날짜/시간
     * @param endDate 종료 날짜/시간
     * @return 생성된 콘텐츠 수
     */
    public int countGeneratedContentsBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return contentRepository.countByCreatedAtBetween(startDate, endDate);
    }
    
    /**
     * 콘텐츠 생성 일별 제한 조회
     * @return 일별 제한 수
     */
    public int getDailyGenerationLimit() {
        return dailyGenerationLimit;
    }
    
    /**
     * 콘텐츠 생성 일별 제한 설정
     * @param limit 새로운 일별 제한 수
     */
    public void setDailyGenerationLimit(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Daily limit cannot be negative");
        }
        this.dailyGenerationLimit = limit;
    }
    
    /**
     * 콘텐츠 생성 간격 조회 (초 단위)
     * @return 생성 간격 (초)
     */
    public int getGenerationIntervalSeconds() {
        return generationIntervalSeconds;
    }
    
    /**
     * 콘텐츠 생성 간격 설정 (초 단위)
     * @param seconds 새로운 생성 간격 (초)
     */
    public void setGenerationIntervalSeconds(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Interval cannot be negative");
        }
        this.generationIntervalSeconds = seconds;
    }
} 