package com.jsportal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 서버 상태 확인을 위한 컨트롤러
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * 서버 상태 확인 엔드포인트
     * @return 서버 상태 정보
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "JS Portal API is running");
        return ResponseEntity.ok(response);
    }
} 