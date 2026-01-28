package com.eng.study.engstudy;

import com.eng.study.engstudy.controller.TestController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestController.class) // TestController만 로드하여 테스트
public class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate; // TestController의 의존성 Mocking

    @Test
    @DisplayName("헬스 체크 API 테스트")
    void healthCheckTest() throws Exception {
        mockMvc.perform(get("/api/test/health"))
                .andExpect(status().isOk()) // HTTP 200 OK
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Application is running!"))
                .andDo(print());
    }

    @Test
    @DisplayName("DB 연결 테스트")
    void dbConnectionTest() throws Exception {
        // JdbcTemplate.queryForList() 호출 시 반환할 Mock 데이터 설정
        List<Map<String, Object>> mockLessons = List.of(Map.of("id", 1, "title", "Lesson 1", "level", "Beginner"));
        List<Map<String, Object>> mockUsers = List.of(Map.of("id", 101, "username", "john_doe", "email", "john@example.com"));

        when(jdbcTemplate.queryForList(anyString()))
                .thenReturn(mockLessons) // 첫 번째 호출 (lessons)
                .thenReturn(mockUsers);  // 두 번째 호출 (users)

        mockMvc.perform(get("/api/test/db"))
                .andExpect(status().isOk()) // HTTP 200 OK
                .andExpect(jsonPath("$.status").value("Connected to Database"))
                .andExpect(jsonPath("$.lessons[0].id").value(1))
                .andExpect(jsonPath("$.users[0].id").value(101))
                .andDo(print());
    }
}
