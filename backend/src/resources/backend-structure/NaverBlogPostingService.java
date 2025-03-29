package com.jsportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsportal.domain.content.Content;
import com.jsportal.domain.social.SocialPostingHistory;
import com.jsportal.repository.SocialPostingHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 네이버 블로그에 자동으로 포스팅하는 서비스
 */
@Service
public class NaverBlogPostingService {

    private static final Logger logger = LoggerFactory.getLogger(NaverBlogPostingService.class);
    
    private static final String NAVER_API_URL = "https://openapi.naver.com/blog";
    private static final String NAVER_OAUTH_URL = "https://nid.naver.com/oauth2.0";
    
    @Value("${naver.client.id}")
    private String clientId;
    
    @Value("${naver.client.secret}")
    private String clientSecret;
    
    @Value("${naver.redirect_uri}")
    private String redirectUri;
    
    @Value("${naver.access_token}")
    private String accessToken;
    
    @Value("${naver.refresh_token}")
    private String refreshToken;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SocialPostingHistoryRepository socialPostingHistoryRepository;
    
    public NaverBlogPostingService(RestTemplate restTemplate, 
                                  ObjectMapper objectMapper,
                                  SocialPostingHistoryRepository socialPostingHistoryRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.socialPostingHistoryRepository = socialPostingHistoryRepository;
    }
    
    /**
     * 네이버 블로그에 콘텐츠 포스팅
     * @param content 포스팅할 콘텐츠
     * @return 성공 여부
     */
    public boolean postContentToNaverBlog(Content content) {
        try {
            logger.info("Posting content to Naver Blog: {}", content.getTitle());
            
            // 이미 포스팅된 콘텐츠인지 확인
            if (isAlreadyPosted(content.getId())) {
                logger.info("Content already posted to Naver Blog: {}", content.getId());
                return false;
            }
            
            // 액세스 토큰이 유효한지 확인하고 필요시 갱신
            validateAndRefreshAccessToken();
            
            // 포스팅할 HTML 콘텐츠 생성
            String htmlContent = generateHtmlContent(content);
            
            // 포스팅 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Bearer " + accessToken);
            
            // 포스팅 요청 파라미터 설정
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("title", content.getTitle());
            params.add("contents", htmlContent);
            params.add("categoryNo", getCategoryForLanguage(content.getLanguage()));
            params.add("visibility", "PUBLIC"); // 공개 설정
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            // 포스팅 API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(
                    NAVER_API_URL + "/writePost.json",
                    request,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseNode = objectMapper.readTree(response.getBody());
                String postId = responseNode.path("message").path("result").path("logNo").asText();
                String blogId = responseNode.path("message").path("result").path("blogId").asText();
                
                // 포스팅 성공 시 이력 저장
                saveSocialPostingHistory(content.getId(), postId, blogId);
                
                logger.info("Successfully posted to Naver Blog. Post ID: {}", postId);
                return true;
            } else {
                logger.error("Failed to post to Naver Blog. Response: {}", response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error posting to Naver Blog: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 콘텐츠의 언어에 따라 적절한 네이버 블로그 카테고리 번호 반환
     * @param language 콘텐츠 언어
     * @return 카테고리 번호
     */
    private String getCategoryForLanguage(String language) {
        // 실제 네이버 블로그에 설정된 카테고리 번호로 대체 필요
        Map<String, String> categoryMap = new HashMap<>();
        categoryMap.put("ko", "1");   // 한국어 콘텐츠용 카테고리
        categoryMap.put("en", "2");   // 영어 콘텐츠용 카테고리
        categoryMap.put("ja", "3");   // 일본어 콘텐츠용 카테고리
        
        return categoryMap.getOrDefault(language, "1");
    }
    
    /**
     * HTML 형식의 콘텐츠 생성
     * @param content 콘텐츠 객체
     * @return HTML 형식의 콘텐츠
     */
    private String generateHtmlContent(Content content) {
        StringBuilder htmlBuilder = new StringBuilder();
        
        // 콘텐츠 헤더
        htmlBuilder.append("<div class='js-portal-content'>");
        
        // 썸네일 이미지가 있는 경우 추가
        if (content.getThumbnailUrl() != null && !content.getThumbnailUrl().isEmpty()) {
            htmlBuilder.append("<div class='thumbnail'>");
            htmlBuilder.append("<img src='").append(content.getThumbnailUrl()).append("' alt='").append(content.getTitle()).append("' />");
            htmlBuilder.append("</div>");
        }
        
        // 본문 내용 추가 (마크다운 -> HTML 변환이 필요할 수 있음)
        htmlBuilder.append("<div class='content-body'>");
        htmlBuilder.append(convertToHtml(content.getContent()));
        htmlBuilder.append("</div>");
        
        // 태그 추가
        if (content.getTags() != null && !content.getTags().isEmpty()) {
            htmlBuilder.append("<div class='tags'>");
            for (String tag : content.getTags()) {
                htmlBuilder.append("<span class='tag'>#").append(tag).append("</span> ");
            }
            htmlBuilder.append("</div>");
        }
        
        // 출처 표시
        htmlBuilder.append("<div class='source'>");
        htmlBuilder.append("원본 출처: <a href='https://jsportal.io/content/").append(content.getSlug()).append("'>JS 포털</a>");
        htmlBuilder.append("</div>");
        
        htmlBuilder.append("</div>");
        
        return htmlBuilder.toString();
    }
    
    /**
     * 텍스트 콘텐츠를 HTML로 변환 (간단한 마크다운 지원)
     * @param text 변환할 텍스트
     * @return HTML 형식으로 변환된 텍스트
     */
    private String convertToHtml(String text) {
        if (text == null) {
            return "";
        }
        
        // 줄바꿈을 <br>로 변환
        String html = text.replace("\n", "<br>");
        
        // 제목 형식 변환
        html = html.replaceAll("(?m)^# (.+)$", "<h1>$1</h1>");
        html = html.replaceAll("(?m)^## (.+)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^### (.+)$", "<h3>$1</h3>");
        
        // 굵은 글씨 변환
        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        
        // 기울임 글씨 변환
        html = html.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        
        // 링크 변환
        html = html.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "<a href='$2'>$1</a>");
        
        return html;
    }
    
    /**
     * 액세스 토큰이 유효한지 확인하고 필요시 갱신
     */
    private void validateAndRefreshAccessToken() {
        try {
            // 토큰 유효성 확인 API 호출
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    NAVER_API_URL + "/getInfo.json",
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            // 응답이 성공적이면 토큰이 유효한 것
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("Naver access token is valid");
                return;
            }
        } catch (Exception e) {
            logger.info("Naver access token validation failed, refreshing token: {}", e.getMessage());
        }
        
        // 토큰 갱신
        try {
            String url = NAVER_OAUTH_URL + "/token" +
                    "?grant_type=refresh_token" +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&refresh_token=" + refreshToken;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode tokenNode = objectMapper.readTree(response.getBody());
                
                // 새 액세스 토큰 적용
                if (tokenNode.has("access_token")) {
                    accessToken = tokenNode.get("access_token").asText();
                    logger.info("Successfully refreshed Naver access token");
                    
                    // 필요시 새 리프레시 토큰도 저장
                    if (tokenNode.has("refresh_token")) {
                        refreshToken = tokenNode.get("refresh_token").asText();
                    }
                    
                    // 여기에서 토큰을 DB에 저장하거나 설정 파일에 업데이트하는 로직 추가 가능
                }
            } else {
                logger.error("Failed to refresh Naver token: {}", response.getBody());
            }
        } catch (Exception e) {
            logger.error("Error refreshing Naver token: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 콘텐츠가 이미 네이버 블로그에 포스팅되었는지 확인
     * @param contentId 확인할 콘텐츠 ID
     * @return 이미 포스팅되었으면 true, 아니면 false
     */
    private boolean isAlreadyPosted(Long contentId) {
        return socialPostingHistoryRepository.findByContentIdAndPlatform(contentId, "NAVER_BLOG").isPresent();
    }
    
    /**
     * 소셜 미디어 포스팅 이력 저장
     * @param contentId 포스팅한 콘텐츠 ID
     * @param postId 네이버 블로그에서의 포스트 ID
     * @param blogId 네이버 블로그 ID
     */
    private void saveSocialPostingHistory(Long contentId, String postId, String blogId) {
        SocialPostingHistory history = new SocialPostingHistory();
        history.setContentId(contentId);
        history.setPlatform("NAVER_BLOG");
        history.setPostId(postId);
        history.setPostUrl("https://blog.naver.com/" + blogId + "/" + postId);
        history.setPostedAt(LocalDateTime.now());
        
        socialPostingHistoryRepository.save(history);
    }
    
    /**
     * OAuth 인증 과정에서 사용할 인증 URL 생성
     * @return 네이버 로그인 인증 URL
     */
    public String getAuthorizationUrl() {
        return NAVER_OAUTH_URL + "/authorize" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&state=jsportal";
    }
    
    /**
     * 인증 코드로 액세스 토큰 요청
     * @param code 인증 코드
     * @param state 상태 값
     * @return 성공 여부
     */
    public boolean requestAccessToken(String code, String state) {
        try {
            String url = NAVER_OAUTH_URL + "/token" +
                    "?grant_type=authorization_code" +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&code=" + code +
                    "&state=" + state;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode tokenNode = objectMapper.readTree(response.getBody());
                
                accessToken = tokenNode.get("access_token").asText();
                refreshToken = tokenNode.get("refresh_token").asText();
                
                // 여기에서 토큰을 DB에 저장하거나 설정 파일에 업데이트하는 로직 추가 가능
                
                logger.info("Successfully obtained Naver access token");
                return true;
            } else {
                logger.error("Failed to obtain Naver access token: {}", response.getBody());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error obtaining Naver access token: {}", e.getMessage(), e);
            return false;
        }
    }
} 