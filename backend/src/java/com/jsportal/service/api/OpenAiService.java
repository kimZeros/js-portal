package com.jsportal.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsportal.domain.content.Content;
import com.jsportal.domain.keyword.Keyword;
import com.jsportal.repository.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI API를 사용하여 콘텐츠를 생성하는 서비스
 */
@Service
public class OpenAiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.model:gpt-3.5-turbo}")
    private String model;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    private final ContentRepository contentRepository;

    @Autowired
    public OpenAiService(RestTemplate restTemplate, ContentRepository contentRepository, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.contentRepository = contentRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 원본 콘텐츠를 기반으로 재미있는 콘텐츠 생성
     * @param originalContent 원본 콘텐츠
     * @param source 콘텐츠 출처
     * @param language 언어 코드
     * @return 생성된 콘텐츠
     */
    public Content generateFunContent(String originalContent, String source, String language) {
        logger.info("Generating fun content from source {} in language {}", source, language);
        
        try {
            String prompt = buildPrompt(originalContent, language);
            String generatedText = callOpenAiApi(prompt);
            
            if (generatedText == null || generatedText.isEmpty()) {
                logger.error("Failed to generate content, received empty response");
                return null;
            }
            
            // 콘텐츠 객체 생성
            Content content = new Content();
            content.setTitle(extractTitle(generatedText));
            content.setBody(extractBody(generatedText));
            content.setLanguage(language);
            content.setSource("OpenAI");
            content.setOriginalSource(source);
            content.setType("FUN");
            content.setStatus("PUBLISHED");
            content.setCreatedAt(LocalDateTime.now());
            
            logger.info("Successfully generated content: {}", content.getTitle());
            return content;
        } catch (Exception e) {
            logger.error("Error generating content: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 프롬프트 생성
     */
    private String buildPrompt(String originalContent, String language) {
        String promptTemplate = "";
        
        switch (language) {
            case "ko":
                promptTemplate = "다음 콘텐츠를 기반으로 재미있고 유머러스한 짧은 글을 작성해주세요. " +
                               "제목과 본문을 포함해야 합니다. 원본 콘텐츠보다 더 흥미롭게 만들어주세요: \n\n%s";
                break;
            case "en":
                promptTemplate = "Based on the following content, write a fun and humorous short article. " +
                               "Include a title and body. Make it more interesting than the original content: \n\n%s";
                break;
            case "ja":
                promptTemplate = "次のコンテンツに基づいて、面白くてユーモラスな短い記事を書いてください。 " +
                               "タイトルと本文を含めてください。元のコンテンツよりも興味深くしてください: \n\n%s";
                break;
            default:
                promptTemplate = "Based on the following content, write a fun and humorous short article. " +
                               "Include a title and body. Make it more interesting than the original content: \n\n%s";
        }
        
        return String.format(promptTemplate, originalContent);
    }
    
    /**
     * OpenAI API 호출
     */
    private String callOpenAiApi(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", new Object[]{message});
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 500);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode choicesNode = rootNode.path("choices");
                
                if (choicesNode.isArray() && choicesNode.size() > 0) {
                    return choicesNode.get(0).path("message").path("content").asText();
                }
            }
            
            logger.error("API call failed with status code: {}", response.getStatusCodeValue());
            return null;
        } catch (Exception e) {
            logger.error("Error calling OpenAI API: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 생성된 텍스트에서 제목 추출
     */
    private String extractTitle(String generatedText) {
        // 일반적으로 첫 번째 줄이 제목일 가능성이 높습니다.
        String[] lines = generatedText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                // # 또는 다른 마크다운 형식의 제목 표시 제거
                return line.replaceAll("^#+ ", "");
            }
        }
        
        // 제목을 찾지 못한 경우 기본값 반환
        return "Generated Content";
    }
    
    /**
     * 생성된 텍스트에서 본문 추출
     */
    private String extractBody(String generatedText) {
        String[] lines = generatedText.split("\n");
        boolean titleFound = false;
        StringBuilder body = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            
            if (!titleFound && !line.isEmpty()) {
                // 첫 번째 비어있지 않은 줄은 제목으로 간주
                titleFound = true;
                continue;
            }
            
            if (titleFound) {
                body.append(line).append("\n");
            }
        }
        
        return body.toString().trim();
    }

    /**
     * 키워드를 기반으로 콘텐츠 생성
     * @param keyword 키워드
     * @param category 카테고리
     * @param language 언어 코드
     * @return 생성된 콘텐츠
     */
    public Content generateKeywordContent(String keyword, String category, String language) {
        logger.info("Generating content for keyword '{}' in category '{}', language '{}'", keyword, category, language);
        
        try {
            String prompt = buildKeywordPrompt(keyword, category, language);
            String generatedText = callOpenAiApi(prompt);
            
            if (generatedText == null || generatedText.isEmpty()) {
                logger.error("Failed to generate content, received empty response");
                return null;
            }
            
            // 콘텐츠 객체 생성
            Content content = new Content();
            content.setTitle(extractTitle(generatedText));
            content.setBody(extractBody(generatedText));
            content.setLanguage(language);
            content.setSource("OpenAI");
            content.setType("KEYWORD");
            content.setCategory(category);
            content.setKeyword(keyword);
            content.setStatus("PUBLISHED");
            content.setCreatedAt(LocalDateTime.now());
            
            // 콘텐츠 저장
            Content savedContent = contentRepository.save(content);
            
            logger.info("Successfully generated and saved content: {}", content.getTitle());
            return savedContent;
        } catch (Exception e) {
            logger.error("Error generating content for keyword {}: {}", keyword, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 키워드 기반 프롬프트 생성
     */
    private String buildKeywordPrompt(String keyword, String category, String language) {
        String promptTemplate = "";
        
        if ("ko".equals(language)) {
            promptTemplate = "다음 키워드에 대한 유익하고 흥미로운 콘텐츠를 작성해주세요: '%s'\n" +
                           "카테고리: %s\n\n" +
                           "최소 500자 이상의 정보성 글을 작성해주세요. 제목은 SEO에 최적화된 형태로 작성하고, " +
                           "본문은 관련 정보와 유용한 팁을 포함해야 합니다. " +
                           "명확한 제목과 본문 구조를 갖추어 응답해주세요.";
        } else if ("en".equals(language)) {
            promptTemplate = "Write an informative and interesting article about the keyword: '%s'\n" +
                           "Category: %s\n\n" +
                           "The article should be at least 500 words, with an SEO-optimized title. " +
                           "Include relevant information and useful tips in the content. " +
                           "Please provide a clear title and structured content in your response.";
        } else {
            promptTemplate = "キーワード '%s' に関する有益で面白い記事を書いてください。\n" + 
                           "カテゴリ: %s\n\n" +
                           "少なくとも500文字の情報記事を書いてください。SEO最適化されたタイトルをつけ、" +
                           "関連情報と役立つヒントを内容に含めてください。" +
                           "明確なタイトルと構造化されたコンテンツを回答に含めてください。";
        }
        
        return String.format(promptTemplate, keyword, category);
    }
} 