/** @type {import('next').NextConfig} */
const nextConfig = {
    output: 'standalone',   // Docker 최적화
    reactStrictMode: true,

    // 환경 변수 명시적 노출
    env: {
        NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
    },

    // 이미지 최적화 설정
    images: {
        domains: ['localhost'],
    },
}

module.exports = nextConfig