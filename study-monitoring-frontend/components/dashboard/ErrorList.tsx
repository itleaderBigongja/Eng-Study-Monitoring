// 경로 : /Monitering/study-monitoring-frontend/components/dashboard/ErrorList.tsx
import {
    AlertTriangle, XCircle, AlertCircle, Info,
    Database, Server, Box, AlertOctagon // [변경] Critical용 아이콘 추가
} from 'lucide-react';

interface ErrorItem {
    id: string;
    timestamp: string;
    level: 'critical' | 'error' | 'warning' | 'info';
    message: string;
    source?: string;
    count?: number;
}

interface ErrorListProps {
    errors: ErrorItem[];
}

export default function ErrorList({ errors }: ErrorListProps) {

    // 1. 로그 레벨 아이콘 (Critical 변경)
    const getLevelIcon = (level: ErrorItem['level']) => {
        switch (level) {
            case 'critical':
                // [변경] 보라색 + 번개/8각형 아이콘으로 변경하여 심각성 강조
                return <AlertOctagon className="w-5 h-5 text-purple-600" />;
            case 'error':
                // [변경] Error는 빨간색이 국룰 (기존 주황 -> 빨강)
                return <XCircle className="w-5 h-5 text-red-500" />;
            case 'warning':
                return <AlertTriangle className="w-5 h-5 text-orange-500" />; // Warning은 주황/노랑
            case 'info':
                return <Info className="w-5 h-5 text-blue-500" />;
            default:
                return <AlertCircle className="w-5 h-5 text-gray-400" />;
        }
    };

    // 2. 로그 레벨 배경색 & 테두리 (Critical 강조)
    const getLevelColor = (level: ErrorItem['level']) => {
        switch (level) {
            case 'critical':
                // [변경] 보라색 배경 + 쉐도우 + 테두리 강조
                return 'bg-purple-50 border-purple-200 hover:bg-purple-100 shadow-[0_0_10px_rgba(147,51,234,0.15)]';
            case 'error':
                return 'bg-red-50 border-red-100 hover:bg-red-100/50';
            case 'warning':
                return 'bg-orange-50 border-orange-100 hover:bg-orange-100/50';
            case 'info':
                return 'bg-blue-50 border-blue-100 hover:bg-blue-100/50';
            default:
                return 'bg-gray-50 border-gray-100 hover:bg-gray-100';
        }
    };

    // 3. 로그 레벨 뱃지 (색상 통일)
    const getLevelBadge = (level: ErrorItem['level']) => {
        const colors = {
            critical: 'bg-purple-100 text-purple-700 ring-1 ring-purple-400/30', // [변경] Ring 효과 추가
            error: 'bg-red-100 text-red-700',
            warning: 'bg-orange-100 text-orange-800',
            info: 'bg-blue-100 text-blue-700',
        };

        return (
            <span className={`px-2 py-0.5 rounded text-[11px] font-bold uppercase tracking-wide flex items-center ${colors[level] || 'bg-gray-100 text-gray-600'}`}>
                {/* Critical일 때만 깜빡이는 점 추가 */}
                {level === 'critical' && (
                    <span className="flex h-2 w-2 mr-1.5 relative">
                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-purple-400 opacity-75"></span>
                        <span className="relative inline-flex rounded-full h-2 w-2 bg-purple-500"></span>
                    </span>
                )}
                {level}
            </span>
        );
    };

    // 소스(Application) 아이콘 결정 (기존 유지)
    const getSourceIcon = (sourceName: string = '') => {
        const name = sourceName.toLowerCase();
        if (name.includes('postgres') || name.includes('mysql') || name.includes('db')) {
            return <Database className="w-3 h-3 mr-1" />;
        }
        if (name.includes('elastic') || name.includes('search')) {
            return <Database className="w-3 h-3 mr-1" />;
        }
        if (name.includes('system')) {
            return <Server className="w-3 h-3 mr-1" />;
        }
        return <Box className="w-3 h-3 mr-1" />;
    };

    const formatTimestamp = (timestamp: string) => {
        try {
            const date = new Date(timestamp);
            return date.toLocaleString('ko-KR', {
                month: '2-digit', day: '2-digit',
                hour: '2-digit', minute: '2-digit', second: '2-digit',
            });
        } catch { return timestamp; }
    };

    return (
        <div className="space-y-3">
            {errors.map((error) => (
                <div
                    key={error.id}
                    // [변경] transition 추가하여 호버 효과 부드럽게
                    className={`p-3 rounded-lg border transition-all duration-300 ${getLevelColor(error.level)}`}
                >
                    <div className="flex items-start space-x-3">
                        {/* 좌측 아이콘 */}
                        <div className="flex-shrink-0 mt-0.5">
                            {getLevelIcon(error.level)}
                        </div>

                        {/* 우측 내용 */}
                        <div className="flex-1 min-w-0">
                            {/* 헤더: 뱃지들 + 시간 */}
                            <div className="flex items-center justify-between mb-1.5 flex-wrap gap-2">
                                <div className="flex items-center space-x-2">
                                    {/* 1. 레벨 뱃지 */}
                                    {getLevelBadge(error.level)}

                                    {/* 2. 소스 뱃지 */}
                                    {error.source && (
                                        <span className="flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-white/80 border border-gray-200 text-gray-600 shadow-sm backdrop-blur-sm">
                                            {getSourceIcon(error.source)}
                                            {error.source}
                                        </span>
                                    )}
                                </div>

                                {/* 시간 */}
                                <span className="text-xs text-gray-500 font-mono">
                                    {formatTimestamp(error.timestamp)}
                                </span>
                            </div>

                            {/* 에러 메시지 */}
                            <p className={`text-sm break-all leading-relaxed ${
                                // Critical일 경우 텍스트를 좀 더 진하게
                                error.level === 'critical' ? 'text-gray-900 font-medium' : 'text-gray-800'
                            }`}>
                                {error.message}
                            </p>

                            {/* 반복 횟수 */}
                            {error.count && error.count > 1 && (
                                <div className="mt-2 inline-flex items-center px-2 py-0.5 bg-white/60 rounded text-xs font-medium text-gray-500 border border-gray-100">
                                    <span className="mr-1">🔁</span>
                                    {error.count}회 반복 발생
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            ))}
        </div>
    );
}