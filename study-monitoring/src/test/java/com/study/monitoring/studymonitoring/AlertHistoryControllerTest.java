package com.study.monitoring.studymonitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.monitoring.studymonitoring.controller.AlertHistoryController;
import com.study.monitoring.studymonitoring.model.dto.response.AlertHistoryResponseDTO;
import com.study.monitoring.studymonitoring.service.AlertHistoryService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertHistoryController.class)
public class AlertHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AlertHistoryService alertHistoryService;

    @Autowired
    private WebApplicationContext ctx;

    @BeforeEach
    void setup() {
        // 한글 깨짐 방지 필터 적용
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(print())
                .build();
    }

    private AlertHistoryResponseDTO createAlertHistoryResponse(Long id, Long ruleId) {
        return AlertHistoryResponseDTO.builder()
                .id(id)
                .alertRuleId(ruleId)
                .alertRuleName("Test Rule")
                .application("TestApp")
                .metricType("CPU_USAGE")
                .triggeredAt(LocalDateTime.now())
                .currentValue(BigDecimal.valueOf(90.0))
                .thresholdValue(BigDecimal.valueOf(80.0))
                .message("CPU usage high")
                .severity("ERROR")
                .resolved(false)
                .build();
    }

    @Test
    @DisplayName("최근 알림 이력 조회")
    void getRecentHistory() throws Exception {
        List<AlertHistoryResponseDTO> mockHistory = Collections.singletonList(createAlertHistoryResponse(1L, 10L));
        when(alertHistoryService.getRecentHistory(any(Integer.class), any(Integer.class))).thenReturn(mockHistory);

        mockMvc.perform(get("/api/v1/alert-history").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                // ✅ 수정: 리스트는 'data' 필드 안에 있음
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1L));

        verify(alertHistoryService).getRecentHistory(1, 10);
    }

    @Test
    @DisplayName("미해결 알림 목록 조회")
    void getUnresolvedAlerts() throws Exception {
        List<AlertHistoryResponseDTO> mockHistory = Collections.singletonList(createAlertHistoryResponse(2L, 20L));
        when(alertHistoryService.getUnresolvedAlerts()).thenReturn(mockHistory);

        mockMvc.perform(get("/api/v1/alert-history/unresolved"))
                .andExpect(status().isOk())
                // ✅ 수정: 리스트는 'data' 필드 안에 있음
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(2L));

        verify(alertHistoryService).getUnresolvedAlerts();
    }

    @Test
    @DisplayName("특정 규칙의 이력 조회")
    void getHistoryByRuleId() throws Exception {
        List<AlertHistoryResponseDTO> mockHistory = Collections.singletonList(createAlertHistoryResponse(3L, 30L));
        when(alertHistoryService.getHistoryByRuleId(anyLong(), any(Integer.class), any(Integer.class))).thenReturn(mockHistory);

        mockMvc.perform(get("/api/v1/alert-history/rule/{ruleId}", 30L).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                // ✅ 수정: 리스트는 'data' 필드 안에 있음
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(3L));

        verify(alertHistoryService).getHistoryByRuleId(30L, 1, 10);
    }

    @Test
    @DisplayName("알림 단건 해결 처리 성공")
    void resolveAlertSuccess() throws Exception {
        AlertHistoryController.AlertResolveRequest request = new AlertHistoryController.AlertResolveRequest();
        request.setMessage("Resolved manually");
        when(alertHistoryService.resolveAlert(anyLong(), anyString())).thenReturn(true);

        mockMvc.perform(put("/api/v1/alert-history/{historyId}/resolve", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                // ✅ 수정: 메시지는 'message' 필드에, 성공여부는 'success'
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("성공적으로 해결 처리되었습니다."));

        verify(alertHistoryService).resolveAlert(1L, "Resolved manually");
    }

    @Test
    @DisplayName("알림 단건 해결 처리 실패 - 찾을 수 없음")
    void resolveAlertFailureNotFound() throws Exception {
        AlertHistoryController.AlertResolveRequest request = new AlertHistoryController.AlertResolveRequest();
        request.setMessage("Resolved manually");
        when(alertHistoryService.resolveAlert(anyLong(), anyString())).thenReturn(false);

        mockMvc.perform(put("/api/v1/alert-history/{historyId}/resolve", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                // ✅ 수정: 실패 시에도 ApiResponseDTO 구조로 응답함
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("해당 알림을 찾을 수 없거나 이미 해결되었습니다."));

        verify(alertHistoryService).resolveAlert(99L, "Resolved manually");
    }

    @Test
    @DisplayName("특정 규칙의 모든 미해결 건 일괄 해결 처리")
    void resolveAllByRuleSuccess() throws Exception {
        AlertHistoryController.AlertResolveRequest request = new AlertHistoryController.AlertResolveRequest();
        request.setMessage("Batch resolved");

        // Service는 해결된 개수(5)를 리턴
        when(alertHistoryService.resolveAllByRuleId(anyLong(), anyString())).thenReturn(5);

        mockMvc.perform(put("/api/v1/alert-history/rule/{ruleId}/resolve-all", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                // ✅ 핵심 수정:
                // data 필드에는 개수(5)가 들어있고,
                // message 필드에는 "5건의..." 문장이 들어있음
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(5))
                .andExpect(jsonPath("$.message").value("5건의 알림이 일괄 해결 처리되었습니다."));

        verify(alertHistoryService).resolveAllByRuleId(10L, "Batch resolved");
    }
}