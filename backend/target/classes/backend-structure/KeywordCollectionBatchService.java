package com.jsportal.service.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsportal.domain.keyword.Keyword;
import com.jsportal.domain.keyword.KeywordSource;
import com.jsportal.repository.KeywordRepository;
import com.jsportal.repository.KeywordSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 다양한 소스에서 인기 키워드를 수집하는 배치 서비스
 */
@Service
public class KeywordCollectionBatchService {

    private static final Logger logger = LoggerFactory.getLogger(KeywordCollectionBatchService.class);
    
    @Value("${google.trends.api.url:https://trends.google.com/trends/api/dailytrends}")
    private String googleTrendsApiUrl;
    
    @Value("${naver.datalab.api.url:https://openapi.naver.com/v1/datalab/search}")
    private String naverDatalabApiUrl;
    
    @Value("${naver.client.id}")
    private String naverClientId;
    
    @Value("${naver.client.secret}")
    private String naverClientSecret;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KeywordRepository keywordRepository;
    private final KeywordSourceRepository keywordSourceRepository;
    
    // 언어별 국가 코드 맵핑
    private static final Map<String, String> LANGUAGE_TO_GEO = Map.of(
        "ko", "KR",
        "en", "US",
        "ja", "JP"
    );
    
    public KeywordCollectionBatchService(RestTemplate restTemplate, 
                                        ObjectMapper objectMapper,
                                        KeywordRepository keywordRepository,
                                        KeywordSourceRepository keywordSourceRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.keywordRepository = keywordRepository;
        this.keywordSourceRepository = keywordSourceRepository;
    }
    
    /**
     * 모든 지원 언어에 대해 키워드를 수집하는 스케줄링된 작업
     * 매일 오전 6시에 실행
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void collectKeywordsForAllLanguages() {
        logger.info("Starting scheduled keyword collection for all languages");
        
        for (String language : LANGUAGE_TO_GEO.keySet()) {
            try {
                collectKeywords(language);
            } catch (Exception e) {
                logger.error("Error collecting keywords for language {}: {}", language, e.getMessage(), e);
            }
        }
        
        logger.info("Completed scheduled keyword collection for all languages");
    }
    
    /**
     * 특정 언어에 대한 인기 키워드 수집
     * @param language 언어 코드 (ko, en, ja)
     * @return 수집된 키워드 수
     */
    public int collectKeywords(String language) {
        logger.info("Collecting keywords for language: {}", language);
        
        List<String> keywords = new ArrayList<>();
        
        // Google Trends에서 키워드 수집
        try {
            List<String> googleTrendsKeywords = collectGoogleTrendsKeywords(language);
            keywords.addAll(googleTrendsKeywords);
            logger.info("Collected {} keywords from Google Trends for {}", googleTrendsKeywords.size(), language);
        } catch (Exception e) {
            logger.error("Error collecting keywords from Google Trends for {}: {}", language, e.getMessage());
        }
        
        // Naver DataLab에서 키워드 수집 (한국어만 해당)
        if ("ko".equals(language)) {
            try {
                List<String> naverKeywords = collectNaverDatalabKeywords();
                keywords.addAll(naverKeywords);
                logger.info("Collected {} keywords from Naver DataLab", naverKeywords.size());
            } catch (Exception e) {
                logger.error("Error collecting keywords from Naver DataLab: {}", e.getMessage());
            }
        }
        
        // 수집된 키워드 중복 제거
        List<String> uniqueKeywords = keywords.stream()
                .distinct()
                .collect(Collectors.toList());
        
        // 키워드 저장
        int savedCount = 0;
        for (String keyword : uniqueKeywords) {
            if (saveKeyword(keyword, language)) {
                savedCount++;
            }
        }
        
        logger.info("Saved {} new keywords for language {}", savedCount, language);
        return savedCount;
    }
    
    /**
     * Google Trends API에서 인기 키워드 수집
     * @param language 언어 코드
     * @return 수집된 키워드 리스트
     */
    private List<String> collectGoogleTrendsKeywords(String language) throws Exception {
        String geo = LANGUAGE_TO_GEO.getOrDefault(language, "US");
        
        String url = googleTrendsApiUrl + 
                "?hl=" + language + 
                "&geo=" + geo + 
                "&tz=-540" +  // GMT+9 (JST/KST)
                "&cat=all" + 
                "&ns=15";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            // Google Trends API는 응답 시작 부분에 ")]}'," 문자열을 포함하기 때문에 제거해야 함
            String jsonString = response.getBody();
            if (jsonString.startsWith(")]}',")) {
                jsonString = jsonString.substring(5);
            }
            
            JsonNode rootNode = objectMapper.readTree(jsonString);
            JsonNode trendingSearchesNode = rootNode.path("default").path("trendingSearchesDays");
            
            List<String> keywords = new ArrayList<>();
            
            if (trendingSearchesNode.isArray()) {
                for (JsonNode dayNode : trendingSearchesNode) {
                    JsonNode trendsNode = dayNode.path("trendingSearches");
                    
                    if (trendsNode.isArray()) {
                        for (JsonNode trendNode : trendsNode) {
                            String keyword = trendNode.path("title").path("query").asText();
                            if (!keyword.isEmpty()) {
                                keywords.add(keyword);
                            }
                            
                            // 관련 쿼리도 수집
                            JsonNode relatedQueriesNode = trendNode.path("relatedQueries");
                            if (relatedQueriesNode.isArray()) {
                                for (JsonNode queryNode : relatedQueriesNode) {
                                    String relatedKeyword = queryNode.path("query").asText();
                                    if (!relatedKeyword.isEmpty()) {
                                        keywords.add(relatedKeyword);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            return keywords;
        } else {
            logger.error("Failed to get data from Google Trends: {}", response.getStatusCode());
            return Collections.emptyList();
        }
    }
    
    /**
     * Naver DataLab API에서 인기 키워드 수집
     * @return 수집된 키워드 리스트
     */
    private List<String> collectNaverDatalabKeywords() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", naverClientId);
        headers.set("X-Naver-Client-Secret", naverClientSecret);
        headers.set("Content-Type", "application/json");
        
        // 현재 날짜 기준으로 지난 30일 데이터 요청
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(30);
        
        String requestBody = "{\n" +
                "  \"startDate\": \"" + startDate.toLocalDate() + "\",\n" +
                "  \"endDate\": \"" + endDate.toLocalDate() + "\",\n" +
                "  \"timeUnit\": \"date\",\n" +
                "  \"keywordGroups\": [\n" +
                "    {\n" +
                "      \"groupName\": \"트렌드\",\n" +
                "      \"keywords\": [\"트렌드\", \"인기\", \"화제\", \"이슈\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                naverDatalabApiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode resultsNode = rootNode.path("results");
            
            List<String> keywords = new ArrayList<>();
            
            if (resultsNode.isArray()) {
                for (JsonNode resultNode : resultsNode) {
                    JsonNode dataNode = resultNode.path("data");
                    
                    if (dataNode.isArray()) {
                        for (JsonNode dataPoint : dataNode) {
                            // 여기서는 단순화된 예제이며, 실제로는 연관 검색어나 
                            // 추가 API 호출이 필요할 수 있음
                            String keyword = dataPoint.path("period").asText();
                            if (!keyword.isEmpty() && Character.isLetter(keyword.charAt(0))) {
                                keywords.add(keyword);
                            }
                        }
                    }
                }
            }
            
            return keywords;
        } else {
            logger.error("Failed to get data from Naver DataLab: {}", response.getStatusCode());
            return Collections.emptyList();
        }
    }
    
    /**
     * 수집된 키워드를 데이터베이스에 저장
     * @param term 키워드 문자열
     * @param language 언어 코드
     * @return 저장 성공 여부
     */
    private boolean saveKeyword(String term, String language) {
        // 이미 존재하는 키워드인지 확인
        if (keywordRepository.existsByTermAndLanguage(term, language)) {
            logger.debug("Keyword already exists: {} ({})", term, language);
            return false;
        }
        
        try {
            // 새 키워드 저장
            Keyword keyword = new Keyword();
            keyword.setTerm(term);
            keyword.setLanguage(language);
            keyword.setCreatedAt(LocalDateTime.now());
            keyword.setStatus("ACTIVE");
            keyword.setPriority(getKeywordPriority(term));
            
            keyword = keywordRepository.save(keyword);
            
            // 키워드 소스 정보 저장
            KeywordSource source = new KeywordSource();
            source.setKeywordId(keyword.getId());
            source.setSourceName("BATCH_COLLECTION");
            source.setCollectedAt(LocalDateTime.now());
            keywordSourceRepository.save(source);
            
            logger.debug("Saved new keyword: {} ({})", term, language);
            return true;
        } catch (Exception e) {
            logger.error("Error saving keyword {}: {}", term, e.getMessage());
            return false;
        }
    }
    
    /**
     * 키워드 우선순위 결정
     * 향후 CPC 데이터나 다른 요소를 통해 우선순위를 결정할 수 있음
     * @param keyword 키워드
     * @return 우선순위 점수
     */
    private int getKeywordPriority(String keyword) {
        // 향후 구글 AdWords API 등을 통해 CPC 정보를 가져와서 우선순위 결정 가능
        // 현재는 단순하게 키워드 길이에 따라 우선순위 결정 (예시)
        int length = keyword.length();
        
        // 너무 짧거나 긴 키워드는 낮은 우선순위
        if (length < 2 || length > 30) {
            return 1;
        } else if (length < 5) {
            return 2;
        } else if (length < 10) {
            return 3;
        } else {
            return 2;
        }
    }
    
    /**
     * 수동으로 키워드 추가
     * @param term 키워드 문자열
     * @param language 언어 코드
     * @param priority 우선순위 (null인 경우 자동 계산)
     * @return 저장된 키워드
     */
    public Keyword addKeywordManually(String term, String language, Integer priority) {
        if (priority == null) {
            priority = getKeywordPriority(term);
        }
        
        Keyword keyword = new Keyword();
        keyword.setTerm(term);
        keyword.setLanguage(language);
        keyword.setCreatedAt(LocalDateTime.now());
        keyword.setStatus("ACTIVE");
        keyword.setPriority(priority);
        keyword.setManuallyAdded(true);
        
        keyword = keywordRepository.save(keyword);
        
        // 키워드 소스 정보 저장
        KeywordSource source = new KeywordSource();
        source.setKeywordId(keyword.getId());
        source.setSourceName("MANUAL_ENTRY");
        source.setCollectedAt(LocalDateTime.now());
        keywordSourceRepository.save(source);
        
        logger.info("Manually added keyword: {} ({}) with priority {}", term, language, priority);
        return keyword;
    }
    
    /**
     * 특정 기간 동안 수집된 활성 키워드 목록 조회
     * @param language 언어 코드
     * @param days 최근 일수
     * @return 활성 키워드 목록
     */
    public List<Keyword> getActiveKeywords(String language, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return keywordRepository.findActiveKeywordsByLanguageAndCreatedAfter(language, startDate);
    }
} 