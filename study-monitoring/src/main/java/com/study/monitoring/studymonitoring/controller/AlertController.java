package com.study.monitoring.studymonitoring.controller;

import com.study.monitoring.studymonitoring.model.dto.request.AlertRuleRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.AlertHistoryResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.AlertRuleResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.ApiResponseDTO;
import com.study.monitoring.studymonitoring.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * 알림 관리 API 컨트롤러
 * ============================================================================
 *
 * 역할:
 * - 알림 규칙 CRUD API
 * - 알림 히스토리 조회 API
 * - 알림 활성화/비활성화 API
 *
 * 엔드포인트:
 * - POST   /api/alerts              : 알림 규칙 생성
 * - GET    /api/alerts              : 모든 알림 규칙 조회
 * - GET    /api/alerts/{id}         : 단건 조회
 * - PUT    /api/alerts/{id}         : 알림 규칙 수정
 * - DELETE /api/alerts/{id}         : 알림 규칙 삭제
 * - PATCH  /api/alerts/{id}/toggle  : 활성화/비활성화 토글
 * - GET    /api/alerts/history      : 알림 히스토리 조회
 *
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Validated
public class AlertController {

    private final AlertService alertService;

    // ========================================================================
    // 📌 알림 규칙 CRUD
    // ========================================================================

    /**
     * 알림 규칙 생성
     *
     * POST /api/alerts
     *
     * @param request 알림 규칙 요청 DTO
     * @return 생성된 알림 규칙
     */
    @PostMapping
    public ResponseEntity<ApiResponseDTO<AlertRuleResponseDTO>> createAlert(
            @Valid @RequestBody AlertRuleRequestDTO request) {
        try {
            log.info("📝 [Alert API] 알림 규칙 생성 요청 - {}", request.getName());

            AlertRuleResponseDTO response = alertService.createAlertRule(request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDTO.success(response, "알림 규칙이 생성되었습니다"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [Alert API] 잘못된 요청 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.validationFail(e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [Alert API] 알림 규칙 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.internalError("알림 규칙 생성 중 오류가 발생했습니다"));
        }
    }

    /**
     * 모든 알림 규칙 조회
     *
     * GET /api/alerts
     * GET /api/alerts?active=true (활성화된 것만)
     *
     * @param active 활성화 필터 (optional)
     * @return 알림 규칙 리스트
     */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<AlertRuleResponseDTO>>> getAllAlerts(
            @RequestParam(required = false) Boolean active) {
        try {
            log.info("📋 [Alert API] 알림 규칙 조회 - active: {}", active);

            List<AlertRuleResponseDTO> alerts = active != null && active
                    ? alertService.getActiveAlertRules()
                    : alertService.getAllAlertRules();

            String message = String.format("알림 규칙 %d개를 조회했습니다", alerts.size());
            return ResponseEntity.ok(ApiResponseDTO.success(alerts, message));

        } catch (Exception e) {
            log.error("❌ [Alert API] 알림 규칙 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.internalError("알림 규칙 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 알림 규칙 단건 조회
     *
     * GET /api/alerts/{id}
     *
     * @param id 알림 규칙 ID
     * @return 알림 규칙
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<AlertRuleResponseDTO>> getAlert(@PathVariable Long id) {
        try {
            log.info("🔍 [Alert API] 알림 규칙 조회 - ID: {}", id);

            AlertRuleResponseDTO alert = alertService.getAlertRule(id);

            return ResponseEntity.ok(ApiResponseDTO.success(alert, "알림 규칙을 조회했습니다"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [Alert API] 알림 규칙을 찾을 수 없음 - ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.notFound(e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [Alert API] 알림 규칙 조회 실패 - ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.internalError("알림 규칙 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 알림 규칙 수정
     *
     * PUT /api/alerts/{id}
     *
     * @param id 알림 규칙 ID
     * @param request 수정 요청 DTO
     * @return 수정된 알림 규칙
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<AlertRuleResponseDTO>> updateAlert(
            @PathVariable Long id,
            @Valid @RequestBody AlertRuleRequestDTO request) {
        try {
            log.info("📝 [Alert API] 알림 규칙 수정 요청 - ID: {}", id);

            AlertRuleResponseDTO response = alertService.updateAlertRule(id, request);

            return ResponseEntity.ok(ApiResponseDTO.success(response, "알림 규칙이 수정되었습니다"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [Alert API] 잘못된 요청 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.validationFail(e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [Alert API] 알림 규칙 수정 실패 - ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.internalError("알림 규칙 수정 중 오류가 발생했습니다"));
        }
    }

    /**
     * 알림 규칙 삭제
     *
     * DELETE /api/alerts/{id}
     *
     * @param id 알림 규칙 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteAlert(@PathVariable Long id) {
        try {
            log.info("🗑️ [Alert API] 알림 규칙 삭제 요청 - ID: {}", id);

            alertService.deleteAlertRule(id);

            return ResponseEntity.ok(ApiResponseDTO.success("알림 규칙이 삭제되었습니다"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [Alert API] 알림 규칙을 찾을 수 없음 - ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.notFound(e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [Alert API] 알림 규칙 삭제 실패 - ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.internalError("알림 규칙 삭제 중 오류가 발생했습니다"));
        }
    }

    /**
     * 알림 규칙 활성화/비활성화 토글
     *
     * PATCH /api/alerts/{id}/toggle
     *
     * @param id 알림 규칙 ID
     * @return 토글 후 알림 규칙
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponseDTO<AlertRuleResponseDTO>> toggleAlert(@PathVariable Long id) {
        try {
            log.info("🔄 [Alert API] 알림 규칙 토글 요청 - ID: {}", id);

            AlertRuleResponseDTO response = alertService.toggleAlertRule(id);

            String message = String.format("알림 규칙이 %s되었습니다",
                    response.getActive() ? "활성화" : "비활성화");
            return ResponseEntity.ok(ApiResponseDTO.success(response, message));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [Alert API] 알림 규칙을 찾을 수 없음 - ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.notFound(e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [Alert API] 알림 규칙 토글 실패 - ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.internalError("알림 규칙 토글 중 오류가 발생했습니다"));
        }
    }

    // ========================================================================
    // 📌 알림 히스토리 조회
    // ========================================================================

    /**
     * 최근 알림 히스토리 조회
     *
     * GET /api/alerts/history?page=0&size=20
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 알림 히스토리 리스트
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponseDTO<List<AlertHistoryResponseDTO>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("📜 [Alert API] 알림 히스토리 조회 - page: {}, size: {}", page, size);

            List<AlertHistoryResponseDTO> history = alertService.getRecentHistory(page, size);

            String message = String.format("알림 히스토리 %d개를 조회했습니다", history.size());
            return ResponseEntity.ok(ApiResponseDTO.success(history, message));

        } catch (Exception e) {
            log.error("❌ [Alert API] 알림 히스토리 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.internalError("알림 히스토리 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 미해결 알림 조회
     *
     * GET /api/alerts/history/unresolved
     *
     * @return 미해결 알림 리스트
     */
    @GetMapping("/history/unresolved")
    public ResponseEntity<ApiResponseDTO<List<AlertHistoryResponseDTO>>> getUnresolvedAlerts() {
        try {
            log.info("🔔 [Alert API] 미해결 알림 조회");

            List<AlertHistoryResponseDTO> alerts = alertService.getUnresolvedAlerts();

            String message = String.format("미해결 알림 %d개를 조회했습니다", alerts.size());
            return ResponseEntity.ok(ApiResponseDTO.success(alerts, message));

        } catch (Exception e) {
            log.error("❌ [Alert API] 미해결 알림 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.internalError("미해결 알림 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 알림 해결 처리
     *
     * PATCH /api/alerts/history/{id}/resolve
     *
     * @param id 히스토리 ID
     * @param body 해결 메시지 (optional)
     * @return 성공 메시지
     */
    @PatchMapping("/history/{id}/resolve")
    public ResponseEntity<ApiResponseDTO<Void>> resolveAlert(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String resolveMessage = (body != null && body.containsKey("message"))
                    ? body.get("message")
                    : "수동 해결됨";

            log.info("✅ [Alert API] 알림 해결 요청 - ID: {}, 메시지: {}", id, resolveMessage);

            alertService.resolveAlert(id, resolveMessage);

            return ResponseEntity.ok(ApiResponseDTO.success("알림이 해결 처리되었습니다"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [Alert API] 알림 히스토리를 찾을 수 없음 - ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.notFound(e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [Alert API] 알림 해결 실패 - ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.internalError("알림 해결 중 오류가 발생했습니다"));
        }
    }

    /**
     * 애플리케이션별 알림 규칙 조회
     * * GET /api/alerts/application/{application}
     * * @param application 애플리케이션 이름
     * @return 해당 앱의 알림 규칙 리스트
     */
    @GetMapping("/application/{application}")
    public ResponseEntity<ApiResponseDTO<List<AlertRuleResponseDTO>>> getAlertsByApplication(
            @PathVariable String application) {
        try {
            log.info("📋 [Alert API] 앱별 알림 규칙 조회 - App: {}", application);

            List<AlertRuleResponseDTO> alerts = alertService.getAlertRulesByApplication(application);

            String message = String.format("'%s' 애플리케이션의 알림 규칙 %d개를 조회했습니다",
                    application, alerts.size());
            return ResponseEntity.ok(ApiResponseDTO.success(alerts, message));

        } catch (Exception e) {
            log.error("❌ [Alert API] 앱별 알림 규칙 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.internalError("애플리케이션별 규칙 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 특정 알림 규칙의 히스토리 조회
     * * GET /api/alerts/{id}/history
     * * @param id 알림 규칙 ID
     * @param page 페이지
     * @param size 크기
     * @return 해당 규칙의 히스토리
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponseDTO<List<AlertHistoryResponseDTO>>> getHistoryByRule(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("📜 [Alert API] 특정 규칙 히스토리 조회 - ID: {}, page: {}", id, page);

            List<AlertHistoryResponseDTO> history = alertService.getHistoryByRule(id, page, size);

            String message = String.format("규칙(ID:%d)의 히스토리 %d개를 조회했습니다", id, history.size());
            return ResponseEntity.ok(ApiResponseDTO.success(history, message));

        } catch (Exception e) {
            log.error("❌ [Alert API] 특정 규칙 히스토리 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDTO.internalError("규칙별 히스토리 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * @Valid 유효성 검사 실패 시 발생하는 예외 처리
     * 메서드 진입 전에 발생하므로 여기서 잡아줘야 합니다.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        // 1. 에러 메시지 추출 (첫 번째 에러만 가져옴)
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        // 2. 로그 기록
        log.warn("⚠️ [Alert API] 유효성 검사 실패: {}", errorMessage);

        // 3. ApiResponseDTO를 사용하여 통일된 응답 생성
        // (Map을 쓰는 것보다 현재 프로젝트 구조에 더 맞습니다. 결과 JSON은 {"success":false...}로 동일합니다.)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.validationFail(errorMessage));
    }
}