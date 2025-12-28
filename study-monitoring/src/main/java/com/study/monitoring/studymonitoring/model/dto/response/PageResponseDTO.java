package com.study.monitoring.studymonitoring.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDTO<T> {
    private List<T> content;            // 실제 데이터 목록
    private long totalElements;         // 전체 데이터 개수
    private int totalPages;             // 전체 페이지 수
    private int currentPage;            // 현재 페이지 번호
    private int size;
}
