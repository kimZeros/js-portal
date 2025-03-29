package com.jsportal.service.batch;

import com.jsportal.domain.content.Content;
import com.jsportal.domain.crawling.CommunitySource;
import com.jsportal.service.api.OpenAiService;
import com.jsportal.service.crawling.CrawlingService;
import com.jsportal.service.crawling.CrawlingService.CrawledPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 커뮤니티 사이트에서 포스트를 자동으로 크롤링하고 콘텐츠를 생성하는 배치 서비스
 */
@Service
public class CommunityPostCrawlingBatchService {

    private static final Logger logger = LoggerFactory.getLogger(CommunityPostCrawlingBatchService.class);
    
    @Value("${crawling.daily.limit:100}")
    private int dailyCrawlingLimit;
    
    @Value("${content.generation.daily.limit:30}")
    private int dailyContentGenerationLimit;
    
    @Value("${crawling.forbidden.keywords:}")
    private String forbiddenKeywordsString;
    
    @Value("${crawling.max-sources-per-crawl:10}")
    private int maxSourcesPerCrawl;
    
    private final CrawlingService crawlingService;
    private final OpenAiService openAiService;
    
    // 일일 크롤링 및 콘텐츠 생성 카운터
    private final AtomicInteger dailyCrawledCount = new AtomicInteger(0);
    private final AtomicInteger dailyGeneratedCount = new AtomicInteger(0);
    
    // 한국어 커뮤니티 사이트 리스트
    private static final List<String> COMMUNITY_SITES = List.of(
        "ruliweb",
        "fmkorea",
        "dcinside",
        "theqoo",
        "slrclub",
        "humoruniv",
        "ppomppu",
        "clien",
        "bobae",
        "inven",
        "mlbpark"
    );
    
    @Autowired
    public CommunityPostCrawlingBatchService(CrawlingService crawlingService, 
                                           OpenAiService openAiService) {
        this.crawlingService = crawlingService;
        this.openAiService = openAiService;
    }
    
    /**
     * 자정에 일일 카운터 초기화
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyCounters() {
        logger.info("Resetting daily crawling and content generation counters");
        dailyCrawledCount.set(0);
        dailyGeneratedCount.set(0);
    }
    
    /**
     * 모든 커뮤니티 소스를 주기적으로 크롤링
     * 3시간마다 실행
     */
    @Scheduled(cron = "0 0 */3 * * ?")
    public void crawlAllCommunitySources() {
        logger.info("Starting scheduled crawling of all community sources");
        
        // 최대 소스 크롤링 개수 제한
        int sourcesToCrawl = Math.min(COMMUNITY_SITES.size(), maxSourcesPerCrawl);
        
        for (int i = 0; i < sourcesToCrawl; i++) {
            String siteName = COMMUNITY_SITES.get(i);
            CommunitySource source = new CommunitySource(siteName, "ko");
            
            try {
                int crawledCount = crawlCommunitySource(source);
                logger.info("Crawled {} posts from {}", crawledCount, source.getName());
            } catch (Exception e) {
                logger.error("Error crawling source {}: {}", source.getName(), e.getMessage(), e);
            }
        }
        
        logger.info("Completed scheduled crawling of all community sources");
    }
    
    /**
     * 크롤링된 포스트로부터 콘텐츠 생성
     * 5시간마다 실행
     */
    @Scheduled(cron = "0 0 */5 * * ?")
    public void generateContentFromCrawledPosts() {
        logger.info("Starting scheduled content generation from crawled posts");
        
        try {
            int count = generateContentFromPosts();
            logger.info("Generated {} contents from crawled posts", count);
        } catch (Exception e) {
            logger.error("Error generating content: {}", e.getMessage(), e);
        }
        
        logger.info("Completed scheduled content generation from crawled posts");
    }
    
    /**
     * 특정 커뮤니티 소스 크롤링
     * @param source 커뮤니티 소스 정보
     * @return 처리된 포스트 수
     */
    private int crawlCommunitySource(CommunitySource source) {
        // 일일 제한 확인
        if (dailyCrawledCount.get() >= dailyCrawlingLimit) {
            logger.info("Daily crawling limit reached: {}", dailyCrawlingLimit);
            return 0;
        }
        
        List<String> forbiddenKeywords = Arrays.asList(forbiddenKeywordsString.split(","));
        
        // 크롤링 실행
        List<CrawledPost> posts = crawlingService.crawlPopularPosts(source.getName());
        int processedCount = 0;
        
        for (CrawledPost post : posts) {
            // 일일 제한 확인
            if (dailyCrawledCount.get() >= dailyCrawlingLimit) {
                break;
            }
            
            // 금지 키워드 확인
            boolean containsForbiddenKeyword = false;
            for (String keyword : forbiddenKeywords) {
                if (keyword.isEmpty()) continue;
                
                if (post.getTitle().toLowerCase().contains(keyword.toLowerCase()) || 
                    post.getContent().toLowerCase().contains(keyword.toLowerCase())) {
                    containsForbiddenKeyword = true;
                    break;
                }
            }
            
            if (containsForbiddenKeyword) {
                logger.info("Skipping post with forbidden keyword: {}", post.getTitle());
                continue;
            }
            
            try {
                // 포스트 처리 및 저장
                crawlingService.processAndSaveCrawledPost(post);
                dailyCrawledCount.incrementAndGet();
                processedCount++;
            } catch (Exception e) {
                logger.error("Error processing post: {}", e.getMessage());
            }
        }
        
        return processedCount;
    }
    
    /**
     * 크롤링된 포스트로부터 콘텐츠 생성
     * @return 생성된 콘텐츠 수
     */
    private int generateContentFromPosts() {
        // 일일 제한 확인
        if (dailyGeneratedCount.get() >= dailyContentGenerationLimit) {
            logger.info("Daily content generation limit reached: {}", dailyContentGenerationLimit);
            return 0;
        }
        
        // 이 부분은 실제 구현에서는 크롤링된 포스트 중 아직 처리되지 않은 것을 가져오는 로직으로 대체
        List<CrawledPost> unprocessedPosts = getUnprocessedPosts();
        int generatedCount = 0;
        
        for (CrawledPost post : unprocessedPosts) {
            // 일일 제한 확인
            if (dailyGeneratedCount.get() >= dailyContentGenerationLimit) {
                break;
            }
            
            try {
                // OpenAI를 통해 콘텐츠 생성
                Content content = openAiService.generateFunContent(
                    post.getContent(), 
                    post.getSource(), 
                    "ko"  // 한국어로 고정
                );
                
                if (content != null) {
                    dailyGeneratedCount.incrementAndGet();
                    generatedCount++;
                    
                    // 포스트 처리 완료 표시 (실제 구현에서 필요)
                    markPostAsProcessed(post);
                }
            } catch (Exception e) {
                logger.error("Error generating content from post: {}", e.getMessage());
            }
        }
        
        return generatedCount;
    }
    
    /**
     * 아직 처리되지 않은 크롤링된 포스트 가져오기 (예시 메서드)
     */
    private List<CrawledPost> getUnprocessedPosts() {
        // 실제 구현에서는 데이터베이스에서 아직 처리되지 않은 포스트를 조회합니다.
        // 이 예제에서는 임시로 빈 리스트를 반환합니다.
        return List.of();
    }
    
    /**
     * 크롤링된 포스트를 처리 완료로 표시 (예시 메서드)
     */
    private void markPostAsProcessed(CrawledPost post) {
        // 실제 구현에서는 데이터베이스에서 해당 포스트의 상태를 업데이트합니다.
    }
    
    /**
     * 일일 크롤링 제한 상태 조회
     */
    public int getDailyCrawlingCount() {
        return dailyCrawledCount.get();
    }
    
    /**
     * 일일 콘텐츠 생성 제한 상태 조회
     */
    public int getDailyContentGenerationCount() {
        return dailyGeneratedCount.get();
    }
    
    /**
     * 커뮤니티 소스 클래스 (실제 구현에서는 엔티티로 정의)
     */
    private static class CommunitySource {
        private final String name;
        private final String language;
        
        public CommunitySource(String name, String language) {
            this.name = name;
            this.language = language;
        }
        
        public String getName() {
            return name;
        }
        
        public String getLanguage() {
            return language;
        }
    }
} 