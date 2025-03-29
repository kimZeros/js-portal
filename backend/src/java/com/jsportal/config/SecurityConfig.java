package com.jsportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 구성
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 보안 필터 체인 구성
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 보안 구성 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/**").permitAll()
            .and()
            .httpBasic()
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                
        return http.build();
    }
} 