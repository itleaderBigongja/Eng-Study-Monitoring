package com.study.monitoring.studymonitoring.model.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * API 공통 응답 DTO
 * ============================================================================
 *
 * 역할:
 * - 모든 API 응답의 공통 포맷 정의
 * - 성공/실패 상태, 데이터, 메시지 포함
 *
 * 응답 예시:
 * {
 *   "success": true,
 *   "message": "조회 성공",
 *   "data": {...},
 *   "timestamp": "2026-01-18T14:30:00"
 * }
 *
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponseDTO<T> {

    /**
     * 성공 여부
     * true: 성공
     * false: 실패
     */
    private boolean success;

    /**
     * 응답 메시지
     * 예: "조회 성공", "알림 규칙 생성 완료", "잘못된 요청입니다"
     */
    private String message;

    /**
     * 응답 데이터 (Generic 타입)
     * 성공 시: 실제 데이터
     * 실패 시: null 또는 에러 상세 정보
     */
    private T data;

    /**
     * 응답 시간
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 에러 코드 (선택사항)
     * 예: "VALIDATION_ERROR", "NOT_FOUND", "INTERNAL_ERROR"
     */
    private String errorCode;

    /**
     * 에러 상세 정보 (선택사항)
     * 개발 환경에서 디버깅용
     */
    private String errorDetails;

    // ========================================================================
    // 정적 팩토리 메서드
    // ========================================================================

    /**
     * 성공 응답 생성 (데이터 포함)
     *
     * @param data 응답 데이터
     * @param <T> 데이터 타입
     * @return 성공 응답 DTO
     */
    public static <T> ApiResponseDTO<T> success(T data) {
        return ApiResponseDTO.<T>builder()
                .success(true)
                .message("성공")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 성공 응답 생성 (데이터 + 메시지)
     *
     * @param data 응답 데이터
     * @param message 성공 메시지
     * @param <T> 데이터 타입
     * @return 성공 응답 DTO
     */
    public static <T> ApiResponseDTO<T> success(T data, String message) {
        return ApiResponseDTO.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     *
     * @param message 성공 메시지
     * @param <T> 데이터 타입
     * @return 성공 응답 DTO
     */
    public static <T> ApiResponseDTO<T> success(String message) {
        return ApiResponseDTO.<T>builder()
                .success(true)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 응답 생성 (메시지만)
     *
     * @param message 실패 메시지
     * @param <T> 데이터 타입
     * @return 실패 응답 DTO
     */
    public static <T> ApiResponseDTO<T> fail(String message) {
        return ApiResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 응답 생성 (메시지 + 에러 코드)
     *
     * @param message 실패 메시지
     * @param errorCode 에러 코드
     * @param <T> 데이터 타입
     * @return 실패 응답 DTO
     */
    public static <T> ApiResponseDTO<T> fail(String message, String errorCode) {
        return ApiResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 응답 생성 (전체 정보)
     *
     * @param message 실패 메시지
     * @param errorCode 에러 코드
     * @param errorDetails 에러 상세 정보
     * @param <T> 데이터 타입
     * @return 실패 응답 DTO
     */
    public static <T> ApiResponseDTO<T> fail(String message, String errorCode, String errorDetails) {
        return ApiResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 유효성 검증 실패 응답
     *
     * @param message 검증 실패 메시지
     * @param <T> 데이터 타입
     * @return 실패 응답 DTO
     */
    public static <T> ApiResponseDTO<T> validationFail(String message) {
        return ApiResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .errorCode("VALIDATION_ERROR")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 리소스를 찾을 수 없음
     *
     * @param message 실패 메시지
     * @param <T> 데이터 타입
     * @return 실패 응답 DTO
     */
    public static <T> ApiResponseDTO<T> notFound(String message) {
        return ApiResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .errorCode("NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 내부 서버 오류
     *
     * @param <T> 데이터 타입
     * @return 실패 응답 DTO
     */
    public static <T> ApiResponseDTO<T> internalError() {
        return ApiResponseDTO.<T>builder()
                .success(false)
                .message("서버 내부 오류가 발생했습니다")
                .errorCode("INTERNAL_ERROR")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 내부 서버 오류 (메시지 커스터마이징)
     *
     * @param message 에러 메시지
     * @param <T> 데이터 타입
     * @return 실패 응답 DTO
     */
    public static <T> ApiResponseDTO<T> internalError(String message) {
        return ApiResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .errorCode("INTERNAL_ERROR")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 인증 실패
     *
     * @param message 실패 메시지
     * @param <T> 데이터 타입
     * @return 실패 응답 DTO
     */
    public static <T> ApiResponseDTO<T> unauthorized(String message) {
        return ApiResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .errorCode("UNAUTHORIZED")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 권한 없음
     *
     * @param message 실패 메시지
     * @param <T> 데이터 타입
     * @return 실패 응답 DTO
     */
    public static <T> ApiResponseDTO<T> forbidden(String message) {
        return ApiResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .errorCode("FORBIDDEN")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public String toString() {
        return "ApiResponseDTO{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}