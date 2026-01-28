package com.study.monitoring.studymonitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.monitoring.studymonitoring.controller.AlertController;
import com.study.monitoring.studymonitoring.model.dto.request.AlertRuleRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.AlertHistoryResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.AlertRuleResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.ApiResponseDTO;
import com.study.monitoring.studymonitoring.service.AlertService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
public class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AlertService alertService;

    @Autowired
    private WebApplicationContext ctx;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(print())
                .build();
    }

    private AlertRuleRequestDTO createAlertRuleRequest() {
        return AlertRuleRequestDTO.builder()
                .name("Test CPU Alert")
                .application("eng-study")
                .metricType("CPU_USAGE")
                .condition(">")
                .threshold(BigDecimal.valueOf(80.0))
                .durationMinutes(5)
                .notificationMethods(Arrays.asList("SLACK"))
                .active(true)
                .notificationSlack("#alerts")
                .build();
    }

    private AlertRuleResponseDTO createAlertRuleResponse(Long id) {
        return AlertRuleResponseDTO.builder()
                .id(id)
                .name("Test CPU Alert")
                .application("eng-study")
                .metricType("CPU_USAGE")
                .condition(">")
                .threshold(BigDecimal.valueOf(80.0))
                .durationMinutes(5)
                .severity("WARNING")
                .notificationMethods(Arrays.asList("SLACK"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AlertHistoryResponseDTO createAlertHistoryResponse(Long id, Long ruleId) {
        return AlertHistoryResponseDTO.builder()
                .id(id)
                .alertRuleId(ruleId)
                .alertRuleName("Test CPU Alert")
                .application("eng-study")
                .metricType("CPU_USAGE")
                .triggeredAt(LocalDateTime.now())
                .currentValue(BigDecimal.valueOf(85.5))
                .thresholdValue(BigDecimal.valueOf(80.0))
                .message("CPU Usage exceeded")
                .severity("CRITICAL")
                .resolved(false)
                .build();
    }

    @Test
    @DisplayName("알림 규칙 생성 성공")
    void createAlertSuccess() throws Exception {
        AlertRuleRequestDTO request = createAlertRuleRequest();
        AlertRuleResponseDTO response = createAlertRuleResponse(1L);

        when(alertService.createAlertRule(any(AlertRuleRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.message").value("알림 규칙이 생성되었습니다"));

        verify(alertService).createAlertRule(any(AlertRuleRequestDTO.class));
    }

    @Test
    @DisplayName("알림 규칙 생성 실패 - 유효성 검증 오류")
    void createAlertFailureValidation() throws Exception {
        AlertRuleRequestDTO request = createAlertRuleRequest();
        request.setName(""); // 이름 비워서 유효성 검증 실패 유도

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("모든 알림 규칙 조회")
    void getAllAlerts() throws Exception {
        List<AlertRuleResponseDTO> alerts = Arrays.asList(
                createAlertRuleResponse(1L),
                createAlertRuleResponse(2L)
        );
        when(alertService.getAllAlertRules()).thenReturn(alerts);

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(alertService).getAllAlertRules();
    }

    @Test
    @DisplayName("활성화된 알림 규칙 조회")
    void getActiveAlerts() throws Exception {
        List<AlertRuleResponseDTO> alerts = Collections.singletonList(createAlertRuleResponse(1L));
        when(alertService.getActiveAlertRules()).thenReturn(alerts);

        mockMvc.perform(get("/api/alerts").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(alertService).getActiveAlertRules();
    }

    @Test
    @DisplayName("알림 규칙 단건 조회 성공")
    void getAlertByIdSuccess() throws Exception {
        AlertRuleResponseDTO response = createAlertRuleResponse(1L);
        when(alertService.getAlertRule(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/alerts/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L));

        verify(alertService).getAlertRule(1L);
    }

    @Test
    @DisplayName("알림 규칙 단건 조회 실패 - 찾을 수 없음")
    void getAlertByIdNotFound() throws Exception {
        when(alertService.getAlertRule(anyLong()))
                .thenThrow(new IllegalArgumentException("Alert rule not found"));

        mockMvc.perform(get("/api/alerts/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));

        verify(alertService).getAlertRule(99L);
    }

    @Test
    @DisplayName("알림 규칙 수정 성공")
    void updateAlertSuccess() throws Exception {
        AlertRuleRequestDTO request = createAlertRuleRequest();
        request.setName("Updated Alert");
        AlertRuleResponseDTO response = createAlertRuleResponse(1L);
        response.setName("Updated Alert");

        when(alertService.updateAlertRule(anyLong(), any(AlertRuleRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/alerts/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Alert"));

        verify(alertService).updateAlertRule(anyLong(), any(AlertRuleRequestDTO.class));
    }

    @Test
    @DisplayName("알림 규칙 삭제 성공")
    void deleteAlertSuccess() throws Exception {
        doNothing().when(alertService).deleteAlertRule(anyLong());

        mockMvc.perform(delete("/api/alerts/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(alertService).deleteAlertRule(1L);
    }

    @Test
    @DisplayName("알림 규칙 토글 성공")
    void toggleAlertSuccess() throws Exception {
        AlertRuleResponseDTO response = createAlertRuleResponse(1L);
        response.setActive(false); // 토글 후 비활성화 상태 가정

        when(alertService.toggleAlertRule(anyLong())).thenReturn(response);

        mockMvc.perform(patch("/api/alerts/{id}/toggle", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));

        verify(alertService).toggleAlertRule(1L);
    }

    @Test
    @DisplayName("최근 알림 히스토리 조회")
    void getRecentAlertHistory() throws Exception {
        List<AlertHistoryResponseDTO> history = Collections.singletonList(createAlertHistoryResponse(100L, 1L));
        when(alertService.getRecentHistory(any(Integer.class), any(Integer.class))).thenReturn(history);

        mockMvc.perform(get("/api/alerts/history").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(alertService).getRecentHistory(0, 10);
    }

    @Test
    @DisplayName("미해결 알림 조회")
    void getUnresolvedAlerts() throws Exception {
        List<AlertHistoryResponseDTO> unresolved = Collections.singletonList(createAlertHistoryResponse(101L, 2L));
        when(alertService.getUnresolvedAlerts()).thenReturn(unresolved);

        mockMvc.perform(get("/api/alerts/history/unresolved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(alertService).getUnresolvedAlerts();
    }

    @Test
    @DisplayName("알림 해결 처리 성공")
    void resolveAlertSuccess() throws Exception {
        doNothing().when(alertService).resolveAlert(anyLong(), anyString());

        mockMvc.perform(patch("/api/alerts/history/{id}/resolve", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonMap("message", "Resolved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(alertService).resolveAlert(100L, "Resolved");
    }
}
