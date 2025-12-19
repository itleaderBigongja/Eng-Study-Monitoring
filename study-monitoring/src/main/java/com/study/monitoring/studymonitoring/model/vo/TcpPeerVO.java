package com.study.monitoring.studymonitoring.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TCP/Peer 실시간 감시 VO
 *
 * 테이블: MONITORING_TCP_PEER
 *
 * 설명:
 * - TCP 연결 상태 모니터링
 * - 송수신 패킷/바이트 통계
 * - 재전송 횟수 및 지연 시간
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TcpPeerVO {

    private Long tcpPeerId;                // TCP Peer ID (PK)
    private Long processId;                // 프로세스 ID (FK)
    private String sourceIp;               // 소스 IP
    private Integer sourcePort;            // 소스 포트
    private String destinationIp;          // 목적지 IP
    private Integer destinationPort;       // 목적지 포트
    private String connectionState;        // 연결 상태 (ESTABLISHED, SYN_SENT, etc)
    private Integer connectionCount;       // 연결 수
    private Integer activeConnections;     // 활성 연결 수
    private Long bytesSent;                // 전송 바이트
    private Long bytesReceived;            // 수신 바이트
    private Long packetsSent;              // 전송 패킷
    private Long packetsReceived;          // 수신 패킷
    private Integer retransmissions;       // 재전송 횟수
    private BigDecimal latencyMs;          // 지연 시간 (ms)
    private Boolean isHealthy;             // 정상 여부
    private LocalDateTime collectedAt;     // 수집 시간
    private LocalDateTime createdAt;       // 생성일시

    /**
     * 연결 상태 Enum
     */
    public enum ConnectionState {
        ESTABLISHED,    // 연결됨
        SYN_SENT,       // SYN 전송됨
        SYN_RECV,       // SYN 수신됨
        FIN_WAIT1,      // FIN 대기 1
        FIN_WAIT2,      // FIN 대기 2
        TIME_WAIT,      // TIME 대기
        CLOSE,          // 닫힘
        CLOSE_WAIT,     // CLOSE 대기
        LAST_ACK,       // 마지막 ACK
        LISTEN,         // 대기 중
        CLOSING         // 닫는 중
    }

    /**
     * 연결이 정상인지 확인
     *
     * @return 정상 여부
     */
    public boolean isConnectionHealthy() {
        return this.isHealthy != null && this.isHealthy;
    }

    /**
     * 재전송이 많은지 확인
     *
     * @param threshold 임계치
     * @return 재전송 많음 여부
     */
    public boolean hasHighRetransmissions(int threshold) {
        return this.retransmissions != null && this.retransmissions >= threshold;
    }

    /**
     * 지연 시간이 높은지 확인
     *
     * @param thresholdMs 임계치 (ms)
     * @return 지연 높음 여부
     */
    public boolean hasHighLatency(BigDecimal thresholdMs) {
        return this.latencyMs != null &&
                this.latencyMs.compareTo(thresholdMs) > 0;
    }

    /**
     * 연결이 확립되어 있는지 확인
     *
     * @return 확립 여부
     */
    public boolean isEstablished() {
        return ConnectionState.ESTABLISHED.name().equals(this.connectionState);
    }
}