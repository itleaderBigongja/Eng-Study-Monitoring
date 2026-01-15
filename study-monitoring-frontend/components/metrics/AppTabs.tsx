// components/metrics/AppTabs.tsx
import React from 'react';

export interface AppInfo {
    id: string;
    name: string;
    color: string;
}

interface AppTabsProps {
    apps: AppInfo[];
    selectedApp: string;
    onChange: (appId: string) => void;
    className?: string;
}

/**
 * 애플리케이션 탭 컴포넌트
 *
 * 용도: 여러 애플리케이션 간 전환
 * - Eng-Study
 * - Monitoring
 * - Postgres
 * - Elasticsearch
 *
 * @param apps - 애플리케이션 목록
 * @param selectedApp - 현재 선택된 애플리케이션 ID
 * @param onChange - 탭 변경 핸들러
 * @param className - 추가 CSS 클래스
 */
export default function AppTabs({
                                    apps,
                                    selectedApp,
                                    onChange,
                                    className = ''
                                }: AppTabsProps) {
    return (
        <div className={`flex gap-2 border-b border-gray-200 ${className}`}>
            {apps.map((app) => {
                const isSelected = selectedApp === app.id;

                return (
                    <button
                        key={app.id}
                        onClick={() => onChange(app.id)}
                        className={`
                            px-6 py-3 font-medium transition-all relative
                            ${isSelected
                            ? 'text-blue-600 border-b-2 border-blue-600'
                            : 'text-gray-600 hover:text-gray-800 hover:bg-gray-50'
                        }
                        `}
                    >
                        <span className="flex items-center gap-2">
                            {/* 앱 색상 인디케이터 */}
                            <span
                                className={`w-2 h-2 rounded-full ${
                                    isSelected ? `bg-${app.color}-500` : 'bg-gray-400'
                                }`}
                                style={{
                                    backgroundColor: isSelected
                                        ? getColorValue(app.color)
                                        : '#9ca3af'
                                }}
                            />
                            {app.name}
                        </span>

                        {/* 선택된 탭 하단 라인 */}
                        {isSelected && (
                            <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600" />
                        )}
                    </button>
                );
            })}
        </div>
    );
}

/**
 * 색상 이름을 HEX 값으로 변환
 *
 * @param color - 색상 이름 (blue, green, purple, orange)
 * @returns HEX 색상 코드
 */
function getColorValue(color: string): string {
    const colors: Record<string, string> = {
        blue: '#3b82f6',
        green: '#10b981',
        purple: '#8b5cf6',
        orange: '#f97316',
    };

    return colors[color] || '#3b82f6';
}

/**
 * 기본 애플리케이션 목록
 */
export const DEFAULT_APPS: AppInfo[] = [
    { id: 'eng-study', name: 'Eng-Study', color: 'blue' },
    { id: 'monitoring', name: 'Monitoring', color: 'purple' },
    { id: 'postgres', name: 'Postgres', color: 'green' },
    { id: 'elasticsearch', name: 'Elasticsearch', color: 'orange' },
];