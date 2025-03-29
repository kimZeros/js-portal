package com.jsportal.service.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsportal.domain.keyword.Keyword;
import com.jsportal.domain.keyword.KeywordSource;
import com.jsportal.repository.KeywordRepository;
import com.jsportal.repository.KeywordSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Value("${naver.client.id:NAVER_CLIENT_ID_PLACEHOLDER}")
    private String naverClientId;
    
    @Value("${naver.client.secret:NAVER_CLIENT_SECRET_PLACEHOLDER}")
    private String naverClientSecret;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KeywordRepository keywordRepository;
    private final KeywordSourceRepository keywordSourceRepository;
    
    // 한국어 전용 서비스
    private static final String LANGUAGE = "ko";
    private static final String GEO = "KR";
    
    @Autowired
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
     * 키워드를 수집하는 스케줄링된 작업
     * 매일 오전 6시에 실행
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void collectKeywords() {
        logger.info("Starting scheduled keyword collection");
        
        try {
            int count = collectKeywordsFromSources();
            logger.info("Collected {} new keywords", count);
        } catch (Exception e) {
            logger.error("Error collecting keywords: {}", e.getMessage(), e);
        }
        
        logger.info("Completed scheduled keyword collection");
    }
    
    /**
     * 다양한 소스에서 키워드 수집
     * @return 수집된 키워드 수
     */
    public int collectKeywordsFromSources() {
        logger.info("Collecting keywords");
        
        List<String> keywords = new ArrayList<>();
        
        // Google Trends에서 키워드 수집
        try {
            List<String> googleTrendsKeywords = collectGoogleTrendsKeywords();
            keywords.addAll(googleTrendsKeywords);
            logger.info("Collected {} keywords from Google Trends", googleTrendsKeywords.size());
        } catch (Exception e) {
            logger.error("Error collecting keywords from Google Trends: {}", e.getMessage());
        }
        
        // Naver DataLab에서 키워드 수집
        try {
            List<String> naverKeywords = collectNaverDatalabKeywords();
            keywords.addAll(naverKeywords);
            logger.info("Collected {} keywords from Naver DataLab", naverKeywords.size());
        } catch (Exception e) {
            logger.error("Error collecting keywords from Naver DataLab: {}", e.getMessage());
        }
        
        // 수집된 키워드 중복 제거
        List<String> uniqueKeywords = keywords.stream()
                .distinct()
                .collect(Collectors.toList());
        
        // 키워드 저장
        int savedCount = 0;
        for (String keyword : uniqueKeywords) {
            if (saveKeyword(keyword)) {
                savedCount++;
            }
        }
        
        logger.info("Saved {} new keywords", savedCount);
        return savedCount;
    }
    
    /**
     * Google Trends API에서 인기 키워드 수집
     * @return 수집된 키워드 리스트
     */
    private List<String> collectGoogleTrendsKeywords() throws Exception {
        String url = googleTrendsApiUrl + 
                "?hl=" + LANGUAGE + 
                "&geo=" + GEO + 
                "&tz=-540" +  // GMT+9 (KST)
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
        // 네이버 API 자격 증명이 기본값인 경우 빈 목록 반환
        if ("NAVER_CLIENT_ID_PLACEHOLDER".equals(naverClientId) || 
            "NAVER_CLIENT_SECRET_PLACEHOLDER".equals(naverClientSecret)) {
            logger.warn("Naver API credentials not configured. Skipping Naver DataLab keyword collection.");
            return Collections.emptyList();
        }
        
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
     * 키워드를 저장하고 출처 정보 추가
     * @param term 키워드 텍스트
     * @return 저장 성공 여부
     */
    private boolean saveKeyword(String term) {
        // 이미 존재하는 키워드인지 확인
        Optional<Keyword> existingKeyword = keywordRepository.findByKeywordAndLanguage(term, LANGUAGE);
        if (existingKeyword.isPresent()) {
            // 이미 존재하는 경우 우선순위만 업데이트
            Keyword keyword = existingKeyword.get();
            keyword.setUpdatedAt(LocalDateTime.now());
            
            // 활성 상태로 변경 (만약 비활성화되어 있었다면)
            if (!keyword.isActive()) {
                keyword.setActive(true);
            }
            
            keywordRepository.save(keyword);
            return false;
        }
        
        // 새 키워드 생성
        try {
            Keyword keyword = new Keyword();
            keyword.setKeyword(term);
            keyword.setLanguage(LANGUAGE);
            keyword.setSource("api");
            keyword.setPriority(getKeywordPriority(term));
            keyword.setActive(true);
            keyword.setCreatedAt(LocalDateTime.now());
            keyword.setUpdatedAt(LocalDateTime.now());
            
            // 카테고리는 간단한 휴리스틱으로 설정 (실제로는 더 복잡한 로직이 필요)
            String category = "general";
            if (term.contains("코로나") || term.contains("백신") || term.contains("건강")) {
                category = "health";
            } else if (term.contains("주식") || term.contains("비트코인") || term.contains("투자")) {
                category = "finance";
            } else if (term.contains("영화") || term.contains("드라마") || term.contains("배우")) {
                category = "entertainment";
            } else if (term.contains("게임") || term.contains("출시")) {
                category = "game";
            }
            
            keyword.setCategory(category);
            
            Keyword savedKeyword = keywordRepository.save(keyword);
            
            // 키워드 출처 정보 저장
            KeywordSource source = new KeywordSource();
            source.setKeywordId(savedKeyword.getId());
            source.setSourceName("api-collection");
            source.setCollectionDate(LocalDateTime.now());
            keywordSourceRepository.save(source);
            
            return true;
        } catch (Exception e) {
            logger.error("Error saving keyword {}: {}", term, e.getMessage());
            return false;
        }
    }
    
    /**
     * 키워드 우선순위 계산 (길이, 특수 단어 등을 고려)
     * @param keyword 키워드 텍스트
     * @return 우선순위 값 (1-10)
     */
    private int getKeywordPriority(String keyword) {
        int priority = 5; // 기본 우선순위
        
        // 단어 길이에 따른 우선순위 조정
        if (keyword.length() < 3) {
            priority -= 2; // 너무 짧은 키워드는 우선순위 낮춤
        } else if (keyword.length() > 10) {
            priority += 1; // 긴 키워드는 조금 더 높은 우선순위
        }
        
        // 특정 단어 포함 여부에 따른 우선순위 조정
        List<String> highPriorityWords = Arrays.asList("신규", "출시", "화제", "인기", "논란", "이슈");
        
        for (String word : highPriorityWords) {
            if (keyword.contains(word)) {
                priority += 1;
                break;
            }
        }
        
        // 우선순위 범위 제한 (1-10)
        return Math.max(1, Math.min(10, priority));
    }
    
    /**
     * 키워드를 수동으로 추가
     * @param term 키워드 텍스트
     * @param priority 우선순위 (null이면 자동 계산)
     * @return 저장된 키워드 객체
     */
    public Keyword addKeywordManually(String term, Integer priority) {
        // 이미 존재하는 경우 해당 키워드 반환
        Optional<Keyword> existingKeyword = keywordRepository.findByKeywordAndLanguage(term, LANGUAGE);
        if (existingKeyword.isPresent()) {
            return existingKeyword.get();
        }
        
        Keyword keyword = new Keyword();
        keyword.setKeyword(term);
        keyword.setLanguage(LANGUAGE);
        keyword.setSource("manual");
        keyword.setPriority(priority != null ? priority : getKeywordPriority(term));
        keyword.setActive(true);
        keyword.setCreatedAt(LocalDateTime.now());
        keyword.setUpdatedAt(LocalDateTime.now());
        
        // 카테고리는 기본값으로 설정 (실제 운영 시에는 사용자가 지정)
        keyword.setCategory("general");
        
        Keyword savedKeyword = keywordRepository.save(keyword);
        
        // 키워드 출처 정보 저장
        KeywordSource source = new KeywordSource();
        source.setKeywordId(savedKeyword.getId());
        source.setSourceName("manual-addition");
        source.setCollectionDate(LocalDateTime.now());
        keywordSourceRepository.save(source);
        
        return savedKeyword;
    }
    
    /**
     * 활성 키워드 목록 조회
     * @param days 최근 몇일 이내의 키워드를 조회할지 (0이면 전체)
     * @return 키워드 목록
     */
    public List<Keyword> getActiveKeywords(int days) {
        if (days > 0) {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            return keywordRepository.findByLanguageAndActiveAndUpdatedAtAfter(LANGUAGE, true, cutoffDate);
        } else {
            return keywordRepository.findByLanguageAndActive(LANGUAGE, true);
        }
    }
} 