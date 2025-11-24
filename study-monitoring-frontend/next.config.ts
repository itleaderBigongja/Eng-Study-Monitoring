/** @type {import('next').NextConfig} */
const nextConfig = {
    output: 'standalone',
    reactStrictMode: true,

    env: {
        NEXT_PUBLIC_MONITORING_API: process.env.NEXT_PUBLIC_MONITORING_API || 'http://localhost:8081/api',
        NEXT_PUBLIC_PROMETHEUS_URL: process.env.NEXT_PUBLIC_PROMETHEUS_URL || 'http://localhost:9090',
        NEXT_PUBLIC_ELASTICSEARCH_URL: process.env.NEXT_PUBLIC_ELASTICSEARCH_URL || 'http://localhost:9200',
    },

    images: {
        remotePatterns: [
            {
                protocol: 'http',
                hostname: 'localhost',
            },
            {
                protocol: 'http',
                hostname: 'nginx-service',
            },
        ],
    },
}

module.exports = nextConfig