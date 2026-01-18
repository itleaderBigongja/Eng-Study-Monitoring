package com.study.monitoring.studymonitoring.service.impl; // 패키지 경로 주의 (보통 impl 패키지 하위에 둠)

import com.study.monitoring.studymonitoring.converter.AlertConverter;
import com.study.monitoring.studymonitoring.mapper.AlertHistoryMapper;
import com.study.monitoring.studymonitoring.mapper.AlertRuleMapper;
import com.study.monitoring.studymonitoring.model.dto.request.AlertRuleRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.AlertHistoryResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.AlertRuleResponseDTO;
import com.study.monitoring.studymonitoring.model.vo.AlertHistoryVO;
import com.study.monitoring.studymonitoring.model.vo.AlertRuleVO;
import com.study.monitoring.studymonitoring.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * 알림 규칙 관리 서비스 구현체 (MyBatis 버전)
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertHistoryMapper alertHistoryMapper;
    private final AlertConverter alertConverter;

    // ========================================================================
    // 📌 알림 규칙 CRUD
    // ========================================================================

    /**
     * 알림 규칙 생성
     *
     * @param requestDTO 알림 규칙 요청 DTO
     * @return 생성된 알림 규칙 응답 DTO
     */
    @Transactional
    public AlertRuleResponseDTO createAlertRule(AlertRuleRequestDTO requestDTO) {
        log.info("📝 [AlertService] 알림 규칙 생성 - {}", requestDTO.getName());

        // 1. 중복 이름 체크
        if (alertRuleMapper.existsByName(requestDTO.getName())) {
            throw new IllegalArgumentException("이미 존재하는 알림 규칙 이름입니다: " + requestDTO.getName());
        }

        // 2. DTO → VO 변환
        AlertRuleVO vo = alertConverter.toVO(requestDTO);

        // 3. DB 저장
        alertRuleMapper.insertAlert(vo);

        log.info("✅ [AlertService] 알림 규칙 생성 완료 - ID: {}", vo.getAlertRuleId());

        // 4. VO → DTO 변환 후 반환
        return alertConverter.toResponseDTO(vo);
    }

    /**
     * 알림 규칙 수정
     *
     * @param id 알림 규칙 ID
     * @param requestDTO 수정 요청 DTO
     * @return 수정된 알림 규칙 응답 DTO
     */
    @Transactional
    public AlertRuleResponseDTO updateAlertRule(Long id, AlertRuleRequestDTO requestDTO) {
        log.info("📝 [AlertService] 알림 규칙 수정 - ID: {}", id);

        // 1. 기존 VO 조회
        AlertRuleVO vo = alertRuleMapper.selectAlertById(id);
        if (vo == null) {
            throw new IllegalArgumentException("알림 규칙을 찾을 수 없습니다: " + id);
        }

        // 2. 이름 변경 시 중복 체크
        if (!vo.getAlertName().equals(requestDTO.getName())
                && alertRuleMapper.existsByName(requestDTO.getName())) {
            throw new IllegalArgumentException("이미 존재하는 알림 규칙 이름입니다: " + requestDTO.getName());
        }

        // 3. VO 업데이트
        alertConverter.updateVO(vo, requestDTO);

        // 4. DB 저장
        alertRuleMapper.updateAlert(vo);

        log.info("✅ [AlertService] 알림 규칙 수정 완료 - ID: {}", id);

        // 5. 다시 조회 후 반환 (updated_at이 갱신되었으므로)
        AlertRuleVO updated = alertRuleMapper.selectAlertById(id);
        return alertConverter.toResponseDTO(updated);
    }

    /**
     * 알림 규칙 삭제
     *
     * @param id 알림 규칙 ID
     */
    @Transactional
    public void deleteAlertRule(Long id) {
        log.info("🗑️ [AlertService] 알림 규칙 삭제 - ID: {}", id);

        AlertRuleVO vo = alertRuleMapper.selectAlertById(id);
        if (vo == null) {
            throw new IllegalArgumentException("알림 규칙을 찾을 수 없습니다: " + id);
        }

        alertRuleMapper.deleteAlert(id);

        log.info("✅ [AlertService] 알림 규칙 삭제 완료 - ID: {}", id);
    }

    /**
     * 알림 규칙 단건 조회
     *
     * @param id 알림 규칙 ID
     * @return 알림 규칙 응답 DTO
     */
    @Transactional(readOnly = true)
    public AlertRuleResponseDTO getAlertRule(Long id) {
        AlertRuleVO vo = alertRuleMapper.selectAlertById(id);
        if (vo == null) {
            throw new IllegalArgumentException("알림 규칙을 찾을 수 없습니다: " + id);
        }

        return alertConverter.toResponseDTO(vo);
    }

    /**
     * 모든 알림 규칙 조회
     *
     * @return 알림 규칙 리스트
     */
    @Transactional(readOnly = true)
    public List<AlertRuleResponseDTO> getAllAlertRules() {
        return alertRuleMapper.selectAllAlerts().stream()
                .map(alertConverter::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * 활성화된 알림 규칙만 조회
     *
     * @return 활성화된 알림 규칙 리스트
     */
    @Transactional(readOnly = true)
    public List<AlertRuleResponseDTO> getActiveAlertRules() {
        return alertRuleMapper.selectActiveAlerts().stream()
                .map(alertConverter::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * 애플리케이션별 알림 규칙 조회
     *
     * @param application 애플리케이션 이름
     * @return 알림 규칙 리스트
     */
    @Transactional(readOnly = true)
    public List<AlertRuleResponseDTO> getAlertRulesByApplication(String application) {
        return alertRuleMapper.selectAlertsByApplication(application).stream()
                .map(alertConverter::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // 📌 알림 히스토리 조회
    // ========================================================================

    /**
     * 최근 알림 히스토리 조회 (페이징)
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 알림 히스토리 리스트
     */
    @Transactional(readOnly = true)
    public List<AlertHistoryResponseDTO> getRecentHistory(int page, int size) {
        int offset = page * size;
        return alertHistoryMapper.selectRecentHistory(offset, size).stream()
                .map(alertConverter::toHistoryResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * 특정 알림 규칙의 히스토리 조회
     *
     * @param alertRuleId 알림 규칙 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 알림 히스토리 리스트
     */
    @Transactional(readOnly = true)
    public List<AlertHistoryResponseDTO> getHistoryByRule(Long alertRuleId, int page, int size) {
        int offset = page * size;
        return alertHistoryMapper.selectHistoryByAlertId(alertRuleId, offset, size).stream()
                .map(alertConverter::toHistoryResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * 미해결 알림 조회
     *
     * @return 미해결 알림 리스트
     */
    @Transactional(readOnly = true)
    public List<AlertHistoryResponseDTO> getUnresolvedAlerts() {
        return alertHistoryMapper.selectUnresolvedHistory().stream()
                .map(alertConverter::toHistoryResponseDTO)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // 📌 알림 토글 (활성화/비활성화)
    // ========================================================================

    /**
     * 알림 규칙 활성화/비활성화 토글
     *
     * @param id 알림 규칙 ID
     * @return 토글 후 상태
     */
    @Transactional
    public AlertRuleResponseDTO toggleAlertRule(Long id) {
        log.info("🔄 [AlertService] 알림 규칙 토글 - ID: {}", id);

        AlertRuleVO vo = alertRuleMapper.selectAlertById(id);
        if (vo == null) {
            throw new IllegalArgumentException("알림 규칙을 찾을 수 없습니다: " + id);
        }

        // 토글
        alertRuleMapper.toggleAlert(id);

        log.info("✅ [AlertService] 알림 규칙 토글 완료 - ID: {}, Active: {}", id, !vo.getIsActive());

        // 다시 조회 후 반환
        AlertRuleVO updated = alertRuleMapper.selectAlertById(id);
        return alertConverter.toResponseDTO(updated);
    }

    // ========================================================================
    // 📌 알림 히스토리 해결 처리
    // ========================================================================

    /**
     * 알림 해결 처리
     *
     * @param historyId 알림 히스토리 ID
     * @param resolveMessage 해결 메시지
     */
    @Transactional
    public void resolveAlert(Long historyId, String resolveMessage) {
        log.info("✅ [AlertService] 알림 해결 처리 - ID: {}", historyId);

        AlertHistoryVO vo = alertHistoryMapper.selectHistoryById(historyId);
        if (vo == null) {
            throw new IllegalArgumentException("알림 히스토리를 찾을 수 없습니다: " + historyId);
        }

        alertHistoryMapper.resolveHistory(historyId, resolveMessage);

        log.info("✅ [AlertService] 알림 해결 완료 - ID: {}", historyId);
    }
}