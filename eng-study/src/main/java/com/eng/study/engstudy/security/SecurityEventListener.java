package com.eng.study.engstudy.security;

import com.eng.study.engstudy.util.SecurityEventLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Spring Security 이벤트를 감지하여 구조화된 보안 로그 생성 */
@Component
public class SecurityEventListener {

    /**
     * 로그인 실패 이벤트 처리
     */
    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        String sourceIp = getClientIp();

        SecurityEventLogger.logLoginFailure(username, sourceIp, 1);
    }

    /**
     * 접근 거부 이벤트 처리
     */
    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        String username = event.getAuthentication().get().getName();
        String resource = event.getAuthorizationDecision().toString();
        String sourceIp = getClientIp();

        SecurityEventLogger.logAccessDenied(username, resource, sourceIp);
    }

    /**
     * 클라이언트 IP 추출
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            // IP 추출 실패 시 무시
        }
        return "unknown";
    }
}
