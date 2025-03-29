package com.jsportal.service.batch;

import com.jsportal.domain.content.Content;
import com.jsportal.domain.keyword.Keyword;
import com.jsportal.repository.ContentRepository;
import com.jsportal.repository.KeywordRepository;
import com.jsportal.service.api.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 키워드 기반으로 자동으로 콘텐츠를 생성하는 배치 서비스
 */
@Service
public class ContentGenerationBatchService {

    private static final Logger logger = LoggerFactory.getLogger(ContentGenerationBatchService.class);
    
    @Value("${content.generation.daily.limit:10}")
    private int dailyContentGenerationLimit;
    
    @Value("${content.generation.interval.seconds:60}")
    private int contentGenerationIntervalSeconds;
    
    private final KeywordRepository keywordRepository;
    private final ContentRepository contentRepository;
    private final OpenAiService openAiService;
    
    // 일일 콘텐츠 생성 카운터
    private final AtomicInteger dailyGeneratedCount = new AtomicInteger(0);
    
    // 한국어 설정
    private static final String LANGUAGE = "ko";
    
    @Autowired
    public ContentGenerationBatchService(KeywordRepository keywordRepository,
                                       ContentRepository contentRepository,
                                       OpenAiService openAiService) {
        this.keywordRepository = keywordRepository;
        this.contentRepository = contentRepository;
        this.openAiService = openAiService;
    }
    
    /**
     * 자정에 일일 카운터 초기화
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyCounter() {
        logger.info("Resetting daily content generation counter");
        dailyGeneratedCount.set(0);
    }
    
    /**
     * 매일 오전 8시에 자동으로 콘텐츠 생성
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void generateContent() {
        logger.info("Starting scheduled content generation");
        
        try {
            int count = generateContentFromKeywords();
            logger.info("Generated {} contents", count);
        } catch (Exception e) {
            logger.error("Error generating content: {}", e.getMessage(), e);
        }
        
        logger.info("Completed scheduled content generation");
    }
    
    /**
     * 키워드를 기반으로 콘텐츠 생성
     * @return 생성된 콘텐츠 수
     */
    public int generateContentFromKeywords() {
        // 일일 제한 확인
        if (dailyGeneratedCount.get() >= dailyContentGenerationLimit) {
            logger.info("Daily content generation limit reached: {}", dailyContentGenerationLimit);
            return 0;
        }
        
        // 콘텐츠 생성에 사용할 키워드 가져오기
        List<Keyword> activeKeywords = getKeywordsForContentGeneration();
        int generatedCount = 0;
        
        for (Keyword keyword : activeKeywords) {
            // 일일 제한 확인
            if (dailyGeneratedCount.get() >= dailyContentGenerationLimit) {
                break;
            }
            
            try {
                // OpenAI를 통해 콘텐츠 생성
                Content content = openAiService.generateKeywordContent(
                    keyword.getKeyword(),
                    keyword.getCategory(),
                    LANGUAGE
                );
                
                if (content != null) {
                    dailyGeneratedCount.incrementAndGet();
                    generatedCount++;
                    
                    // 키워드 최근 생성 시간 업데이트
                    keyword.setLastGeneratedAt(LocalDateTime.now());
                    keywordRepository.save(keyword);
                }
                
                // 콘텐츠 생성 간격 대기
                Thread.sleep(contentGenerationIntervalSeconds * 1000L);
            } catch (Exception e) {
                logger.error("Error generating content for keyword {}: {}", keyword.getKeyword(), e.getMessage());
            }
        }
        
        return generatedCount;
    }
    
    /**
     * 콘텐츠 생성에 사용할 키워드 목록 조회
     * @return 키워드 목록
     */
    private List<Keyword> getKeywordsForContentGeneration() {
        // 활성 상태이며, 아직 콘텐츠가 생성되지 않았거나 일정 기간 이상 지난 키워드 선택
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7); // 7일 이상 지난 키워드는 재사용
        
        return keywordRepository.findByLanguageAndActiveAndLastGeneratedAtIsNullOrLastGeneratedAtBefore(
            LANGUAGE, true, cutoffDate);
    }
    
    /**
     * 수동으로 특정 키워드에 대한 콘텐츠 생성
     * @param keyword 키워드
     * @param category 카테고리
     * @return 생성된 콘텐츠 또는 null
     */
    public Content generateContentForKeyword(String keyword, String category) {
        try {
            Content content = openAiService.generateKeywordContent(
                keyword,
                category,
                LANGUAGE
            );
            
            if (content != null) {
                // 해당 키워드가 DB에 존재하는지 확인하고 최근 생성 시간 업데이트
                keywordRepository.findByKeywordAndLanguage(keyword, LANGUAGE)
                    .ifPresent(existingKeyword -> {
                        existingKeyword.setLastGeneratedAt(LocalDateTime.now());
                        keywordRepository.save(existingKeyword);
                    });
            }
            
            return content;
        } catch (Exception e) {
            logger.error("Error generating content for keyword {}: {}", keyword, e.getMessage());
            return null;
        }
    }
    
    /**
     * 특정 기간 동안 생성된 콘텐츠 수 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 생성된 콘텐츠 수
     */
    public long countGeneratedContents(LocalDateTime startDate, LocalDateTime endDate) {
        return contentRepository.countByCreatedAtBetween(startDate, endDate);
    }
} 