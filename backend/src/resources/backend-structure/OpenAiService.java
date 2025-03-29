package com.jsportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsportal.domain.content.Content;
import com.jsportal.domain.keyword.Keyword;
import com.jsportal.repository.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * OpenAI API와 연동하여 키워드 기반 콘텐츠를 자동 생성하는 서비스
 */
@Service
public class OpenAiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model:gpt-4-turbo}")
    private String defaultModel;

    private final RestTemplate restTemplate;
    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper;

    public OpenAiService(RestTemplate restTemplate, ContentRepository contentRepository, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.contentRepository = contentRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 키워드 기반으로 정보성 콘텐츠 생성
     * @param keyword 키워드 객체
     * @param language 언어 코드 (ko, en, ja)
     * @return 생성된 Content 객체
     */
    public Content generateInfoContent(Keyword keyword, String language) {
        try {
            String prompt = buildInfoPrompt(keyword, language);
            String content = callOpenAiApi(prompt);
            
            JsonNode parsed = parseGeneratedContent(content);
            
            Content newContent = new Content();
            newContent.setTitle(parsed.get("title").asText());
            newContent.setSlug(generateSlug(parsed.get("title").asText()));
            newContent.setExcerpt(parsed.get("excerpt").asText());
            newContent.setContent(parsed.get("content").asText());
            newContent.setType("info");
            newContent.setStatus("draft");
            newContent.setAuthor("AI Writer");
            newContent.setCreatedAt(LocalDateTime.now());
            newContent.setUpdatedAt(LocalDateTime.now());
            
            return contentRepository.save(newContent);
        } catch (Exception e) {
            logger.error("Error generating info content for keyword: {}", keyword.getKeyword(), e);
            throw new RuntimeException("Error generating content", e);
        }
    }

    /**
     * 커뮤니티 글 기반으로 재미/이슈 콘텐츠 생성
     * @param sourceText 원본 커뮤니티 글
     * @param sourceName 출처 (예: 루리웹, 5ch)
     * @param language 언어 코드 (ko, en, ja)
     * @return 생성된 Content 객체
     */
    public Content generateFunContent(String sourceText, String sourceName, String language) {
        try {
            String prompt = buildFunPrompt(sourceText, sourceName, language);
            String content = callOpenAiApi(prompt);
            
            JsonNode parsed = parseGeneratedContent(content);
            
            Content newContent = new Content();
            newContent.setTitle(parsed.get("title").asText());
            newContent.setSlug(generateSlug(parsed.get("title").asText()));
            newContent.setExcerpt(parsed.get("excerpt").asText());
            newContent.setContent(parsed.get("content").asText());
            newContent.setType("fun");
            newContent.setStatus("draft");
            newContent.setAuthor("AI Writer");
            newContent.setCreatedAt(LocalDateTime.now());
            newContent.setUpdatedAt(LocalDateTime.now());
            
            return contentRepository.save(newContent);
        } catch (Exception e) {
            logger.error("Error generating fun content from source: {}", sourceName, e);
            throw new RuntimeException("Error generating content", e);
        }
    }

    /**
     * 정보성 콘텐츠 생성을 위한 프롬프트 작성
     */
    private String buildInfoPrompt(Keyword keyword, String language) {
        Map<String, String> promptTemplates = new HashMap<>();
        
        // 한국어 프롬프트
        promptTemplates.put("ko", 
            "다음 키워드에 대한 SEO 최적화된 정보성 글을 작성해주세요: %s\n\n" +
            "글은 다음 JSON 형식으로 반환해주세요:\n" +
            "{\n" +
            "  \"title\": \"SEO 최적화된 제목 (키워드 포함)\",\n" +
            "  \"excerpt\": \"약 100자 이내의 글 요약. 핵심 정보와 키워드 포함\",\n" +
            "  \"content\": \"HTML 형식의 본문 콘텐츠. h2, h3 태그로 구조화하고, p 태그로 문단 작성. 최소 1000단어 분량\"\n" +
            "}\n\n" +
            "중요 지침:\n" +
            "1. 제목과 본문에 반드시 키워드를 자연스럽게 포함시켜주세요.\n" +
            "2. 본문은 h2, h3 태그로 구조화하고, 목차 구성이 SEO에 유리하게 작성해주세요.\n" +
            "3. 사실에 근거한 유용한 정보를 제공해주세요.\n" +
            "4. 키워드 카테고리는 '%s'입니다. 이 주제에 맞게 작성해주세요."
        );
        
        // 영어 프롬프트
        promptTemplates.put("en", 
            "Write an SEO-optimized informative article about the following keyword: %s\n\n" +
            "Please format your response as JSON:\n" +
            "{\n" +
            "  \"title\": \"SEO-optimized title (including the keyword)\",\n" +
            "  \"excerpt\": \"A summary of about 100 characters. Include key information and the keyword\",\n" +
            "  \"content\": \"HTML-formatted content. Structure with h2, h3 tags, and write paragraphs with p tags. Minimum 1000 words\"\n" +
            "}\n\n" +
            "Important instructions:\n" +
            "1. Include the keyword naturally in the title and content.\n" +
            "2. Structure the content with h2, h3 tags, and create a table of contents that is SEO-friendly.\n" +
            "3. Provide useful information based on facts.\n" +
            "4. The keyword category is '%s'. Please write according to this topic."
        );
        
        // 일본어 프롬프트
        promptTemplates.put("ja", 
            "次のキーワードについてSEO最適化された情報記事を書いてください: %s\n\n" +
            "以下のJSON形式で返してください:\n" +
            "{\n" +
            "  \"title\": \"SEO最適化されたタイトル（キーワードを含む）\",\n" +
            "  \"excerpt\": \"約100文字程度の記事要約。主要情報とキーワードを含む\",\n" +
            "  \"content\": \"HTML形式の本文コンテンツ。h2、h3タグで構造化し、p タグで段落を作成。最低1000単語以上\"\n" +
            "}\n\n" +
            "重要な指示:\n" +
            "1. タイトルと本文に自然にキーワードを含めてください。\n" +
            "2. 本文はh2、h3タグで構造化し、SEOに有利な目次構成にしてください。\n" +
            "3. 事実に基づいた有用な情報を提供してください。\n" +
            "4. キーワードカテゴリは '%s' です。このテーマに合わせて作成してください。"
        );
        
        String template = promptTemplates.getOrDefault(language, promptTemplates.get("en"));
        return String.format(template, keyword.getKeyword(), keyword.getCategory());
    }

    /**
     * 재미/이슈 콘텐츠 생성을 위한 프롬프트 작성
     */
    private String buildFunPrompt(String sourceText, String sourceName, String language) {
        Map<String, String> promptTemplates = new HashMap<>();
        
        // 한국어 프롬프트
        promptTemplates.put("ko", 
            "다음은 '%s'에서 가져온 커뮤니티 글입니다:\n\n%s\n\n" +
            "이 글을 바탕으로 독자들이 흥미를 가질만한 재미있는 글로 재작성해주세요. 다음 JSON 형식으로 반환해주세요:\n" +
            "{\n" +
            "  \"title\": \"관심을 끌 수 있는 흥미로운 제목\",\n" +
            "  \"excerpt\": \"약 100자 이내의 글 요약. 가장 재미있는 포인트를 포함\",\n" +
            "  \"content\": \"HTML 형식의 본문 콘텐츠. 원본 내용을 재구성하고 추가적인 맥락이나 의견 포함\"\n" +
            "}\n\n" +
            "중요 지침:\n" +
            "1. 원본 글의 핵심 내용은 유지하되, 저작권 침해를 피하기 위해 문장 구조와 표현을 완전히 바꿔주세요.\n" +
            "2. 유머, 놀라움, 흥미를 유발하는 요소를 강조해주세요.\n" +
            "3. 제목은 클릭을 유도할 수 있도록 매력적으로 작성해주세요.\n" +
            "4. 내용이 너무 길어지지 않도록 적절히 요약해주세요."
        );
        
        // 영어 프롬프트
        promptTemplates.put("en", 
            "Here is a community post from '%s':\n\n%s\n\n" +
            "Please rewrite this into an engaging and entertaining article that readers will find interesting. Format your response as JSON:\n" +
            "{\n" +
            "  \"title\": \"An attention-grabbing, interesting title\",\n" +
            "  \"excerpt\": \"A summary of about 100 characters. Include the most entertaining points\",\n" +
            "  \"content\": \"HTML-formatted content. Restructure the original content and include additional context or opinions\"\n" +
            "}\n\n" +
            "Important instructions:\n" +
            "1. Maintain the core content of the original post, but completely change the sentence structure and expressions to avoid copyright infringement.\n" +
            "2. Emphasize elements that evoke humor, surprise, or interest.\n" +
            "3. Write the title to be attractive and encourage clicks.\n" +
            "4. Summarize appropriately so the content doesn't get too long."
        );
        
        // 일본어 프롬프트
        promptTemplates.put("ja", 
            "これは'%s'からのコミュニティ投稿です:\n\n%s\n\n" +
            "これを読者が興味を持つような面白い記事に書き直してください。以下のJSON形式で返してください:\n" +
            "{\n" +
            "  \"title\": \"興味を引く魅力的なタイトル\",\n" +
            "  \"excerpt\": \"約100文字程度の記事要約。最も面白いポイントを含める\",\n" +
            "  \"content\": \"HTML形式の本文コンテンツ。元の内容を再構成し、追加の背景や意見を含める\"\n" +
            "}\n\n" +
            "重要な指示:\n" +
            "1. 元の投稿の核心内容は維持しつつも、著作権侵害を避けるために文章構造と表現を完全に変えてください。\n" +
            "2. ユーモア、驚き、興味を引き起こす要素を強調してください。\n" +
            "3. クリックを促すような魅力的なタイトルを作成してください。\n" +
            "4. 内容が長くなりすぎないように適切に要約してください。"
        );
        
        String template = promptTemplates.getOrDefault(language, promptTemplates.get("en"));
        return String.format(template, sourceName, sourceText);
    }
    
    /**
     * OpenAI API 호출
     */
    private String callOpenAiApi(String prompt) {
        String apiUrl = "https://api.openai.com/v1/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", defaultModel);
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        
        requestBody.put("messages", new Object[]{message});
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 3000);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
        
        try {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return jsonResponse.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            logger.error("Error parsing OpenAI response", e);
            throw new RuntimeException("Error parsing OpenAI response", e);
        }
    }
    
    /**
     * GPT가 생성한 JSON 문자열을 파싱
     */
    private JsonNode parseGeneratedContent(String content) {
        try {
            // JSON 문자열 추출 (만약 마크다운 코드 블록 안에 있을 경우)
            if (content.contains("```json")) {
                content = content.substring(content.indexOf("```json") + 7);
                content = content.substring(0, content.indexOf("```"));
            } else if (content.contains("```")) {
                content = content.substring(content.indexOf("```") + 3);
                content = content.substring(0, content.indexOf("```"));
            }
            
            return objectMapper.readTree(content.trim());
        } catch (Exception e) {
            logger.error("Error parsing generated content JSON", e);
            throw new RuntimeException("Error parsing generated content", e);
        }
    }
    
    /**
     * 제목에서 URL 슬러그 생성
     */
    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // 영문자, 숫자, 공백, 하이픈만 남김
                .replaceAll("\\s+", "-")         // 공백을 하이픈으로 변경
                .replaceAll("-+", "-")           // 여러 개의 하이픈을 하나로 줄임
                .trim();
    }
} 