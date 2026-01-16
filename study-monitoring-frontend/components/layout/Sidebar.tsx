'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
    LayoutDashboard,
    Activity,
    BarChart3,
    FileText,
    Heart,
    ChevronRight,
    Terminal,
} from 'lucide-react';

const menuItems = [
    {
        title: '대시보드',
        href: '/dashboard',
        icon: LayoutDashboard,
    },
    {
        title: '메트릭',
        href: '/metrics',
        icon: Activity,
    },
    {
        title: "커스텀 쿼리",
        href: '/metrics/custom-query',
        icon: Terminal,
    },
    {
        title: '통계',
        href: '/statistics',
        icon: BarChart3,
        subItems: [
            { title: '시계열', href: '/statistics/timeseries' },
            { title: '로그 통계', href: '/statistics/logs' },
            { title: '접근 로그', href: '/statistics/access-logs' },
            { title: '에러 로그', href: '/statistics/error-logs' },
            { title: '성능 메트릭', href: '/statistics/performance-metrics' },
            { title: 'DB 로그', href: '/statistics/database-logs' },
            { title: '감사 로그', href: '/statistics/audit-logs' },
            { title: '보안 로그', href: '/statistics/security-logs' },
        ],
    },
    {
        title: '로그',
        href: '/logs',
        icon: FileText,
    },
    {
        title: '헬스체크',
        href: '/health',
        icon: Heart,
    },
];

export default function Sidebar() {
    const pathname = usePathname();

    return (
        <aside className="w-64 bg-white shadow-sky border-r border-primary-100 h-screen sticky top-16 overflow-y-auto">
            <nav className="p-4 space-y-2">
                {menuItems.map((item) => (
                    <div key={item.href}>
                        <SidebarItem
                            item={item}
                            isActive={
                                item.subItems
                                    ? pathname.startsWith(item.href) // 서브메뉴가 있으면 하위 경로도 활성화 인정
                                    : pathname === item.href         // 서브메뉴가 없으면 정확히 일치해야 함
                            }
                        />

                        {/* 서브 메뉴 */}
                        {item.subItems && pathname.startsWith(item.href) && (
                            <div className="ml-6 mt-2 space-y-1 fade-in">
                                {item.subItems.map((subItem) => (
                                    <Link
                                        key={subItem.href}
                                        href={subItem.href}
                                        className={`block px-3 py-2 rounded-lg text-sm transition-colors ${
                                            pathname === subItem.href
                                                ? 'bg-primary-100 text-primary-700 font-medium'
                                                : 'text-secondary-600 hover:bg-primary-50 hover:text-primary-600'
                                        }`}
                                    >
                                        {subItem.title}
                                    </Link>
                                ))}
                            </div>
                        )}
                    </div>
                ))}
            </nav>
        </aside>
    );
}

function SidebarItem({
                         item,
                         isActive,
                     }: {
    item: typeof menuItems[0];
    isActive: boolean;
}) {
    const Icon = item.icon;

    return (
        <Link
            href={item.href}
            className={`flex items-center justify-between px-4 py-3 rounded-lg transition-colors ${
                isActive
                    ? 'bg-primary-500 text-white shadow-lg'
                    : 'text-secondary-700 hover:bg-primary-50 hover:text-primary-700'
            }`}
        >
            <div className="flex items-center space-x-3">
                <Icon className="w-5 h-5" />
                <span className="font-medium">{item.title}</span>
            </div>
            {item.subItems && (
                <ChevronRight
                    className={`w-4 h-4 transition-transform ${
                        isActive ? 'rotate-90' : ''
                    }`}
                />
            )}
        </Link>
    );
}