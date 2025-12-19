package com.study.monitoring.studymonitoring.model.dto.response;

import lombok.*;

/**
 * 공통 API 응답 DTO
 *
 * 사용처:
 * - 모든 API 응답의 표준 포맷
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDTO<T> {

    private Boolean success;            // 성공 여부
    private String message;             // 응답 메시지
    private T data;                     // 응답 데이터

    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponseDTO<T> success(T data) {
        return new ApiResponseDTO<>(true, "요청이 성공했습니다", data);
    }

    /**
     * 성공 응답 생성 (메시지 커스텀)
     */
    public static <T> ApiResponseDTO<T> success(String message, T data) {
        return new ApiResponseDTO<>(true, message, data);
    }

    /**
     * 실패 응답 생성
     */
    public static <T> ApiResponseDTO<T> fail(String message) {
        return new ApiResponseDTO<>(false, message, null);
    }
}
