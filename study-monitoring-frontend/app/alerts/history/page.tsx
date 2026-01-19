// app/alerts/history/page.tsx
'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Card from '@/components/common/Card';
import Button from '@/components/common/Button';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';

// 기존 구조의 API 및 타입 import
import {
    getAlertHistory,
    resolveAlert,
    formatDateTime,
    formatDuration,
    getMetricDisplayName,
    getMetricUnit,
    getSeverityBadgeClass,
    type AlertHistoryResponse,
} from '@/lib/api/alerts';

export default function AlertHistoryPage() {
    const router = useRouter();
    const [histories, setHistories] = useState<AlertHistoryResponse[]>([]);
    const [filteredHistories, setFilteredHistories] = useState<AlertHistoryResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [resolving, setResolving] = useState<number | null>(null);

    // 필터 상태
    const [filterStatus, setFilterStatus] = useState<'all' | 'unresolved' | 'resolved'>('all');
    const [filterApplication, setFilterApplication] = useState<string>('all');
    const [filterSeverity, setFilterSeverity] = useState<string>('all');

    // 페이징 상태
    const [page, setPage] = useState(1);  // ⚠️ 1부터 시작
    const [hasMore, setHasMore] = useState(true);

    useEffect(() => {
        fetchHistories();
    }, [page]);

    useEffect(() => {
        applyFilters();
    }, [filterStatus, filterApplication, filterSeverity, histories]);

    const fetchHistories = async () => {
        try {
            setLoading(true);
            const data = await getAlertHistory(page, 50);

            if (page === 1) {
                setHistories(data);
            } else {
                setHistories(prev => [...prev, ...data]);
            }

            setHasMore(data.length === 50);
        } catch (err: any) {
            setError(err.message || '알림 히스토리를 불러오는데 실패했습니다');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const applyFilters = () => {
        let filtered = [...histories];

        // 해결 상태 필터
        if (filterStatus === 'unresolved') {
            filtered = filtered.filter(h => !h.resolved);
        } else if (filterStatus === 'resolved') {
            filtered = filtered.filter(h => h.resolved);
        }

        // 애플리케이션 필터
        if (filterApplication !== 'all') {
            filtered = filtered.filter(h => h.application === filterApplication);
        }

        // 심각도 필터
        if (filterSeverity !== 'all') {
            filtered = filtered.filter(h => h.severity === filterSeverity);
        }

        setFilteredHistories(filtered);
    };

    const handleResolve = async (historyId: number) => {
        const message = prompt('해결 메시지를 입력하세요:', '수동 해결됨');
        if (!message) return;

        try {
            setResolving(historyId);
            const result = await resolveAlert(historyId, message);

            alert(result); // 서버 메시지 표시

            // 상태 업데이트
            setHistories(prev =>
                prev.map(h =>
                    h.id === historyId
                        ? { ...h, resolved: true, resolvedAt: new Date().toISOString(), resolvedMessage: message }
                        : h
                )
            );
        } catch (err: any) {
            alert(err.message || '알림 해결 처리에 실패했습니다');
            console.error(err);
        } finally {
            setResolving(null);
        }
    };

    const loadMore = () => {
        setPage(prev => prev + 1);
    };

    // 고유 애플리케이션 목록
    const applications = Array.from(new Set(histories.map(h => h.application)));

    if (loading && page === 1) return <Loading />;
    if (error && page === 1) return <ErrorMessage message={error} />;

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex justify-between items-center">
                <div>
                    <div className="flex items-center space-x-4">
                        <Button
                            onClick={() => router.back()}
                            className="bg-gray-100 text-gray-700 hover:bg-gray-200"
                        >
                            ← 뒤로 가기
                        </Button>
                        <h1 className="text-3xl font-bold text-gray-900">알림 발생 이력</h1>
                    </div>
                    <p className="mt-1 text-sm text-gray-500">
                        시스템 알림 발생 기록을 확인하고 관리합니다
                    </p>
                </div>
                <Button onClick={() => { setPage(1); fetchHistories(); }}>
                    🔄 새로고침
                </Button>
            </div>

            {/* 통계 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <Card className="bg-gradient-to-br from-blue-50 to-blue-100">
                    <div className="text-sm font-medium text-blue-600">전체 알림</div>
                    <div className="text-3xl font-bold text-blue-900">{histories.length}</div>
                </Card>
                <Card className="bg-gradient-to-br from-red-50 to-red-100">
                    <div className="text-sm font-medium text-red-600">미해결</div>
                    <div className="text-3xl font-bold text-red-900">
                        {histories.filter(h => !h.resolved).length}
                    </div>
                </Card>
                <Card className="bg-gradient-to-br from-green-50 to-green-100">
                    <div className="text-sm font-medium text-green-600">해결됨</div>
                    <div className="text-3xl font-bold text-green-900">
                        {histories.filter(h => h.resolved).length}
                    </div>
                </Card>
                <Card className="bg-gradient-to-br from-yellow-50 to-yellow-100">
                    <div className="text-sm font-medium text-yellow-600">전송 성공률</div>
                    <div className="text-3xl font-bold text-yellow-900">
                        {histories.length > 0
                            ? Math.round((histories.filter(h => h.notificationSent).length / histories.length) * 100)
                            : 0}%
                    </div>
                </Card>
            </div>

            {/* 필터 */}
            <Card>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    {/* 해결 상태 필터 */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            해결 상태
                        </label>
                        <select
                            value={filterStatus}
                            onChange={(e) => setFilterStatus(e.target.value as any)}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        >
                            <option value="all">전체</option>
                            <option value="unresolved">미해결</option>
                            <option value="resolved">해결됨</option>
                        </select>
                    </div>

                    {/* 애플리케이션 필터 */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            애플리케이션
                        </label>
                        <select
                            value={filterApplication}
                            onChange={(e) => setFilterApplication(e.target.value)}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        >
                            <option value="all">전체</option>
                            {applications.map(app => (
                                <option key={app} value={app}>{app}</option>
                            ))}
                        </select>
                    </div>

                    {/* 심각도 필터 */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            심각도
                        </label>
                        <select
                            value={filterSeverity}
                            onChange={(e) => setFilterSeverity(e.target.value)}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        >
                            <option value="all">전체</option>
                            <option value="CRITICAL">CRITICAL</option>
                            <option value="ERROR">ERROR</option>
                            <option value="WARNING">WARNING</option>
                            <option value="INFO">INFO</option>
                        </select>
                    </div>
                </div>

                <div className="mt-4 text-sm text-gray-500">
                    {filteredHistories.length}개의 알림이 표시되고 있습니다
                </div>
            </Card>

            {/* 알림 리스트 */}
            <div className="space-y-4">
                {filteredHistories.length === 0 ? (
                    <Card>
                        <p className="text-center text-gray-500 py-8">
                            조건에 맞는 알림 이력이 없습니다.
                        </p>
                    </Card>
                ) : (
                    filteredHistories.map((history) => (
                        <Card
                            key={history.id}
                            className={`${
                                !history.resolved ? 'border-l-4 border-red-500' : ''
                            } hover:shadow-lg transition-shadow`}
                        >
                            <div className="flex items-start justify-between">
                                <div className="flex-1">
                                    {/* 헤더 */}
                                    <div className="flex items-center space-x-3 mb-2">
                                        <h4 className="font-semibold text-gray-900">
                                            {history.alertRuleName || `알림 #${history.alertRuleId}`}
                                        </h4>
                                        <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                                            history.resolved
                                                ? 'bg-green-100 text-green-800'
                                                : 'bg-red-100 text-red-800'
                                        }`}>
                      {history.resolved ? '✓ 해결됨' : '● 미해결'}
                    </span>
                                        <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                                            getSeverityBadgeClass(history.severity)
                                        }`}>
                      {history.severity}
                    </span>
                                        <span className="px-2 py-1 text-xs font-medium rounded-full bg-blue-100 text-blue-800">
                      {history.application}
                    </span>
                                        <span className={`text-xs ${
                                            history.notificationSent ? 'text-green-600' : 'text-red-600'
                                        }`}>
                      {history.notificationSent ? '✅ 전송됨' : '❌ 전송 실패'}
                    </span>
                                    </div>

                                    {/* 메시지 */}
                                    <p className="text-sm text-gray-700 mb-3">
                                        {history.message}
                                    </p>

                                    {/* 상세 정보 */}
                                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs text-gray-600">
                                        <div>
                                            <span className="font-medium">발생 시간:</span>
                                            <br />
                                            {formatDateTime(history.triggeredAt)}
                                        </div>
                                        <div>
                                            <span className="font-medium">현재 값:</span>
                                            <br />
                                            {history.currentValue.toFixed(2)}{getMetricUnit(history.metricType)}
                                        </div>
                                        <div>
                                            <span className="font-medium">임계치:</span>
                                            <br />
                                            {history.thresholdValue.toFixed(2)}{getMetricUnit(history.metricType)}
                                        </div>
                                        <div>
                                            <span className="font-medium">메트릭:</span>
                                            <br />
                                            {getMetricDisplayName(history.metricType)}
                                        </div>
                                    </div>

                                    {/* 해결 정보 */}
                                    {history.resolved && history.resolvedAt && (
                                        <div className="mt-3 p-3 bg-green-50 rounded-lg">
                                            <div className="text-xs text-green-800">
                                                <div className="font-medium mb-1">해결 정보</div>
                                                <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
                                                    <div>
                                                        <span className="font-medium">해결 시간:</span> {formatDateTime(history.resolvedAt)}
                                                    </div>
                                                    <div>
                                                        <span className="font-medium">지속 시간:</span> {formatDuration(history.durationMinutes)}
                                                    </div>
                                                    {history.resolvedMessage && (
                                                        <div className="md:col-span-3">
                                                            <span className="font-medium">메시지:</span> {history.resolvedMessage}
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    )}

                                    {/* 알림 전송 상세 */}
                                    {history.notificationMethods && (
                                        <div className="mt-2 text-xs text-gray-500">
                                            <span className="font-medium">전송 방법:</span> {history.notificationMethods}
                                            {history.notificationResult && (
                                                <> | <span className="font-medium">결과:</span> {history.notificationResult}</>
                                            )}
                                            {history.notificationError && (
                                                <div className="text-red-600 mt-1">
                                                    <span className="font-medium">에러:</span> {history.notificationError}
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>

                                {/* 액션 버튼 */}
                                {!history.resolved && (
                                    <div className="ml-4">
                                        <button
                                            onClick={() => handleResolve(history.id)}
                                            disabled={resolving === history.id}
                                            className="px-3 py-1 text-sm bg-green-50 text-green-600 rounded hover:bg-green-100 disabled:opacity-50"
                                        >
                                            {resolving === history.id ? '처리 중...' : '해결 처리'}
                                        </button>
                                    </div>
                                )}
                            </div>
                        </Card>
                    ))
                )}
            </div>

            {/* 더 보기 버튼 */}
            {hasMore && (
                <div className="flex justify-center">
                    <Button
                        onClick={loadMore}
                        disabled={loading}
                        className="bg-gray-100 text-gray-700 hover:bg-gray-200"
                    >
                        {loading ? '로딩 중...' : '더 보기'}
                    </Button>
                </div>
            )}
        </div>
    );
}