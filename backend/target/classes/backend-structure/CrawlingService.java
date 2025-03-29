package com.jsportal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsportal.domain.content.Content;
import com.jsportal.domain.content.ContentSource;
import com.jsportal.repository.ContentRepository;
import com.jsportal.repository.ContentSourceRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // 크롤링할 사이트 정보와 선택자를 저장하는 맵
    private final Map<String, CrawlConfig> crawlConfigs = new HashMap<>();

    public CrawlingService(RestTemplate restTemplate, ObjectMapper objectMapper, 
                          OpenAiService openAiService, ContentRepository contentRepository,
                          ContentSourceRepository contentSourceRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.openAiService = openAiService;
        this.contentRepository = contentRepository;
        this.contentSourceRepository = contentSourceRepository;
        
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
        CrawlConfig config = crawlConfigs.get(sourceName);
        if (config == null) {
            throw new IllegalArgumentException("Unsupported source: " + sourceName);
        }
        
        List<CrawledPost> posts = new ArrayList<>();
        
        try {
            Document doc = Jsoup.connect(config.getUrl())
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .get();
            
            Elements items = doc.select(config.getItemSelector());
            
            for (Element item : items) {
                try {
                    Element titleElement = item.selectFirst(config.getTitleSelector());
                    if (titleElement == null) continue;
                    
                    String title = titleElement.text();
                    String url = titleElement.attr("href");
                    if (!url.startsWith("http")) {
                        url = config.getBaseUrl() + url;
                    }
                    
                    String category = "";
                    Element categoryElement = item.selectFirst(config.getCategorySelector());
                    if (categoryElement != null) {
                        category = categoryElement.text();
                    }
                    
                    CrawledPost post = new CrawledPost();
                    post.setTitle(title);
                    post.setUrl(url);
                    post.setCategory(category);
                    post.setSource(config.getName());
                    post.setLanguage(config.getLanguage());
                    
                    // 글 본문 크롤링
                    String content = crawlPostContent(url);
                    post.setContent(content);
                    
                    posts.add(post);
                } catch (Exception e) {
                    logger.warn("Error crawling post from {}: {}", sourceName, e.getMessage());
                }
            }
            
            logger.info("Crawled {} posts from {}", posts.size(), sourceName);
            return posts;
        } catch (IOException e) {
            logger.error("Error connecting to {}: {}", config.getUrl(), e.getMessage());
            throw new RuntimeException("Error crawling data", e);
        }
    }
    
    /**
     * 특정 글의 본문 내용을 크롤링
     */
    private String crawlPostContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .get();
            
            // 사이트별로 본문 선택자가 다를 수 있음
            // 여기서는 일반적인 패턴으로 시도
            Elements contentElements = doc.select("div.article, div.post-content, div.content, article");
            if (!contentElements.isEmpty()) {
                return contentElements.first().text();
            }
            
            return doc.body().text();
        } catch (IOException e) {
            logger.warn("Error crawling post content from {}: {}", url, e.getMessage());
            return "";
        }
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
     * 크롤링된 포스트 정보 클래스
     */
    public static class CrawledPost {
        private String title;
        private String content;
        private String url;
        private String category;
        private String source;
        private String language;
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public String getSource() {
            return source;
        }
        
        public void setSource(String source) {
            this.source = source;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public void setLanguage(String language) {
            this.language = language;
        }
    }
} 