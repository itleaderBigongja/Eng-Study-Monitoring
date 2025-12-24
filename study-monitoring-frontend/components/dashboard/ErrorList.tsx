import { AlertTriangle, XCircle, AlertCircle, Info } from 'lucide-react';
import Card from '@/components/common/Card';

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
    title?: string;
    maxItems?: number;
}

export default function ErrorList({
                                      errors,
                                      title = '최근 에러 로그',
                                      maxItems = 10
                                  }: ErrorListProps) {
    const getLevelIcon = (level: ErrorItem['level']) => {
        switch (level) {
            case 'critical':
                return <XCircle className="w-5 h-5 text-error" />;
            case 'error':
                return <AlertCircle className="w-5 h-5 text-error" />;
            case 'warning':
                return <AlertTriangle className="w-5 h-5 text-warning" />;
            case 'info':
                return <Info className="w-5 h-5 text-primary-500" />;
            default:
                return <AlertCircle className="w-5 h-5 text-secondary-400" />;
        }
    };

    const getLevelColor = (level: ErrorItem['level']) => {
        switch (level) {
            case 'critical':
                return 'bg-error/10 border-error/20';
            case 'error':
                return 'bg-error/5 border-error/10';
            case 'warning':
                return 'bg-warning/10 border-warning/20';
            case 'info':
                return 'bg-primary-50 border-primary-100';
            default:
                return 'bg-secondary-50 border-secondary-100';
        }
    };

    const getLevelBadge = (level: ErrorItem['level']) => {
        const colors = {
            critical: 'bg-error text-white',
            error: 'bg-error/80 text-white',
            warning: 'bg-warning text-white',
            info: 'bg-primary-500 text-white',
        };

        return (
            <span className={`px-2 py-1 rounded text-xs font-semibold uppercase ${colors[level]}`}>
                {level}
            </span>
        );
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

    const displayErrors = errors.slice(0, maxItems);

    return (
        <Card
            title={title}
            subtitle={errors.length > 0 ? `총 ${errors.length}개 (최근 ${displayErrors.length}개 표시)` : undefined}
        >
            {errors.length === 0 ? (
                <div className="text-center py-12">
                    <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-success/10 flex items-center justify-center">
                        <AlertCircle className="w-8 h-8 text-success" />
                    </div>
                    <p className="text-secondary-600 font-medium">에러가 없습니다</p>
                    <p className="text-sm text-secondary-500 mt-1">시스템이 정상적으로 작동 중입니다</p>
                </div>
            ) : (
                <div className="space-y-3">
                    {displayErrors.map((error) => (
                        <div
                            key={error.id}
                            className={`p-4 rounded-lg border transition-all duration-200 hover:shadow-md ${getLevelColor(error.level)}`}
                        >
                            <div className="flex items-start space-x-3">
                                <div className="flex-shrink-0 mt-0.5">
                                    {getLevelIcon(error.level)}
                                </div>

                                <div className="flex-1 min-w-0">
                                    <div className="flex items-start justify-between mb-2">
                                        <div className="flex items-center space-x-2">
                                            {getLevelBadge(error.level)}
                                            {error.source && (
                                                <span className="text-xs text-secondary-600 font-medium">
                                                    {error.source}
                                                </span>
                                            )}
                                        </div>
                                        <span className="text-xs text-secondary-500 whitespace-nowrap ml-2">
                                            {formatTimestamp(error.timestamp)}
                                        </span>
                                    </div>

                                    <p className="text-sm text-gray-900 break-words">
                                        {error.message}
                                    </p>

                                    {error.count && error.count > 1 && (
                                        <div className="mt-2 inline-flex items-center px-2 py-1 bg-white/50 rounded text-xs font-medium text-secondary-700">
                                            <span className="mr-1">🔁</span>
                                            {error.count}회 발생
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    ))}

                    {errors.length > maxItems && (
                        <div className="text-center pt-2">
                            <p className="text-sm text-secondary-500">
                                {errors.length - maxItems}개의 에러가 더 있습니다
                            </p>
                        </div>
                    )}
                </div>
            )}
        </Card>
    );
}