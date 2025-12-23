import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';

import Header from '@/components/layout/Header';    // components/layout/Header.tsx 컴포넌트를 불러옵니다.
import Footer from '@/components/layout/Footer';    // components/layout/Footer.tsx 컴포넌트를 불러옵니다.
import Sidebar from '@/components/layout/Sidebar';  // components/layout/Sidebar.tsx 컴포넌트를 불러옵니다.

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
    title: 'Study Monitoring System',
    description: '실시간 시스템 모니터링 대시보드',
};

export default function RootLayout({
   children,
}: {
    children: React.ReactNode;
}) {
    return (
        <html lang="ko">
        <body className={`${inter.className} bg-primary-50 text-gray-900`}>
        <div className="min-h-screen flex flex-col">
            <Header />

            {/* 2. 사이드바와 메인 컨텐츠를 감싸는 Flex 컨테이너 추가 */}
            {/* container mx-auto: 중앙 정렬, flex: 좌우 배치 */}
            <div className="flex flex-1 container mx-auto max-w-7xl w-full">

                {/* 왼쪽: 사이드바 */}
                <Sidebar />

                {/* 오른쪽: 메인 컨텐츠 */}
                <main className="flex-1 px-4 py-8 w-full">
                    {children}
                </main>
            </div>
            <Footer />
        </div>
        </body>
        </html>
    );
}