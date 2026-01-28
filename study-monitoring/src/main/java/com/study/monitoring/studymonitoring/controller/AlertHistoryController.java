package com.study.monitoring.studymonitoring.controller;

import com.study.monitoring.studymonitoring.model.dto.response.ApiResponseDTO; // 패키지 경로 확인 필요
import com.study.monitoring.studymonitoring.model.dto.response.AlertHistoryResponseDTO;
import com.study.monitoring.studymonitoring.service.AlertHistoryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alert-history")
@RequiredArgsConstructor
public class AlertHistoryController {

    private final AlertHistoryService alertHistoryService;

    // 1. 최근 알림 이력 조회
    @GetMapping
    public ApiResponseDTO<List<AlertHistoryResponseDTO>> getRecentHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<AlertHistoryResponseDTO> history = alertHistoryService.getRecentHistory(page, size);
        return ApiResponseDTO.success(history);
    }

    // 2. 미해결 알림 목록 조회
    @GetMapping("/unresolved")
    public ApiResponseDTO<List<AlertHistoryResponseDTO>> getUnresolvedAlerts() {
        List<AlertHistoryResponseDTO> unresolved = alertHistoryService.getUnresolvedAlerts();
        return ApiResponseDTO.success(unresolved);
    }

    // 3. 특정 규칙의 이력 조회
    @GetMapping("/rule/{ruleId}")
    public ApiResponseDTO<List<AlertHistoryResponseDTO>> getHistoryByRuleId(
            @PathVariable Long ruleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<AlertHistoryResponseDTO> history = alertHistoryService.getHistoryByRuleId(ruleId, page, size);
        return ApiResponseDTO.success(history);
    }

    // 4. 알림 단건 해결 처리
    @PutMapping("/{historyId}/resolve")
    public ResponseEntity<ApiResponseDTO<Void>> resolveAlert(
            @PathVariable Long historyId,
            @RequestBody AlertResolveRequest request) {

        boolean resolved = alertHistoryService.resolveAlert(historyId, request.getMessage());

        if (resolved) {
            // 성공 시 200 OK + 성공 메시지
            return ResponseEntity.ok(ApiResponseDTO.success(null, "성공적으로 해결 처리되었습니다."));
        } else {
            // 실패 시 400 Bad Request + 에러 메시지
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.fail("해당 알림을 찾을 수 없거나 이미 해결되었습니다."));
        }
    }

    // 5. 특정 규칙의 모든 미해결 건 일괄 해결
    @PutMapping("/rule/{ruleId}/resolve-all")
    public ApiResponseDTO<Integer> resolveAllByRule(
            @PathVariable Long ruleId,
            @RequestBody AlertResolveRequest request) {

        int count = alertHistoryService.resolveAllByRuleId(ruleId, request.getMessage());

        // 데이터에는 '개수', 메시지에는 '문장'을 담아서 리턴
        return ApiResponseDTO.success(count, count + "건의 알림이 일괄 해결 처리되었습니다.");
    }

    // 요청 DTO (Inner Class로 사용 중이신 것 같아 포함)
    @Data
    public static class AlertResolveRequest {
        private String message;
    }
}