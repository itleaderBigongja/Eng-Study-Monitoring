package com.study.monitoring.studymonitoring.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MetricsQueryRequestDTO {
    @NotBlank(message = "애플리케이션 이름은 필수입니다")
    private String application;

    @NotBlank(message = "메트릭 타입은 필수 입니다")
    private String metric;

    @Min(value = 1, message = "시간 범위는 최소 1시간입니다.")
    @Max(value = 168, message = "시간 범위는 최대 168시간(7일)입니다")
    private Integer hours = 1;  // 기본 1시간
}
