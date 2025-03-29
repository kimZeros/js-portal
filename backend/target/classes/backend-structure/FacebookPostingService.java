package com.jsportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsportal.domain.content.Content;
import com.jsportal.domain.social.SocialPostingHistory;
import com.jsportal.repository.SocialPostingHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 페이스북 페이지에 자동으로 포스팅하는 서비스
 */
@Service
public class FacebookPostingService {

    private static final Logger logger = LoggerFactory.getLogger(FacebookPostingService.class);
    private static final String GRAPH_API_URL = "https://graph.facebook.com/v17.0";
    
    @Value("${facebook.app.id}")
    private String appId;
    
    @Value("${facebook.app.secret}")
    private String appSecret;
    
    @Value("${facebook.page.id}")
    private String pageId;
    
    @Value("${facebook.page.access_token}")
    private String pageAccessToken;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SocialPostingHistoryRepository socialPostingHistoryRepository;
    
    public FacebookPostingService(RestTemplate restTemplate, 
                                  ObjectMapper objectMapper,
                                  SocialPostingHistoryRepository socialPostingHistoryRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.socialPostingHistoryRepository = socialPostingHistoryRepository;
    }
    
    /**
     * 페이스북 페이지에 텍스트와 링크 게시물 포스팅
     * @param content 포스팅할 콘텐츠
     * @param message 포스팅할 메시지
     * @param link 포스팅할 링크 (사이트 URL)
     * @return 성공 여부
     */
    public boolean postContentToFacebook(Content content, String message, String link) {
        try {
            logger.info("Posting content to Facebook: {}", content.getTitle());
            
            // 이미 포스팅된 콘텐츠인지 확인
            if (isAlreadyPosted(content.getId())) {
                logger.info("Content already posted to Facebook: {}", content.getId());
                return false;
            }
            
            // 포스팅 요청 데이터 준비
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("message", message);
            if (link != null && !link.isEmpty()) {
                params.add("link", link);
            }
            params.add("access_token", pageAccessToken);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            // 포스팅 API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GRAPH_API_URL + "/" + pageId + "/feed",
                    request,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseNode = objectMapper.readTree(response.getBody());
                String postId = responseNode.get("id").asText();
                
                // 포스팅 이력 저장
                saveSocialPostingHistory(content.getId(), postId, "FACEBOOK");
                
                logger.info("Successfully posted to Facebook. Post ID: {}", postId);
                return true;
            } else {
                logger.error("Failed to post to Facebook. Response: {}", response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error posting to Facebook: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 페이스북 페이지에 이미지와 함께 포스팅
     * @param content 포스팅할 콘텐츠
     * @param message 포스팅할 메시지
     * @param imageUrl 포스팅할 이미지 URL
     * @param link 포스팅할 링크 (사이트 URL)
     * @return 성공 여부
     */
    public boolean postImageToFacebook(Content content, String message, String imageUrl, String link) {
        try {
            logger.info("Posting image to Facebook: {}", content.getTitle());
            
            // 이미 포스팅된 콘텐츠인지 확인
            if (isAlreadyPosted(content.getId())) {
                logger.info("Content already posted to Facebook: {}", content.getId());
                return false;
            }
            
            // 1단계: 이미지 업로드
            String photoId = uploadPhotoToFacebook(imageUrl, message);
            if (photoId == null) {
                logger.error("Failed to upload photo to Facebook");
                return false;
            }
            
            // 2단계: 이미지와 함께 포스팅
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("message", message);
            if (link != null && !link.isEmpty()) {
                params.add("link", link);
            }
            params.add("attached_media[0]", "{\"media_fbid\":\"" + photoId + "\"}");
            params.add("access_token", pageAccessToken);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GRAPH_API_URL + "/" + pageId + "/feed",
                    request,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseNode = objectMapper.readTree(response.getBody());
                String postId = responseNode.get("id").asText();
                
                // 포스팅 이력 저장
                saveSocialPostingHistory(content.getId(), postId, "FACEBOOK");
                
                logger.info("Successfully posted image to Facebook. Post ID: {}", postId);
                return true;
            } else {
                logger.error("Failed to post to Facebook. Response: {}", response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error posting image to Facebook: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 페이스북 페이지에 비디오 업로드 및 포스팅
     * @param content 포스팅할 콘텐츠
     * @param message 포스팅할 메시지
     * @param videoUrl 포스팅할 비디오 URL
     * @param title 비디오 제목
     * @return 성공 여부
     */
    public boolean postVideoToFacebook(Content content, String message, String videoUrl, String title) {
        try {
            logger.info("Posting video to Facebook: {}", title);
            
            // 이미 포스팅된 콘텐츠인지 확인
            if (isAlreadyPosted(content.getId())) {
                logger.info("Content already posted to Facebook: {}", content.getId());
                return false;
            }
            
            // 비디오 포스팅 요청 데이터 준비
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("file_url", videoUrl);
            params.add("title", title);
            params.add("description", message);
            params.add("access_token", pageAccessToken);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            // 비디오 업로드 API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GRAPH_API_URL + "/" + pageId + "/videos",
                    request,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseNode = objectMapper.readTree(response.getBody());
                String postId = responseNode.get("id").asText();
                
                // 포스팅 이력 저장
                saveSocialPostingHistory(content.getId(), postId, "FACEBOOK");
                
                logger.info("Successfully posted video to Facebook. Video ID: {}", postId);
                return true;
            } else {
                logger.error("Failed to post video to Facebook. Response: {}", response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error posting video to Facebook: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 이미지를 페이스북에 업로드
     * @param imageUrl 업로드할 이미지 URL
     * @param caption 이미지 캡션
     * @return 업로드된 이미지의 ID
     */
    private String uploadPhotoToFacebook(String imageUrl, String caption) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("url", imageUrl);
            params.add("caption", caption);
            params.add("published", "false"); // 바로 게시하지 않음
            params.add("access_token", pageAccessToken);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GRAPH_API_URL + "/" + pageId + "/photos",
                    request,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseNode = objectMapper.readTree(response.getBody());
                return responseNode.get("id").asText();
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error uploading photo to Facebook: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 콘텐츠가 이미 페이스북에 포스팅되었는지 확인
     * @param contentId 확인할 콘텐츠 ID
     * @return 이미 포스팅되었으면 true, 아니면 false
     */
    private boolean isAlreadyPosted(Long contentId) {
        return socialPostingHistoryRepository.findByContentIdAndPlatform(contentId, "FACEBOOK").isPresent();
    }
    
    /**
     * 소셜 미디어 포스팅 이력 저장
     * @param contentId 포스팅한 콘텐츠 ID
     * @param postId 소셜 미디어 플랫폼에서의 포스트 ID
     * @param platform 플랫폼 이름 (예: "FACEBOOK")
     */
    private void saveSocialPostingHistory(Long contentId, String postId, String platform) {
        SocialPostingHistory history = new SocialPostingHistory();
        history.setContentId(contentId);
        history.setPlatform(platform);
        history.setPostId(postId);
        history.setPostUrl("https://facebook.com/" + postId);
        history.setPostedAt(LocalDateTime.now());
        
        socialPostingHistoryRepository.save(history);
    }
    
    /**
     * 페이스북 액세스 토큰 갱신
     * @return 갱신 성공 여부
     */
    public boolean refreshPageAccessToken() {
        try {
            // 장기 액세스 토큰 획득 요청
            String url = String.format(
                "%s/oauth/access_token?grant_type=fb_exchange_token&client_id=%s&client_secret=%s&fb_exchange_token=%s",
                GRAPH_API_URL, appId, appSecret, pageAccessToken
            );
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseNode = objectMapper.readTree(response.getBody());
                String newToken = responseNode.get("access_token").asText();
                
                // 여기에서 DB에 새 토큰 저장 로직을 추가할 수 있음
                // 또는 Spring Cloud Config 또는 환경 변수 업데이트 등
                
                logger.info("Successfully refreshed Facebook access token");
                return true;
            } else {
                logger.error("Failed to refresh Facebook access token. Response: {}", response.getBody());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error refreshing Facebook access token: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 콘텐츠로 자동 페이스북 포스팅 메시지 생성
     * @param content 콘텐츠
     * @param siteUrl 사이트 URL
     * @return 생성된 메시지
     */
    public String generatePostMessage(Content content, String siteUrl) {
        StringBuilder message = new StringBuilder();
        
        // 언어별 메시지 포맷 설정
        switch (content.getLanguage()) {
            case "ko":
                message.append("🔥 ").append(content.getTitle()).append("\n\n");
                
                // 내용 요약 (첫 100자 정도만)
                String summary = content.getContent();
                if (summary.length() > 100) {
                    summary = summary.substring(0, 97) + "...";
                }
                message.append(summary).append("\n\n");
                
                // 해시태그 추가
                if (content.getTags() != null && !content.getTags().isEmpty()) {
                    content.getTags().forEach(tag -> message.append("#").append(tag).append(" "));
                    message.append("\n\n");
                }
                
                // URL 추가
                message.append("👉 더 보기: ").append(siteUrl).append("/ko/content/").append(content.getSlug());
                break;
                
            case "en":
                message.append("🔥 ").append(content.getTitle()).append("\n\n");
                
                String enSummary = content.getContent();
                if (enSummary.length() > 100) {
                    enSummary = enSummary.substring(0, 97) + "...";
                }
                message.append(enSummary).append("\n\n");
                
                if (content.getTags() != null && !content.getTags().isEmpty()) {
                    content.getTags().forEach(tag -> message.append("#").append(tag).append(" "));
                    message.append("\n\n");
                }
                
                message.append("👉 Read more: ").append(siteUrl).append("/en/content/").append(content.getSlug());
                break;
                
            case "ja":
                message.append("🔥 ").append(content.getTitle()).append("\n\n");
                
                String jaSummary = content.getContent();
                if (jaSummary.length() > 100) {
                    jaSummary = jaSummary.substring(0, 97) + "...";
                }
                message.append(jaSummary).append("\n\n");
                
                if (content.getTags() != null && !content.getTags().isEmpty()) {
                    content.getTags().forEach(tag -> message.append("#").append(tag).append(" "));
                    message.append("\n\n");
                }
                
                message.append("👉 続きを読む: ").append(siteUrl).append("/ja/content/").append(content.getSlug());
                break;
                
            default:
                message.append(content.getTitle()).append("\n\n");
                message.append(siteUrl).append("/content/").append(content.getSlug());
        }
        
        return message.toString();
    }
} 