/** @type {import('next').NextConfig} */
const nextConfig = {
    // 이미지 최적화 도메인 설정
    images: {
        domains: ['localhost'],
    },

    // 환경 변수 노출
    env: {
        API_URL: process.env.NEXT_PUBLIC_API_URL,
    },

    // CORS 프록시 설정 (개발 환경)
    async rewrites() {
        return [
            {
                source: '/api/:path*',
                destination: 'http://localhost:8081/api/:path*',
            },
        ];
    },
};

export default nextConfig;