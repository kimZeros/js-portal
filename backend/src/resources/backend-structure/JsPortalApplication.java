package com.jsportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * JS Portal 애플리케이션의 메인 클래스
 * 다중 언어 콘텐츠를 자동으로 생성하고 관리하는 시스템
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
public class JsPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(JsPortalApplication.class, args);
    }
    
    /**
     * REST API 호출을 위한 RestTemplate 빈 설정
     * @param builder RestTemplateBuilder
     * @return RestTemplate 인스턴스
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(10000))
                .setReadTimeout(Duration.ofMillis(30000))
                .build();
    }
    
    /**
     * ObjectMapper 설정은 Spring Boot의 자동 구성으로 처리됨
     * 필요시 커스텀 설정을 위해 추가 가능
     */
} 