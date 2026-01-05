package com.eng.study.engstudy.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Logger accessLogger = LoggerFactory.getLogger("com.eng.study.access");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // ✅ 템플릿 구조에 맞춰 http 객체 구성
        Map<String, Object> httpData = new HashMap<>();
        httpData.put("method", request.getMethod());
        httpData.put("url", request.getRequestURI());
        httpData.put("status_code", response.getStatus());
        httpData.put("response_time_ms", executionTime);  // ✅ 템플릿 필드명

        Map<String, Object> clientData = new HashMap<>();
        clientData.put("ip", getClientIp(request));
        clientData.put("user_agent", request.getHeader("User-Agent"));

        Map<String, Object> accessData = new HashMap<>();
        accessData.put("http", httpData);
        accessData.put("client", clientData);

        accessLogger.info(Markers.appendEntries(accessData), "API Request");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();
        return ip;
    }
}