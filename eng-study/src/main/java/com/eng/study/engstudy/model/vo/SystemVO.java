package com.eng.study.engstudy.model.vo;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public class SystemVO {
    private LocalDateTime createdAt;
    private String createdId;
    private LocalDateTime updatedAt;
    private String updatedId;
}
