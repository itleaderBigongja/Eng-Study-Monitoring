package com.study.monitoring.studymonitoring.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 클라이언트 설정
 *
 * 역할:
 * - Elasticsearch 8.11 버전의 Java 클라이언트 구성
 * - 로그 데이터 검색 및 집계를 위한 연결 설정
 *
 * 사용처:
 * - ElasticsearchService에서 로그 검색
 * - 인덱스 관리 및 데이터 조회
 */
@Configuration
public class ElasticsearchConfig {

    // application.yml에서 Elasticsearch 호스트 읽기
    @Value("${elasticsearch.host}")
    private String host;

    // application.yml에서 Elasticsearch 포트 읽기
    @Value("${elasticsearch.port}")
    private int port;

    /**
     * Elasticsearch 클라이언트 Bean 생성
     *
     * @return ElasticsearchClient 인스턴스
     */
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 1. REST 클라이언트 생성 (HTTP 연결 담당)
        RestClient restClient = RestClient.builder(
                new HttpHost(host, port, "http")  // http://elasticsearch-service:9200
        ).build();

        // 2. Transport 레이어 생성 (JSON 변환 담당)
        ElasticsearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()  // Jackson을 사용한 JSON 매핑
        );

        // 3. Elasticsearch 클라이언트 생성 및 반환
        return new ElasticsearchClient(transport);
    }
}