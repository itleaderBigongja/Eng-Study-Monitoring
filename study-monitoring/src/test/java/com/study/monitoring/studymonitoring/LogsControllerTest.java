package com.study.monitoring.studymonitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.monitoring.studymonitoring.controller.LogsController;
import com.study.monitoring.studymonitoring.converter.LogsConverter;
import com.study.monitoring.studymonitoring.model.dto.request.LogSearchRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.LogSearchResponseDTO;
import com.study.monitoring.studymonitoring.service.ElasticsearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LogsController.class)
public class LogsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ElasticsearchService elasticsearchService;

    @MockitoBean
    private LogsConverter logsConverter;

    @Autowired
    private WebApplicationContext ctx;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(print())
                .build();
    }

    private LogSearchResponseDTO createLogSearchResponse() {
        LogSearchResponseDTO.LogEntry entry = new LogSearchResponseDTO.LogEntry(
                "id1", "app-logs-2026", "2026-01-27T10:00:00", "INFO", "Logger", "Message", "App", null);
        return LogSearchResponseDTO.createWithPaging(1L, Collections.singletonList(entry), 0, 10);
    }

    @Test
    @DisplayName("로그 검색 API 테스트")
    void searchLogs() throws Exception {
        Map<String, Object> esData = Map.of("total", 1L, "logs", Collections.singletonList(Map.of("message", "test")));
        LogSearchResponseDTO mockResponse = createLogSearchResponse();

        when(elasticsearchService.searchLogs(
                anyString(), anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class), anyInt(), anyInt()))
                .thenReturn(esData);
        when(logsConverter.toSearchDTO(any(), anyInt(), anyInt())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/logs/search")
                        .param("index", "application-logs-*")
                        .param("keyword", "error")
                        .param("logLevel", "ERROR")
                        .param("startDate", "2026-01-01T00:00:00")
                        .param("endDate", "2026-01-31T23:59:59")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1L));

        verify(elasticsearchService).searchLogs(
                anyString(), anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class), anyInt(), anyInt());
        verify(logsConverter).toSearchDTO(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("최근 에러 로그 조회 API 테스트")
    void getRecentErrors() throws Exception {
        List<Map<String, Object>> mockErrors = Collections.singletonList(Map.of("message", "NPE", "log_level", "ERROR"));
        when(elasticsearchService.getRecentErrors(anyInt())).thenReturn(mockErrors);

        mockMvc.perform(get("/api/logs/errors").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1L));

        verify(elasticsearchService).getRecentErrors(1);
    }

    @Test
    @DisplayName("로그 통계 조회 API 테스트")
    void getLogStats() throws Exception {
        Map<String, Long> mockStats = Map.of("ERROR", 100L, "INFO", 500L);
        when(elasticsearchService.countByLogLevel(anyString())).thenReturn(mockStats);

        mockMvc.perform(get("/api/logs/stats").param("index", "application-logs-*"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stats.ERROR").value(100L));

        verify(elasticsearchService).countByLogLevel("application-logs-*");
    }

    @Test
    @DisplayName("사용 가능한 인덱스 목록 조회 API 테스트")
    void getAvailableIndices() throws Exception {
        mockMvc.perform(get("/api/logs/indices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(7)); // 7개 인덱스
    }
}
