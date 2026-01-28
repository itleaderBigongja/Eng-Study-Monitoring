package com.study.monitoring.studymonitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.monitoring.studymonitoring.controller.StatisticsController;
import com.study.monitoring.studymonitoring.model.dto.request.AccessLogStatisticsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.request.AuditLogStatisticsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.request.DatabaseLogStatisticsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.request.ErrorLogStatisticsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.request.LogStatisticsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.request.PerformanceMetricsStatisticsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.request.SecurityLogStatisticsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.request.StatisticsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.AccessLogStatisticsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.AuditLogStatisticsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.DatabaseLogStatisticsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.ErrorLogStatisticsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.LogStatisticsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.PerformanceMetricsStatisticsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.SecurityLogStatisticsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.StatisticsResponseDTO;
import com.study.monitoring.studymonitoring.service.StatisticsService;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
public class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StatisticsService statisticsService;

    @Autowired
    private WebApplicationContext ctx;

    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(print())
                .build();
    }

    private StatisticsResponseDTO createTimeSeriesResponse() {
        StatisticsResponseDTO.DataPoint dp1 = new StatisticsResponseDTO.DataPoint(
                LocalDateTime.now().minusHours(1).atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                100.0, 90.0, 110.0, 60);
        return new StatisticsResponseDTO(
                LocalDateTime.now().minusHours(2).atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                "TPS", "HOUR", "AVG", "MIXED", Collections.singletonList(dp1));
    }

    private LogStatisticsResponseDTO createLogStatisticsResponse() {
        LogStatisticsResponseDTO.LogDistribution ld = new LogStatisticsResponseDTO.LogDistribution(
                FORMATTER.format(LocalDateTime.now().minusHours(1)), 100L);
        return new LogStatisticsResponseDTO(
                LocalDateTime.now().minusHours(2).atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                "HOUR", Map.of("INFO", 1000L, "ERROR", 100L), Collections.singletonList(ld));
    }

    private AccessLogStatisticsResponseDTO createAccessLogStatisticsResponse() {
        AccessLogStatisticsResponseDTO.AccessDistribution ad = new AccessLogStatisticsResponseDTO.AccessDistribution(
                FORMATTER.format(LocalDateTime.now().minusHours(1)), 500L, 150.0, 5L);
        return new AccessLogStatisticsResponseDTO(
                LocalDateTime.now().minusHours(2).atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                "HOUR", Map.of("GET", 400L), Map.of("200", 300L), 120.0, 1000L, 10L, 1.0, Collections.singletonList(ad));
    }

    private ErrorLogStatisticsResponseDTO createErrorLogStatisticsResponse() {
        ErrorLogStatisticsResponseDTO.ErrorDistribution ed = new ErrorLogStatisticsResponseDTO.ErrorDistribution(
                FORMATTER.format(LocalDateTime.now().minusHours(1)), 10L, Map.of("NPE", 5L));
        return new ErrorLogStatisticsResponseDTO(
                LocalDateTime.now().minusHours(2).atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                "HOUR", Map.of("NPE", 10L), Map.of("ERROR", 15L), Collections.singletonList(ed));
    }

    private PerformanceMetricsStatisticsResponseDTO createPerformanceMetricsStatisticsResponse() {
        PerformanceMetricsStatisticsResponseDTO.SystemMetrics sm = new PerformanceMetricsStatisticsResponseDTO.SystemMetrics(
                50.0, 70.0, 20.0, 80.0, 90.0);
        PerformanceMetricsStatisticsResponseDTO.JvmMetrics jm = new PerformanceMetricsStatisticsResponseDTO.JvmMetrics(
                60.0, 80.0, 100L, 5000L, 30.0);
        PerformanceMetricsStatisticsResponseDTO.MetricDistribution md = new PerformanceMetricsStatisticsResponseDTO.MetricDistribution(
                FORMATTER.format(LocalDateTime.now().minusHours(1)), 55.0, 75.0, 65.0);
        return new PerformanceMetricsStatisticsResponseDTO(
                LocalDateTime.now().minusHours(2).atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                "HOUR", sm, jm, Collections.singletonList(md));
    }

    private DatabaseLogStatisticsResponseDTO createDatabaseLogStatisticsResponse() {
        DatabaseLogStatisticsResponseDTO.QueryPerformance qp = new DatabaseLogStatisticsResponseDTO.QueryPerformance(
                50.0, 200.0, 5L, 1000L);
        DatabaseLogStatisticsResponseDTO.DatabaseDistribution dd = new DatabaseLogStatisticsResponseDTO.DatabaseDistribution(
                FORMATTER.format(LocalDateTime.now().minusHours(1)), 100L, 60.0, 2L);
        return new DatabaseLogStatisticsResponseDTO(
                LocalDateTime.now().minusHours(2).atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                "HOUR", Map.of("SELECT", 500L), Map.of("users", 200L), qp, Collections.singletonList(dd));
    }

    private AuditLogStatisticsResponseDTO createAuditLogStatisticsResponse() {
        AuditLogStatisticsResponseDTO.ResultStats rs = new AuditLogStatisticsResponseDTO.ResultStats(
                90L, 10L, 90.0);
        AuditLogStatisticsResponseDTO.AuditDistribution ad = new AuditLogStatisticsResponseDTO.AuditDistribution(
                FORMATTER.format(LocalDateTime.now().minusHours(1)), 100L, 90L, 10L);
        return new AuditLogStatisticsResponseDTO(
                LocalDateTime.now().minusHours(2).atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                "HOUR", Map.of("login", 50L), Map.of("authentication", 70L), rs, Collections.singletonList(ad));
    }

    private SecurityLogStatisticsResponseDTO createSecurityLogStatisticsResponse() {
        SecurityLogStatisticsResponseDTO.BlockStats bs = new SecurityLogStatisticsResponseDTO.BlockStats(
                100L, 90L, 10L, 90.0);
        SecurityLogStatisticsResponseDTO.SecurityDistribution sd = new SecurityLogStatisticsResponseDTO.SecurityDistribution(
                FORMATTER.format(LocalDateTime.now().minusHours(1)), 10L, 8L, Map.of("HIGH", 5L));
        return new SecurityLogStatisticsResponseDTO(
                LocalDateTime.now().minusHours(2).atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                "HOUR", Map.of("CRITICAL", 5L), Map.of("SQL_INJECTION", 3L), bs, Collections.singletonList(sd));
    }


    @Test
    @DisplayName("시계열 데이터 통계 조회 API 테스트")
    void getTimeSeriesStatistics() throws Exception {
        StatisticsResponseDTO mockResponse = createTimeSeriesResponse();
        when(statisticsService.getTimeSeriesStatistics(any(StatisticsQueryRequestDTO.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/statistics/timeseries")
                        .param("startTime", FORMATTER.format(LocalDateTime.now().minusHours(2)))
                        .param("endTime", FORMATTER.format(LocalDateTime.now()))
                        .param("metricType", "TPS")
                        .param("timePeriod", "HOUR")
                        .param("aggregationType", "AVG")
                        .param("application", "eng-study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metricType").value("TPS"));

        verify(statisticsService).getTimeSeriesStatistics(any(StatisticsQueryRequestDTO.class));
    }

    @Test
    @DisplayName("로그 통계 조회 API 테스트")
    void getLogStatistics() throws Exception {
        LogStatisticsResponseDTO mockResponse = createLogStatisticsResponse();
        when(statisticsService.getLogStatistics(any(LogStatisticsQueryRequestDTO.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/statistics/logs")
                        .param("startTime", FORMATTER.format(LocalDateTime.now().minusHours(2)))
                        .param("endTime", FORMATTER.format(LocalDateTime.now()))
                        .param("timePeriod", "HOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.logCounts.INFO").value(1000L));

        verify(statisticsService).getLogStatistics(any(LogStatisticsQueryRequestDTO.class));
    }

    @Test
    @DisplayName("접근 로그 통계 조회 API 테스트")
    void getAccessLogStatistics() throws Exception {
        AccessLogStatisticsResponseDTO mockResponse = createAccessLogStatisticsResponse();
        when(statisticsService.getAccessLogStatistics(any(AccessLogStatisticsQueryRequestDTO.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/statistics/access-logs")
                        .param("startTime", FORMATTER.format(LocalDateTime.now().minusHours(2)))
                        .param("endTime", FORMATTER.format(LocalDateTime.now()))
                        .param("timePeriod", "HOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.methodCounts.GET").value(400L));

        verify(statisticsService).getAccessLogStatistics(any(AccessLogStatisticsQueryRequestDTO.class));
    }

    @Test
    @DisplayName("에러 로그 통계 조회 API 테스트")
    void getErrorLogStatistics() throws Exception {
        ErrorLogStatisticsResponseDTO mockResponse = createErrorLogStatisticsResponse();
        when(statisticsService.getErrorLogStatistics(any(ErrorLogStatisticsQueryRequestDTO.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/statistics/error-logs")
                        .param("startTime", FORMATTER.format(LocalDateTime.now().minusHours(2)))
                        .param("endTime", FORMATTER.format(LocalDateTime.now()))
                        .param("timePeriod", "HOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.errorTypeCounts.NPE").value(10L));

        verify(statisticsService).getErrorLogStatistics(any(ErrorLogStatisticsQueryRequestDTO.class));
    }

    @Test
    @DisplayName("성능 메트릭 통계 조회 API 테스트")
    void getPerformanceMetricsStatistics() throws Exception {
        PerformanceMetricsStatisticsResponseDTO mockResponse = createPerformanceMetricsStatisticsResponse();
        when(statisticsService.getPerformanceMetricsStatistics(any(PerformanceMetricsStatisticsQueryRequestDTO.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/statistics/performance-metrics")
                        .param("startTime", FORMATTER.format(LocalDateTime.now().minusHours(2)))
                        .param("endTime", FORMATTER.format(LocalDateTime.now()))
                        .param("timePeriod", "HOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.systemMetrics.avgCpuUsage").value(50.0));

        verify(statisticsService).getPerformanceMetricsStatistics(any(PerformanceMetricsStatisticsQueryRequestDTO.class));
    }

    @Test
    @DisplayName("데이터베이스 로그 통계 조회 API 테스트")
    void getDatabaseLogStatistics() throws Exception {
        DatabaseLogStatisticsResponseDTO mockResponse = createDatabaseLogStatisticsResponse();
        when(statisticsService.getDatabaseLogStatistics(any(DatabaseLogStatisticsQueryRequestDTO.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/statistics/database-logs")
                        .param("startTime", FORMATTER.format(LocalDateTime.now().minusHours(2)))
                        .param("endTime", FORMATTER.format(LocalDateTime.now()))
                        .param("timePeriod", "HOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.operationCounts.SELECT").value(500L));

        verify(statisticsService).getDatabaseLogStatistics(any(DatabaseLogStatisticsQueryRequestDTO.class));
    }

    @Test
    @DisplayName("감사 로그 통계 조회 API 테스트")
    void getAuditLogStatistics() throws Exception {
        AuditLogStatisticsResponseDTO mockResponse = createAuditLogStatisticsResponse();
        when(statisticsService.getAuditLogStatistics(any(AuditLogStatisticsQueryRequestDTO.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/statistics/audit-logs")
                        .param("startTime", FORMATTER.format(LocalDateTime.now().minusHours(2)))
                        .param("endTime", FORMATTER.format(LocalDateTime.now()))
                        .param("timePeriod", "HOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventActionCounts.login").value(50L));

        verify(statisticsService).getAuditLogStatistics(any(AuditLogStatisticsQueryRequestDTO.class));
    }

    @Test
    @DisplayName("보안 로그 통계 조회 API 테스트")
    void getSecurityLogStatistics() throws Exception {
        SecurityLogStatisticsResponseDTO mockResponse = createSecurityLogStatisticsResponse();
        when(statisticsService.getSecurityLogStatistics(any(SecurityLogStatisticsQueryRequestDTO.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/statistics/security-logs")
                        .param("startTime", FORMATTER.format(LocalDateTime.now().minusHours(2)))
                        .param("endTime", FORMATTER.format(LocalDateTime.now()))
                        .param("timePeriod", "HOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.threatLevelCounts.CRITICAL").value(5L));

        verify(statisticsService).getSecurityLogStatistics(any(SecurityLogStatisticsQueryRequestDTO.class));
    }
}
