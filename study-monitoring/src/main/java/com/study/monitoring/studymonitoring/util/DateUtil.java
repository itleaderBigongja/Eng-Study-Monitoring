package com.study.monitoring.studymonitoring.util;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 날짜/시간 유틸리티 클래스
 **/
public class DateUtil {

    private static final DateTimeFormatter DEFAULT_FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter ISO_FORMATTER
            = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * LocalDateTime을 문자열로 변환
     *
     * @param dateTime LocalDateTime
     * @return "yyyy-MM-dd HH:mm:ss" 형식 문자열
     */
    public static String format(LocalDateTime dateTime) {
        if(dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_FORMATTER);
    }

    /**
     * Unix timestamp를 LocalDateTime으로 변환
     *
     * @param timestamp Unix timestamp(초)
     * @return LocalDateTime
     **/
    public static LocalDateTime fromTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()
        );
    }

    /**
     * 현재 시간에서 N시간 전 timestamp 계산
     *
     * @param hours 시간 수
     * @return Unix timestamp(초)
     */
    public static long getTimestamp(int hours){
        return Instant.now()
                .minus(hours, ChronoUnit.HOURS)
                .getEpochSecond();
    }

    /**
     * Uptime 포맷팅(초 -> "1h 30m" 형식)
     *
     * @param seconds 초 단위 시간
     * @return 포맷된 문자열
     **/
    public static String formatUptime(Long seconds) {
        if(seconds == null || seconds == 0){
            return "0m";
        }

        long hours = seconds / 3600;
        long minuts = (seconds % 3600) / 60;

        if(hours > 0) {
            return String.format("%d %d", hours, minuts);
        }else {
            return String.format("%dm", minuts);
        }
    }
}
