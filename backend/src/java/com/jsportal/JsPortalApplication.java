package com.jsportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * JS Portal 애플리케이션의 메인 클래스
 */
@SpringBootApplication(exclude = {
    FlywayAutoConfiguration.class
})
@EnableScheduling
public class JsPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(JsPortalApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 