package com.study.monitoring.studymonitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.monitoring.studymonitoring.controller.HealthCheckController;
import com.study.monitoring.studymonitoring.service.HealthCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthCheckController.class)
public class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthCheckService healthCheckService;

    @Autowired
    private WebApplicationContext ctx;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(print())
                .build();
    }

    @Test
    @DisplayName("전체 헬스체크 API 테스트")
    void healthCheck() throws Exception {
        Map<String, Object> mockResponse = Map.of(
                "status", "UP",
                "database", Map.of("status", "UP"),
                "elasticsearch", Map.of("status", "UP"),
                "prometheus", Map.of("status", "UP")
        );
        when(healthCheckService.getOverallHealth()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database.status").value("UP"));

        verify(healthCheckService).getOverallHealth();
    }

    @Test
    @DisplayName("Elasticsearch 헬스체크 API 테스트")
    void checkElasticsearchEndpoint() throws Exception {
        Map<String, Object> mockResponse = Map.of("status", "UP", "cluster_name", "monitoring-cluster");
        when(healthCheckService.getElasticsearchHealth()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/health/elasticsearch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.cluster_name").value("monitoring-cluster"));

        verify(healthCheckService).getElasticsearchHealth();
    }

    @Test
    @DisplayName("Database 헬스체크 API 테스트")
    void checkDatabaseEndpoint() throws Exception {
        Map<String, Object> mockResponse = Map.of("status", "UP", "database", "PostgreSQL");
        when(healthCheckService.getDatabaseHealth()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/health/database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("PostgreSQL"));

        verify(healthCheckService).getDatabaseHealth();
    }

    @Test
    @DisplayName("Prometheus 헬스체크 API 테스트")
    void checkPrometheusEndpoint() throws Exception {
        Map<String, Object> mockResponse = Map.of("status", "UP", "url", "http://localhost:9090");
        when(healthCheckService.getPrometheusHealth()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/health/prometheus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.url").value("http://localhost:9090"));

        verify(healthCheckService).getPrometheusHealth();
    }
}
