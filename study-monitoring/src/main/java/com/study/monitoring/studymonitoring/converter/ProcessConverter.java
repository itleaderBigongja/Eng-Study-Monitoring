package com.study.monitoring.studymonitoring.converter;

import com.study.monitoring.studymonitoring.model.dto.response.DashboardResponseDTO;
import com.study.monitoring.studymonitoring.model.vo.ProcessVO;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProcessConverter {

    private static final DateTimeFormatter DATE_FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * ProcessVO -> ProcessStatusDTO 변환
     *
     * @param vo ProcessVO
     * @return  ProcessStatusDTO
     **/
    public DashboardResponseDTO.ProcessStatusDTO toDTO(ProcessVO vo) {
        if (vo == null) {
            return null;
        }

        DashboardResponseDTO.ProcessStatusDTO dto = new DashboardResponseDTO.ProcessStatusDTO();
        dto.setProcessId(vo.getProcessId());
        dto.setProcessName(vo.getProcessName());
        dto.setProcessType(vo.getProcessType());
        dto.setStatus(vo.getStatus());

        // BigDecimal → Double 변환
        dto.setCpuUsage(vo.getCpuUsage() != null ? vo.getCpuUsage().doubleValue() : 0.0);
        dto.setMemoryUsage(vo.getMemoryUsage() != null ? vo.getMemoryUsage().doubleValue() : 0.0);

        // Uptime 포맷팅 (초 → "1h 30m" 형식)
        dto.setUptime(formatUptime(vo.getUptimeSeconds()));

        // 날짜 포맷팅
        dto.setLastHealthCheck(vo.getLastHealthCheck() != null
                ? vo.getLastHealthCheck().format(DATE_FORMATTER)
                : null);

        return dto;
    }

    /**
     * ProcessVO 리스트 → ProcessStatusDTO 리스트 변환
     *
     * @param voList ProcessVO 리스트
     * @return ProcessStatusDTO 리스트
     */
    public List<DashboardResponseDTO.ProcessStatusDTO> toDTOList(List<ProcessVO> voList) {
        if (voList == null) {
            return List.of();
        }

        return voList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * ProcessVO를 Map으로 변환
     **/
    public Map<String, Object> convertProcessToMap(ProcessVO processVO) {
        Map<String, Object> map = new HashMap<>();

        map.put("processId", processVO.getProcessId());
        map.put("processName", processVO.getProcessName());
        map.put("processType", processVO.getProcessType());
        map.put("status", processVO.getStatus());
        map.put("cpuUsage", processVO.getCpuUsage() != null ? processVO.getCpuUsage().doubleValue() : 0.0);
        map.put("memoryUsage", processVO.getMemoryUsage() != null ? processVO.getMemoryUsage().doubleValue() : 0.0);
        map.put("uptime", formatUptime(processVO.getUptimeSeconds()));
        map.put("lastHealthCheck", processVO.getLastHealthCheck() != null
                ? processVO.getLastHealthCheck().format(DATE_FORMATTER)
                : null);

        return map;
    }

    /**
     * Elasticsearch 에러를 Map으로 변환
     **/
    public Map<String, Object> convertElasticErrorToMap(Map<String, Object> error) {
        Map<String, Object> map = new HashMap<>();

        map.put("id", error.get("id"));
        map.put("timestamp", error.get("@timestamp"));
        map.put("logLevel", error.get("log_level"));
        map.put("message", error.get("message"));
        map.put("application", error.get("application"));

        return map;
    }

    /**
     * Uptime 포맷팅 헬퍼
     *
     * @param seconds 초 단위 시간
     * @return "1h 30m" 형식
     */
    private String formatUptime(Long seconds) {
        if (seconds == null || seconds == 0) {
            return "0m";
        }

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
}
