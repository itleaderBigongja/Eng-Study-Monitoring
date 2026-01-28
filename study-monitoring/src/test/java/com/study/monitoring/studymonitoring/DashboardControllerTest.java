package com.study.monitoring.studymonitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.monitoring.studymonitoring.controller.DashboardController;
import com.study.monitoring.studymonitoring.model.dto.request.MetricsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.DashboardResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.MetricsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.PageResponseDTO;
import com.study.monitoring.studymonitoring.service.DashboardService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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

@WebMvcTest(DashboardController.class)
public class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DashboardService dashboardService;

    @Autowired
    private WebApplicationContext ctx;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(print())
                .build();
    }

    private DashboardResponseDTO createDashboardOverviewResponse() {
        DashboardResponseDTO.ProcessStatusDTO process1 = new DashboardResponseDTO.ProcessStatusDTO(
                1L, "App1", "BACKEND", "RUNNING", 5.0, 10.0, "1h 30m", "2026-01-27 10:00:00");
        DashboardResponseDTO.ProcessStatusDTO process2 = new DashboardResponseDTO.ProcessStatusDTO(
                2L, "DB1", "DATABASE", "UP", 10.0, 20.0, "2h 0m", "2026-01-27 10:00:00");
        List<DashboardResponseDTO.ProcessStatusDTO> processes = Arrays.asList(process1, process2);

        DashboardResponseDTO.ApplicationMetricsDTO engStudyMetrics = new DashboardResponseDTO.ApplicationMetricsDTO(
                100.0, 50.0, 1.0, 200.0);
        DashboardResponseDTO.MetricsSummaryDTO metricsSummary = new DashboardResponseDTO.MetricsSummaryDTO(
                engStudyMetrics, null);

        DashboardResponseDTO.ErrorLogDTO errorLog = new DashboardResponseDTO.ErrorLogDTO(
                "log1", "2026-01-27 10:00:00", "ERROR", "NPE occurred", "App1");
        List<DashboardResponseDTO.ErrorLogDTO> recentErrors = Collections.singletonList(errorLog);

        Map<String, Long> logCounts = Map.of("ERROR", 10L, "INFO", 100L);

        DashboardResponseDTO.SystemStatisticsDTO systemStats = new DashboardResponseDTO.SystemStatisticsDTO(
                1000L, 150.0, "10h 0m");

        return new DashboardResponseDTO(processes, metricsSummary, recentErrors, logCounts, systemStats);
    }

    private MetricsResponseDTO createMetricsResponse() {
        MetricsResponseDTO.DataPoint dp1 = new MetricsResponseDTO.DataPoint(1678886400L, 10.5);
        MetricsResponseDTO.DataPoint dp2 = new MetricsResponseDTO.DataPoint(1678886460L, 12.3);
        return new MetricsResponseDTO("TestApp", "CPU_USAGE", Arrays.asList(dp1, dp2), 1678886400L, 1678886500L);
    }

    private Map<String, Object> createProcessStatusResponse() {
        Map<String, Object> summary = Map.of("total", 2L, "running", 1L, "stopped", 1L);
        Map<String, Object> process1 = Map.of("processId", 1L, "processName", "App1", "status", "RUNNING");
        return Map.of("processes", Collections.singletonList(process1), "summary", summary);
    }

    private PageResponseDTO<DashboardResponseDTO.ErrorLogDTO> createErrorLogsPageResponse() {
        DashboardResponseDTO.ErrorLogDTO errorLog = new DashboardResponseDTO.ErrorLogDTO(
                "log1", "2026-01-27 10:00:00", "ERROR", "NPE occurred", "App1");
        return PageResponseDTO.<DashboardResponseDTO.ErrorLogDTO>builder()
                .content(Collections.singletonList(errorLog))
                .totalElements(1L).totalPages(1).currentPage(0).size(5)
                .build();
    }


    @Test
    @DisplayName("대시보드 개요 조회")
    void getDashboardOverview() throws Exception {
        DashboardResponseDTO mockResponse = createDashboardOverviewResponse();
        when(dashboardService.getDashboardOverview()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.processes.length()").value(2))
                .andExpect(jsonPath("$.data.metrics.engStudy.tps").value(100.0));

        verify(dashboardService).getDashboardOverview();
    }

    @Test
    @DisplayName("실시간 메트릭 조회")
    void getMetrics() throws Exception {
        MetricsResponseDTO mockResponse = createMetricsResponse();
        when(dashboardService.getMetrics(any(MetricsQueryRequestDTO.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/dashboard/metrics")
                        .param("application", "TestApp")
                        .param("metric", "CPU_USAGE")
                        .param("hours", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.application").value("TestApp"));

        verify(dashboardService).getMetrics(any(MetricsQueryRequestDTO.class));
    }

    @Test
    @DisplayName("프로세스 현황 조회")
    void getProcesses() throws Exception {
        Map<String, Object> mockResponse = createProcessStatusResponse();
        when(dashboardService.getProcessStatus()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/dashboard/processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.processes[0].processName").value("App1"));

        verify(dashboardService).getProcessStatus();
    }

    @Test
    @DisplayName("에러 로그 목록 조회")
    void getErrorLogs() throws Exception {
        PageResponseDTO<DashboardResponseDTO.ErrorLogDTO> mockResponse = createErrorLogsPageResponse();
        when(dashboardService.getErrorLogs(anyString(), anyInt(), anyInt())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/dashboard/errors")
                        .param("type", "APP")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].message").value("NPE occurred"));

        verify(dashboardService).getErrorLogs("APP", 1, 5);
    }
}
