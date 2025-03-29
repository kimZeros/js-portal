package com.jsportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsportal.domain.revenue.DailyRevenue;
import com.jsportal.domain.revenue.RevenuePlatform;
import com.jsportal.domain.revenue.RevenueSource;
import com.jsportal.repository.DailyRevenueRepository;
import com.jsportal.repository.RevenuePlatformRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 다양한 수익 소스(AdSense, Coupang Partners 등)로부터 수익을 분석하는 서비스
 */
@Service
public class RevenueAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(RevenueAnalysisService.class);
    
    private static final String ADSENSE_API_URL = "https://www.googleapis.com/adsense/v2";
    private static final String COUPANG_API_URL = "https://api-gateway.coupang.com/api/v1";
    
    @Value("${adsense.access_token}")
    private String adsenseAccessToken;
    
    @Value("${adsense.refresh_token}")
    private String adsenseRefreshToken;
    
    @Value("${adsense.client_id}")
    private String adsenseClientId;
    
    @Value("${adsense.client_secret}")
    private String adsenseClientSecret;
    
    @Value("${coupang.access_key}")
    private String coupangAccessKey;
    
    @Value("${coupang.secret_key}")
    private String coupangSecretKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DailyRevenueRepository dailyRevenueRepository;
    private final RevenuePlatformRepository revenuePlatformRepository;
    
    public RevenueAnalysisService(RestTemplate restTemplate,
                                 ObjectMapper objectMapper,
                                 DailyRevenueRepository dailyRevenueRepository,
                                 RevenuePlatformRepository revenuePlatformRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.dailyRevenueRepository = dailyRevenueRepository;
        this.revenuePlatformRepository = revenuePlatformRepository;
    }
    
    /**
     * 모든 플랫폼에서 일일 수익 데이터를 수집하는 스케줄링된 작업
     * 매일 오전 5시에 실행 (전날 데이터 수집)
     */
    @Scheduled(cron = "0 0 5 * * ?")
    public void collectDailyRevenueForAllPlatforms() {
        logger.info("Starting scheduled daily revenue collection for all platforms");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        try {
            collectAdSenseRevenue(yesterday, yesterday);
            logger.info("Collected AdSense revenue for {}", yesterday);
        } catch (Exception e) {
            logger.error("Error collecting AdSense revenue: {}", e.getMessage(), e);
        }
        
        try {
            collectCoupangPartnersRevenue(yesterday, yesterday);
            logger.info("Collected Coupang Partners revenue for {}", yesterday);
        } catch (Exception e) {
            logger.error("Error collecting Coupang Partners revenue: {}", e.getMessage(), e);
        }
        
        logger.info("Completed scheduled daily revenue collection for all platforms");
    }
    
    /**
     * Google AdSense 수익 데이터 수집
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 수집된 일일 수익 목록
     */
    public List<DailyRevenue> collectAdSenseRevenue(LocalDate startDate, LocalDate endDate) {
        try {
            // AdSense API 액세스 토큰 검증 및 갱신
            validateAndRefreshAdSenseToken();
            
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
            String startDateStr = startDate.format(formatter);
            String endDateStr = endDate.format(formatter);
            
            // AdSense API 호출
            String url = ADSENSE_API_URL + "/accounts/pub-XXXXXXXXXXXXXXXX/reports" +
                    "?dateRange=CUSTOM" +
                    "&startDate=" + startDateStr +
                    "&endDate=" + endDateStr +
                    "&metrics=ESTIMATED_EARNINGS,PAGE_VIEWS,CLICKS" +
                    "&dimensions=DATE";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + adsenseAccessToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode rowsNode = rootNode.path("rows");
                
                List<DailyRevenue> revenues = new ArrayList<>();
                
                if (rowsNode.isArray()) {
                    for (JsonNode rowNode : rowsNode) {
                        String date = rowNode.path("cells").path(0).path("value").asText(); // DATE
                        BigDecimal earnings = new BigDecimal(rowNode.path("cells").path(1).path("value").asText()); // ESTIMATED_EARNINGS
                        int pageViews = rowNode.path("cells").path(2).path("value").asInt(); // PAGE_VIEWS
                        int clicks = rowNode.path("cells").path(3).path("value").asInt(); // CLICKS
                        
                        LocalDate revenueDate = LocalDate.parse(date);
                        
                        // 기존 데이터가 있는지 확인
                        DailyRevenue existingRevenue = dailyRevenueRepository
                                .findByDateAndPlatform(revenueDate, "ADSENSE")
                                .orElse(new DailyRevenue());
                        
                        existingRevenue.setDate(revenueDate);
                        existingRevenue.setPlatform("ADSENSE");
                        existingRevenue.setAmount(earnings);
                        existingRevenue.setImpressions(pageViews);
                        existingRevenue.setClicks(clicks);
                        existingRevenue.setCurrency("KRW");
                        
                        Map<String, Object> extraData = new HashMap<>();
                        extraData.put("ctr", clicks > 0 && pageViews > 0 ? (double) clicks / pageViews : 0);
                        existingRevenue.setExtraData(objectMapper.writeValueAsString(extraData));
                        
                        dailyRevenueRepository.save(existingRevenue);
                        revenues.add(existingRevenue);
                    }
                }
                
                return revenues;
            } else {
                logger.error("Failed to get data from AdSense API: {}", response.getStatusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Error collecting AdSense revenue: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Coupang Partners 수익 데이터 수집
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 수집된 일일 수익 목록
     */
    public List<DailyRevenue> collectCoupangPartnersRevenue(LocalDate startDate, LocalDate endDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String startDateStr = startDate.format(formatter);
            String endDateStr = endDate.format(formatter);
            
            // Coupang Partners API 호출
            String url = COUPANG_API_URL + "/reports/daily" +
                    "?startDate=" + startDateStr +
                    "&endDate=" + endDateStr;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", generateCoupangHmacSignature(url));
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode dataNode = rootNode.path("data");
                
                List<DailyRevenue> revenues = new ArrayList<>();
                
                if (dataNode.isArray()) {
                    for (JsonNode dayNode : dataNode) {
                        String date = dayNode.path("date").asText();
                        BigDecimal earnings = new BigDecimal(dayNode.path("commission").asText());
                        int orders = dayNode.path("orderCount").asInt();
                        int clicks = dayNode.path("clickCount").asInt();
                        
                        LocalDate revenueDate = LocalDate.parse(date, formatter);
                        
                        // 기존 데이터가 있는지 확인
                        DailyRevenue existingRevenue = dailyRevenueRepository
                                .findByDateAndPlatform(revenueDate, "COUPANG")
                                .orElse(new DailyRevenue());
                        
                        existingRevenue.setDate(revenueDate);
                        existingRevenue.setPlatform("COUPANG");
                        existingRevenue.setAmount(earnings);
                        existingRevenue.setClicks(clicks);
                        existingRevenue.setCurrency("KRW");
                        
                        Map<String, Object> extraData = new HashMap<>();
                        extraData.put("orders", orders);
                        extraData.put("conversionRate", clicks > 0 ? (double) orders / clicks : 0);
                        existingRevenue.setExtraData(objectMapper.writeValueAsString(extraData));
                        
                        dailyRevenueRepository.save(existingRevenue);
                        revenues.add(existingRevenue);
                    }
                }
                
                return revenues;
            } else {
                logger.error("Failed to get data from Coupang Partners API: {}", response.getStatusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Error collecting Coupang Partners revenue: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 특정 기간의 모든 플랫폼 수익 데이터 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 수익 데이터 맵 (플랫폼별 구분)
     */
    public Map<String, List<DailyRevenue>> getRevenueByPlatform(LocalDate startDate, LocalDate endDate) {
        List<DailyRevenue> allRevenues = dailyRevenueRepository.findByDateBetween(startDate, endDate);
        
        Map<String, List<DailyRevenue>> revenueByPlatform = new HashMap<>();
        
        for (DailyRevenue revenue : allRevenues) {
            String platform = revenue.getPlatform();
            revenueByPlatform.computeIfAbsent(platform, k -> new ArrayList<>()).add(revenue);
        }
        
        return revenueByPlatform;
    }
    
    /**
     * 특정 기간의 총 수익 계산
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 총 수익
     */
    public BigDecimal calculateTotalRevenue(LocalDate startDate, LocalDate endDate) {
        return dailyRevenueRepository.sumAmountByDateBetween(startDate, endDate);
    }
    
    /**
     * 플랫폼별 총 수익 계산
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 플랫폼별 총 수익 맵
     */
    public Map<String, BigDecimal> calculateRevenueByPlatform(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = dailyRevenueRepository.sumAmountByPlatformAndDateBetween(startDate, endDate);
        
        Map<String, BigDecimal> revenueByPlatform = new HashMap<>();
        
        for (Object[] result : results) {
            String platform = (String) result[0];
            BigDecimal amount = (BigDecimal) result[1];
            revenueByPlatform.put(platform, amount);
        }
        
        return revenueByPlatform;
    }
    
    /**
     * 언어별 수익 분석
     * 각 언어(한국어, 영어, 일본어)별로 AdSense 수익을 분석
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 언어별 수익 맵
     */
    public Map<String, BigDecimal> analyzeRevenueByLanguage(LocalDate startDate, LocalDate endDate) {
        // API 응답에서 언어별 수익 정보가 제공된다고 가정
        // 실제로는 GA4 또는 다른 분석 도구와 연동하여 이 정보를 얻어야 할 수 있음
        
        Map<String, BigDecimal> revenueByLanguage = new HashMap<>();
        revenueByLanguage.put("ko", BigDecimal.ZERO);
        revenueByLanguage.put("en", BigDecimal.ZERO);
        revenueByLanguage.put("ja", BigDecimal.ZERO);
        
        try {
            // AdSense API로 언어별 데이터 요청 (차원에 LANGUAGE 추가)
            validateAndRefreshAdSenseToken();
            
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
            String startDateStr = startDate.format(formatter);
            String endDateStr = endDate.format(formatter);
            
            String url = ADSENSE_API_URL + "/accounts/pub-XXXXXXXXXXXXXXXX/reports" +
                    "?dateRange=CUSTOM" +
                    "&startDate=" + startDateStr +
                    "&endDate=" + endDateStr +
                    "&metrics=ESTIMATED_EARNINGS" +
                    "&dimensions=LANGUAGE";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + adsenseAccessToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode rowsNode = rootNode.path("rows");
                
                if (rowsNode.isArray()) {
                    for (JsonNode rowNode : rowsNode) {
                        String languageCode = rowNode.path("cells").path(0).path("value").asText(); // LANGUAGE
                        BigDecimal earnings = new BigDecimal(rowNode.path("cells").path(1).path("value").asText()); // ESTIMATED_EARNINGS
                        
                        // ISO 639-1 언어 코드를 우리 시스템의 코드(ko, en, ja)로 변환
                        String normalizedCode = normalizeLanguageCode(languageCode);
                        
                        if (revenueByLanguage.containsKey(normalizedCode)) {
                            revenueByLanguage.put(normalizedCode, 
                                    revenueByLanguage.get(normalizedCode).add(earnings));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error analyzing revenue by language: {}", e.getMessage(), e);
        }
        
        return revenueByLanguage;
    }
    
    /**
     * ISO 언어 코드를 우리 시스템 코드로 변환
     * @param isoCode ISO 언어 코드
     * @return 정규화된 언어 코드
     */
    private String normalizeLanguageCode(String isoCode) {
        if (isoCode == null) return "unknown";
        
        String lowerCode = isoCode.toLowerCase();
        
        if (lowerCode.startsWith("ko") || lowerCode.equals("kr")) {
            return "ko";
        } else if (lowerCode.startsWith("en")) {
            return "en";
        } else if (lowerCode.startsWith("ja") || lowerCode.equals("jp")) {
            return "ja";
        } else {
            return "other";
        }
    }
    
    /**
     * AdSense API 토큰 유효성 검사 및 갱신
     */
    private void validateAndRefreshAdSenseToken() {
        // 토큰 검증 로직
        // 만료되었다면 리프레시 토큰으로 갱신
    }
    
    /**
     * Coupang Partners API 호출을 위한 HMAC 서명 생성
     * @param url API URL
     * @return 인증 헤더 값
     */
    private String generateCoupangHmacSignature(String url) {
        // Coupang Partners에서 요구하는 서명 생성 로직
        return "HMAC " + coupangAccessKey + ":" + "signature";
    }
    
    /**
     * 수익 플랫폼 설정 조회
     * @param platformId 플랫폼 ID
     * @return 플랫폼 설정
     */
    public RevenuePlatform getPlatformSettings(Long platformId) {
        return revenuePlatformRepository.findById(platformId)
                .orElseThrow(() -> new IllegalArgumentException("Platform not found: " + platformId));
    }
    
    /**
     * 모든 수익 플랫폼 설정 조회
     * @return 모든 플랫폼 설정 목록
     */
    public List<RevenuePlatform> getAllPlatformSettings() {
        return revenuePlatformRepository.findAll();
    }
    
    /**
     * 수익 플랫폼 설정 저장/업데이트
     * @param platform 플랫폼 설정
     * @return 저장된 설정
     */
    public RevenuePlatform savePlatformSettings(RevenuePlatform platform) {
        return revenuePlatformRepository.save(platform);
    }
} 