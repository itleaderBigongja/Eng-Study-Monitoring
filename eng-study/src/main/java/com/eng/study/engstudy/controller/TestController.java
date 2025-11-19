package com.eng.study.engstudy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "OK",
                "message", "Application is running!"
        );
    }

    @GetMapping("/db")
    public Map<String, Object> testDb() {
        // DB 연결 테스트
        List<Map<String, Object>> lessons = jdbcTemplate.queryForList(
                "SELECT id, title, level FROM lessons LIMIT 5"
        );

        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, username, email FROM users LIMIT 5"
        );

        return Map.of(
                "status", "Connected to Database",
                "lessons", lessons,
                "users", users
        );
    }
}
