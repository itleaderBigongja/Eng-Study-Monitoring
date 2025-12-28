import {
    AlertTriangle, XCircle, AlertCircle, Info,
    Database, Server, Box // [추가] 소스 구분을 위한 아이콘
} from 'lucide-react';

interface ErrorItem {
    id: string;
    timestamp: string;
    level: 'critical' | 'error' | 'warning' | 'info';
    message: string;
    source?: string; // Application 이름
    count?: number;
}

interface ErrorListProps {
    errors: ErrorItem[];
    // title, maxItems는 부모(DashboardPage)에서 제어하므로 필수 아님
}

export default function ErrorList({ errors }: ErrorListProps) {

    // 로그 레벨 아이콘
    const getLevelIcon = (level: ErrorItem['level']) => {
        switch (level) {
            case 'critical':
                return <XCircle className="w-5 h-5 text-red-500" />;
            case 'error':
                return <AlertCircle className="w-5 h-5 text-orange-500" />;
            case 'warning':
                return <AlertTriangle className="w-5 h-5 text-yellow-500" />;
            case 'info':
                return <Info className="w-5 h-5 text-blue-500" />;
            default:
                return <AlertCircle className="w-5 h-5 text-gray-400" />;
        }
    };

    // 로그 레벨 배경색
    const getLevelColor = (level: ErrorItem['level']) => {
        switch (level) {
            case 'critical':
                return 'bg-red-50 border-red-100 hover:bg-red-100/50';
            case 'error':
                return 'bg-orange-50 border-orange-100 hover:bg-orange-100/50';
            case 'warning':
                return 'bg-yellow-50 border-yellow-100 hover:bg-yellow-100/50';
            case 'info':
                return 'bg-blue-50 border-blue-100 hover:bg-blue-100/50';
            default:
                return 'bg-gray-50 border-gray-100 hover:bg-gray-100';
        }
    };

    // 로그 레벨 텍스트 뱃지
    const getLevelBadge = (level: ErrorItem['level']) => {
        const colors = {
            critical: 'bg-red-100 text-red-700',
            error: 'bg-orange-100 text-orange-700',
            warning: 'bg-yellow-100 text-yellow-800',
            info: 'bg-blue-100 text-blue-700',
        };

        return (
            <span className={`px-2 py-0.5 rounded text-[11px] font-bold uppercase tracking-wide ${colors[level] || 'bg-gray-100 text-gray-600'}`}>
                {level}
            </span>
        );
    };

    // [✨신규] 소스(Application) 아이콘 결정
    const getSourceIcon = (sourceName: string = '') => {
        const name = sourceName.toLowerCase();
        if (name.includes('postgres') || name.includes('mysql') || name.includes('db')) {
            return <Database className="w-3 h-3 mr-1" />;
        }
        if (name.includes('elastic') || name.includes('search')) {
            return <Database className="w-3 h-3 mr-1" />; // ES도 DB 취급 혹은 별도 아이콘
        }
        if (name.includes('system')) {
            return <Server className="w-3 h-3 mr-1" />;
        }
        return <Box className="w-3 h-3 mr-1" />; // 기본 앱
    };

    const formatTimestamp = (timestamp: string) => {
        try {
            const date = new Date(timestamp);
            return date.toLocaleString('ko-KR', {
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
            });
        } catch {
            return timestamp;
        }
    };

    // [변경] Card 컴포넌트 제거 -> 순수 리스트만 반환
    return (
        <div className="space-y-3">
            {errors.map((error) => (
                <div
                    key={error.id}
                    className={`p-3 rounded-lg border transition-all duration-200 ${getLevelColor(error.level)}`}
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

                                    {/* [✨중요] 2. 소스(Application) 뱃지 - 시각적 강조 */}
                                    {error.source && (
                                        <span className="flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-white border border-gray-200 text-gray-600 shadow-sm">
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
                            <p className="text-sm text-gray-800 break-all leading-relaxed">
                                {error.message}
                            </p>

                            {/* 반복 횟수 (있을 경우) */}
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