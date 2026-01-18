package com.study.monitoring.studymonitoring.service;

public interface NotificationService {
    /**
     * 알림 메시지를 전송합니다.
     * @param message 전송할 메시지 내용
     * @return 전송 성공 여부
     */
    boolean sendNotification(String message);
}
