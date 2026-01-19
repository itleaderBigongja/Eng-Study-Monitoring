package com.study.monitoring.studymonitoring.controller;

import com.study.monitoring.studymonitoring.model.dto.response.AlertHistoryResponseDTO;
import com.study.monitoring.studymonitoring.service.AlertHistoryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * 알림 히스토리 컨트롤러
 * ============================================================================
 * API 경로: /api/v1/alert-history
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alert-history")
@RequiredArgsConstructor
public class AlertHistoryController {

    private final AlertHistoryService alertHistoryService;

    /**
     * 1. 최근 알림 이력 조회
     * GET /api/v1/alert-history?page=1&size=20
     */
    @GetMapping
    public ResponseEntity<List<AlertHistoryResponseDTO>> getRecentHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<AlertHistoryResponseDTO> history = alertHistoryService.getRecentHistory(page, size);
        return ResponseEntity.ok(history);
    }

    /**
     * 2. 미해결 알림 목록 조회 (Dashboard용)
     * GET /api/v1/alert-history/unresolved
     */
    @GetMapping("/unresolved")
    public ResponseEntity<List<AlertHistoryResponseDTO>> getUnresolvedAlerts() {
        List<AlertHistoryResponseDTO> unresolved = alertHistoryService.getUnresolvedAlerts();
        return ResponseEntity.ok(unresolved);
    }

    /**
     * 3. 특정 규칙의 이력 조회
     * GET /api/v1/alert-history/rule/{ruleId}?page=1&size=20
     */
    @GetMapping("/rule/{ruleId}")
    public ResponseEntity<List<AlertHistoryResponseDTO>> getHistoryByRuleId(
            @PathVariable Long ruleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<AlertHistoryResponseDTO> history = alertHistoryService.getHistoryByRuleId(ruleId, page, size);
        return ResponseEntity.ok(history);
    }

    /**
     * 4. 알림 단건 해결 처리
     * PUT /api/v1/alert-history/{historyId}/resolve
     * Body: { "message": "조치 완료했습니다." }
     */
    @PutMapping("/{historyId}/resolve")
    public ResponseEntity<String> resolveAlert(
            @PathVariable Long historyId,
            @RequestBody AlertResolveRequest request) {

        boolean resolved = alertHistoryService.resolveAlert(historyId, request.getMessage());
        if (resolved) {
            return ResponseEntity.ok("성공적으로 해결 처리되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("해당 알림을 찾을 수 없거나 이미 해결되었습니다.");
        }
    }

    /**
     * 5. 특정 규칙의 모든 미해결 건 일괄 해결
     * PUT /api/v1/alert-history/rule/{ruleId}/resolve-all
     * Body: { "message": "일괄 조치함" }
     */
    @PutMapping("/rule/{ruleId}/resolve-all")
    public ResponseEntity<String> resolveAllByRule(
            @PathVariable Long ruleId,
            @RequestBody AlertResolveRequest request) {

        int count = alertHistoryService.resolveAllByRuleId(ruleId, request.getMessage());
        return ResponseEntity.ok(count + "건의 알림이 일괄 해결 처리되었습니다.");
    }

    // === 요청용 간단 DTO (Inner Class) ===
    @Data
    public static class AlertResolveRequest {
        private String message;
    }
}