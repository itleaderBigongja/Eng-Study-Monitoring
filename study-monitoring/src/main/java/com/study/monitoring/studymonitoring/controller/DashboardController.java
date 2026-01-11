package com.study.monitoring.studymonitoring.controller;

import com.study.monitoring.studymonitoring.model.dto.request.MetricsQueryRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.ApiResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.DashboardResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.MetricsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.PageResponseDTO;
import com.study.monitoring.studymonitoring.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Validated
public class DashboardController {

    private final DashboardService dashboardService; // ✅ 오직 이 서비스만 의존

    /**
     * 대시보드 전체 현황 조회
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponseDTO<DashboardResponseDTO>> getDashboardOverview() {
        // 서비스가 이미 완성된 DTO를 반환하므로 Controller는 전달만 함
        return ResponseEntity.ok(ApiResponseDTO.success(dashboardService.getDashboardOverview()));
    }

    /**
     * 실시간 메트릭 조회
     */
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponseDTO<MetricsResponseDTO>> getMetrics(
            @Valid @ModelAttribute MetricsQueryRequestDTO request) {
        return ResponseEntity.ok(ApiResponseDTO.success(dashboardService.getMetrics(request)));
    }

    /**
     * 프로세스 현황 조회
     */
    @GetMapping("/processes")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getProcesses() {
        return ResponseEntity.ok(ApiResponseDTO.success(dashboardService.getProcessStatus()));
    }

    /**
     * 에러 로그 목록 조회 (페이징)
     * ✅ 복잡한 매핑 로직은 모두 Service로 이동했습니다.
     */
    @GetMapping("/errors")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<DashboardResponseDTO.ErrorLogDTO>>> getErrorLogs(
            @RequestParam(defaultValue = "APP") String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                dashboardService.getErrorLogs(type, page, size)
        ));
    }
}