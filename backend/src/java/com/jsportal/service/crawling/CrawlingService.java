package com.jsportal.service.crawling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsportal.domain.content.Content;
import com.jsportal.domain.content.ContentSource;
import com.jsportal.domain.crawling.CommunitySource;
import com.jsportal.repository.ContentRepository;
import com.jsportal.repository.ContentSourceRepository;
import com.jsportal.repository.CommunitySourceRepository;
import com.jsportal.service.api.OpenAiService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 다양한 커뮤니티 사이트로부터 콘텐츠를 크롤링하는 서비스
 */
@Service
public class CrawlingService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlingService.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiService openAiService;
    private final ContentRepository contentRepository;
    private final ContentSourceRepository contentSourceRepository;
    private final CommunitySourceRepository communitySourceRepository;

    // 크롤링할 사이트 정보와 선택자를 저장하는 맵
    private final Map<String, CrawlConfig> crawlConfigs = new HashMap<>();

    @Autowired
    public CrawlingService(RestTemplate restTemplate, ObjectMapper objectMapper, 
                          OpenAiService openAiService, ContentRepository contentRepository,
                          ContentSourceRepository contentSourceRepository,
                          CommunitySourceRepository communitySourceRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.openAiService = openAiService;
        this.contentRepository = contentRepository;
        this.contentSourceRepository = contentSourceRepository;
        this.communitySourceRepository = communitySourceRepository;
        
        initializeCrawlConfigs();
    }
    
    /**
     * 크롤링 설정 초기화
     */
    private void initializeCrawlConfigs() {
        // 한국 커뮤니티 설정
        crawlConfigs.put("ruliweb", new CrawlConfig(
            "루리웹",
            "https://bbs.ruliweb.com/best",
            "ko",
            "table.board_list_table tr.item",
            "td.subject a",
            "td.divsn a",
            "a.nick",
            "https://bbs.ruliweb.com"
        ));
        
        crawlConfigs.put("fmkorea", new CrawlConfig(
            "에펨코리아",
            "https://www.fmkorea.com/index.php?mid=best",
            "ko",
            "div.fm_best_widget ul li",
            "h3.title a",
            "span.category",
            "a.author",
            "https://www.fmkorea.com"
        ));
        
        // 일본 커뮤니티 설정
        crawlConfigs.put("5ch", new CrawlConfig(
            "5ch",
            "https://5ch.net",
            "ja",
            "div.thread",
            "a.title",
            "span.category",
            "span.name",
            ""
        ));
        
        crawlConfigs.put("girlschannel", new CrawlConfig(
            "Girls Channel",
            "https://girlschannel.net/topics/new/",
            "ja",
            "article.topic",
            "h1.topic-title a",
            "div.topic-tag",
            "div.topic-footer .name",
            ""
        ));
    }
    
    /**
     * 지정된 커뮤니티 사이트에서 인기 글 목록을 크롤링
     * @param sourceName 크롤링할 사이트 이름 (예: ruliweb, fmkorea, 5ch 등)
     * @return 크롤링된 글 목록
     */
    public List<CrawledPost> crawlPopularPosts(String sourceName) {
        logger.info("Crawling popular posts from source: {}", sourceName);
        
        // 실제 구현에서는 DB에서 소스 정보 조회
        Optional<CommunitySource> sourceOpt = communitySourceRepository.findByName(sourceName);
        
        if (sourceOpt.isEmpty()) {
            logger.error("Community source not found: {}", sourceName);
            return List.of();
        }
        
        CommunitySource source = sourceOpt.get();
        
        try {
            // 이 메서드는 실제 구현에서 해당 사이트에 맞는 크롤링 로직을 구현해야 합니다.
            // 여기서는 예시로 크롤링된 포스트를 생성합니다.
            List<CrawledPost> posts = simulateCrawling(source);
            
            // 소스의 마지막 크롤링 시간 업데이트
            source.setLastCrawledAt(LocalDateTime.now());
            communitySourceRepository.save(source);
            
            return posts;
        } catch (Exception e) {
            logger.error("Error crawling source {}: {}", sourceName, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * 크롤링 시뮬레이션 (실제 구현에서는 제거)
     */
    private List<CrawledPost> simulateCrawling(CommunitySource source) {
        List<CrawledPost> posts = new ArrayList<>();
        
        // 테스트용 가상 데이터 생성
        int postCount = Math.min(source.getMaxPostsPerCrawl(), 5); // 최대 5개
        
        for (int i = 1; i <= postCount; i++) {
            String title = String.format("[%s] 인기 게시물 #%d", source.getName(), i);
            String content = String.format("이것은 %s에서 크롤링한 인기 게시물 #%d의 내용입니다.", 
                                         source.getName(), i);
            String url = String.format("https://%s.com/posts/%d", source.getName(), i);
            
            posts.add(new CrawledPost(
                title,
                content,
                url,
                source.getName(),
                1000 + i * 100, // 임의의 좋아요 수
                50 + i * 10,    // 임의의 댓글 수
                source.getLanguage(),
                LocalDateTime.now().minusHours(i)
            ));
        }
        
        return posts;
    }
    
    /**
     * 크롤링된 포스트를 GPT를 통해 컨텐츠로 변환하고 저장
     * @param crawledPost 크롤링된 포스트
     * @return 저장된 Content 객체
     */
    public Content processAndSaveCrawledPost(CrawledPost crawledPost) {
        try {
            // GPT를 사용하여 크롤링한 글 재구성
            Content content = openAiService.generateFunContent(
                crawledPost.getContent(), 
                crawledPost.getSource(), 
                crawledPost.getLanguage()
            );
            
            // 콘텐츠 소스 정보 저장
            ContentSource source = new ContentSource();
            source.setContentId(content.getId());
            source.setSourceName(crawledPost.getSource());
            source.setSourceUrl(crawledPost.getUrl());
            contentSourceRepository.save(source);
            
            return content;
        } catch (Exception e) {
            logger.error("Error processing crawled post: {}", e.getMessage());
            throw new RuntimeException("Error processing crawled post", e);
        }
    }
    
    /**
     * 특정 사이트에서 인기 글을 크롤링하고 처리하여 자동 저장하는 메서드
     * @param sourceName 크롤링할 사이트 이름
     * @param limit 크롤링할 글 수
     * @return 처리된 글 수
     */
    public int crawlAndProcessPosts(String sourceName, int limit) {
        List<CrawledPost> posts = crawlPopularPosts(sourceName);
        int count = 0;
        
        for (CrawledPost post : posts) {
            if (count >= limit) break;
            
            try {
                processAndSaveCrawledPost(post);
                count++;
            } catch (Exception e) {
                logger.error("Error processing post: {} - {}", post.getTitle(), e.getMessage());
            }
        }
        
        return count;
    }
    
    /**
     * 크롤링 설정 클래스
     */
    private static class CrawlConfig {
        private final String name;
        private final String url;
        private final String language;
        private final String itemSelector;
        private final String titleSelector;
        private final String categorySelector;
        private final String authorSelector;
        private final String baseUrl;
        
        public CrawlConfig(String name, String url, String language, String itemSelector, 
                           String titleSelector, String categorySelector, String authorSelector, String baseUrl) {
            this.name = name;
            this.url = url;
            this.language = language;
            this.itemSelector = itemSelector;
            this.titleSelector = titleSelector;
            this.categorySelector = categorySelector;
            this.authorSelector = authorSelector;
            this.baseUrl = baseUrl;
        }
        
        public String getName() {
            return name;
        }
        
        public String getUrl() {
            return url;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public String getItemSelector() {
            return itemSelector;
        }
        
        public String getTitleSelector() {
            return titleSelector;
        }
        
        public String getCategorySelector() {
            return categorySelector;
        }
        
        public String getAuthorSelector() {
            return authorSelector;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
    }
    
    /**
     * 크롤링된 포스트 데이터 클래스
     */
    public static class CrawledPost {
        private final String title;
        private final String content;
        private final String url;
        private final String source;
        private final int likes;
        private final int comments;
        private final String language;
        private final LocalDateTime publishedAt;
        
        public CrawledPost(String title, String content, String url, String source, 
                          int likes, int comments, String language, LocalDateTime publishedAt) {
            this.title = title;
            this.content = content;
            this.url = url;
            this.source = source;
            this.likes = likes;
            this.comments = comments;
            this.language = language;
            this.publishedAt = publishedAt;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getContent() {
            return content;
        }
        
        public String getUrl() {
            return url;
        }
        
        public String getSource() {
            return source;
        }
        
        public int getLikes() {
            return likes;
        }
        
        public int getComments() {
            return comments;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public LocalDateTime getPublishedAt() {
            return publishedAt;
        }
    }
} 