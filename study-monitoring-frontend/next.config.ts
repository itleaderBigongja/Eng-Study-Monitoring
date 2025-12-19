// ===================================
// next.config.ts
// ===================================
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
    output: 'standalone',  // Docker 배포용
    reactStrictMode: true,

    // 환경 변수
    env: {
        NEXT_PUBLIC_MONITORING_API: process.env.NEXT_PUBLIC_MONITORING_API ||
            'http://monitoring-backend-service.monitoring.svc.cluster.local:8081/api',
        NEXT_PUBLIC_PROMETHEUS_URL: process.env.NEXT_PUBLIC_PROMETHEUS_URL ||
            'http://prometheus-service.monitoring.svc.cluster.local:9090',
        NEXT_PUBLIC_ELASTICSEARCH_URL: process.env.NEXT_PUBLIC_ELASTICSEARCH_URL ||
            'http://elasticsearch-service.monitoring.svc.cluster.local:9200',
    },

    // 이미지 최적화 도메인
    images: {
        remotePatterns: [
            {
                protocol: 'http',
                hostname: 'localhost',
            },
            {
                protocol: 'http',
                hostname: 'monitoring-backend-service',
            },
        ],
    },

    // 리다이렉트 설정
    async redirects() {
        return [
            {
                source: '/monitoring',
                destination: '/monitoring/',
                permanent: true,
            },
        ];
    },
};

export default nextConfig;