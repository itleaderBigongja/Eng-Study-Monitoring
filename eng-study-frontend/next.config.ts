/** @type {import('next').NextConfig} */
const nextConfig = {
    output: 'standalone', // Docker 최적화
    reactStrictMode: true,
    env: {
        NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL,
    },
}

module.exports = nextConfig