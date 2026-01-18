package com.study.monitoring.studymonitoring.service.impl;

import com.study.monitoring.studymonitoring.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationServiceImpl implements NotificationService {

    // application.yml에서 URL을 가져옵니다.
    @Value("${slack.webhook-url}")
    private String webhookUrl;

    // RestTemplate은 빈(Bean)으로 등록되어 있다고 가정하거나, 없으면 생성자에서 new로 만드셔도 됩니다.
    private final RestTemplate restTemplate;

    @Override
    public boolean sendNotification(String message) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.error("❌ Slack Webhook URL이 설정되지 않았습니다.");
            return false;
        }

        try {
            // Slack Webhook은 JSON 포맷의 "text" 필드를 요구합니다.
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", "<!channel> " + message);

            // POST 요청 전송
            restTemplate.postForEntity(webhookUrl, requestBody, String.class);

            log.info("✅ Slack 알림 전송 성공: {}", message);
            return true;

        } catch (Exception e) {
            log.error("❌ Slack 알림 전송 실패: {}", e.getMessage());
            return false;
        }
    }
}