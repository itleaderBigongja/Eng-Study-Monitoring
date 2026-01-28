package com.study.monitoring.studymonitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.monitoring.studymonitoring.controller.MetricsController;
import com.study.monitoring.studymonitoring.model.dto.request.PrometheusQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.MetricsResponseDTO;
import com.study.monitoring.studymonitoring.service.MetricsService;
import com.study.monitoring.studymonitoring.service.PrometheusService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetricsController.class)
public class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MetricsService metricsService;

    @MockitoBean
    private PrometheusService prometheusService;

    @Autowired
    private WebApplicationContext ctx;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(print())
                .build();
    }

    private Map<String, Object> createCurrentMetricsResponse() {
        return Map.of(
                "application", "eng-study",
                "metrics", Map.of("tps", 100.0, "heapUsage", 50.0)
        );
    }

    private List<Map<String, Object>> createRangeQueryData() {
        return Arrays.asList(
                Map.of("metric", Map.of("application", "eng-study"),
                        "values", Arrays.asList(
                                Arrays.asList(1678886400L, "10.5"),
                                Arrays.asList(1678886460L, "12.3")
                        ))
        );
    }

    @Test
    @DisplayName("현재 메트릭 조회 API 테스트")
    void getCurrentMetrics() throws Exception {
        Map<String, Object> mockResponse = createCurrentMetricsResponse();
        when(metricsService.getCurrentMetrics(anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/metrics/current").param("application", "eng-study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.application").value("eng-study"));

        verify(metricsService).getCurrentMetrics("eng-study");
    }

    @Test
    @DisplayName("시간 범위 메트릭 조회 (Range Query) API 테스트")
    void executeRangeQuery() throws Exception {
        PrometheusQueryRequestDTO request = new PrometheusQueryRequestDTO();
        request.setQuery("rate(http_server_requests_seconds_count{application=\"eng-study\"}[1m])");
        request.setStart(1678886400L);
        request.setEnd(1678886500L);
        request.setStep("15s");

        List<Map<String, Object>> mockPrometheusData = createRangeQueryData();
        when(prometheusService.queryRange(anyString(), anyLong(), anyLong(), anyString())).thenReturn(mockPrometheusData);

        mockMvc.perform(post("/api/metrics/range")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.query").value(request.getQuery()));

        verify(prometheusService).queryRange(anyString(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("PromQL 쿼리 실행 (Instant Query) API 테스트")
    void executeQuery() throws Exception {
        PrometheusQueryRequestDTO request = new PrometheusQueryRequestDTO();
        request.setQuery("up{job=\"kubernetes-nodes\"}");

        Map<String, Object> mockPrometheusResponse = Map.of(
                "status", "success",
                "data", Map.of("resultType", "vector", "result", Collections.emptyList()));
        when(prometheusService.queryInstance(anyString())).thenReturn(mockPrometheusResponse);

        mockMvc.perform(post("/api/metrics/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("success"));

        verify(prometheusService).queryInstance(request.getQuery());
    }

    @Test
    @DisplayName("메트릭 이름 목록 조회 API 테스트")
    void getMetricNames() throws Exception {
        List<String> mockMetricNames = Arrays.asList("http_requests_total", "jvm_memory_bytes_used");
        when(prometheusService.getMetricNames()).thenReturn(mockMetricNames);

        mockMvc.perform(get("/api/metrics/names"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(prometheusService).getMetricNames();
    }
}
