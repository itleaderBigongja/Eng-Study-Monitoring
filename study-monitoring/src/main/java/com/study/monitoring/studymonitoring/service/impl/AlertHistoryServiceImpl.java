package com.study.monitoring.studymonitoring.service.impl;

import com.study.monitoring.studymonitoring.converter.AlertConverter;
import com.study.monitoring.studymonitoring.mapper.AlertHistoryMapper;
import com.study.monitoring.studymonitoring.model.dto.response.AlertHistoryResponseDTO;
import com.study.monitoring.studymonitoring.model.vo.AlertHistoryVO;
import com.study.monitoring.studymonitoring.service.AlertHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertHistoryServiceImpl implements AlertHistoryService {

    private final AlertHistoryMapper alertHistoryMapper;
    private final AlertConverter alertConverter;

    @Override
    @Transactional(readOnly = true)
    public List<AlertHistoryResponseDTO> getRecentHistory(int page, int size) {
        int offset = (page - 1) * size;
        List<AlertHistoryVO> historyList = alertHistoryMapper.selectRecentHistory(offset, size);
        return convertToDTOList(historyList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertHistoryResponseDTO> getHistoryByRuleId(Long alertRuleId, int page, int size) {
        int offset = (page - 1) * size;
        List<AlertHistoryVO> historyList = alertHistoryMapper.selectHistoryByAlertId(alertRuleId, offset, size);
        return convertToDTOList(historyList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertHistoryResponseDTO> getUnresolvedAlerts() {
        List<AlertHistoryVO> historyList = alertHistoryMapper.selectUnresolvedHistory();
        return convertToDTOList(historyList);
    }

    @Override
    @Transactional
    public boolean resolveAlert(Long historyId, String message) {
        int updatedRows = alertHistoryMapper.resolveHistory(historyId, message);
        if (updatedRows > 0) {
            log.info("✅ 알림 해결 처리 완료 (ID: {}, Msg: {})", historyId, message);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public int resolveAllByRuleId(Long alertRuleId, String message) {
        int count = alertHistoryMapper.resolveAllByAlertId(alertRuleId, message);
        log.info("✅ 알림 일괄 해결 처리 완료 (RuleID: {}, Count: {})", alertRuleId, count);
        return count;
    }

    // VO 리스트 -> DTO 리스트 변환 헬퍼 메서드
    private List<AlertHistoryResponseDTO> convertToDTOList(List<AlertHistoryVO> voList) {
        return voList.stream()
                .map(alertConverter::toHistoryResponseDTO)
                .collect(Collectors.toList());
    }
}