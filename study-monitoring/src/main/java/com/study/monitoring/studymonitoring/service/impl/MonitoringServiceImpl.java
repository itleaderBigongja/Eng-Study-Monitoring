package com.study.monitoring.studymonitoring.service.impl;

import com.study.monitoring.studymonitoring.mapper.ProcessMapper;
import com.study.monitoring.studymonitoring.mapper.RealtimeMapper;
import com.study.monitoring.studymonitoring.mapper.StatisticsMapper;
import com.study.monitoring.studymonitoring.model.vo.ProcessVO;
import com.study.monitoring.studymonitoring.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {

    private final ProcessMapper processMapper;
    private final RealtimeMapper realtimeMapper;
    private final StatisticsMapper statisticsMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProcessVO> getAllProcesses() {
        log.debug("Fetchuing all processes");
        return processMapper.getAllProcesses();
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessVO getProcessById(Long processId) {
        log.debug("Fetching process by id {}", processId);
        return processMapper.getProcessById(processId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getProcessSummary() {
        log.debug("Fetching process summary");
        Map<String, Long> summary = new HashMap<>();
        summary.put("total", processMapper.countAllProcesses());
        summary.put("running", processMapper.countByStatus("RUNNING"));
        summary.put("stopped", processMapper.countByStatus("STOPPED"));
        summary.put("error", processMapper.countByStatus("ERROR"));

        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemStatistics() {
        log.debug("Fetching system statistics");

        // DB에서 통계 데이터 조회
        Map<String, Object> stats = statisticsMapper.getSystemStats();

        // 기본값 설정 (데이터가 없을 경우)
        if (stats == null) {
            stats = new HashMap<>();
            stats.put("totalRequests", 0L);
            stats.put("avgResponseTime", 0.0);
            stats.put("uptime", "N/A");
        }

        return stats;
    }

    @Override
    public List<Map<String, Object>> getRecentAlerts(int limit) {
        log.debug("Fetching recent alerts");
        return realtimeMapper.getRecentAlerts(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean updateProcessStatus(ProcessVO process) {
        log.debug("Updating process status: processId={}, status={}",
                process.getProcessId(), process.getStatus());

        try {
            int updated = processMapper.updateProcessStatus(process);
            return updated > 0;
        } catch (Exception e) {
            log.error("Failed to update process status", e);
            return false;
        }
    }
}
