'use client';

import { useState, useEffect } from 'react';
import Card from '@/components/common/Card';
import Button from '@/components/common/Button';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';

// api 서비스 및 타입 import
import {
    getAlertRules,
    getAlertHistory,
    toggleAlertRule,
    deleteAlertRule,
    resolveAlert,
    type AlertRuleResponse,
    type AlertHistoryResponse
} from '@/lib/api/alerts';

export default function AlertsPage() {
    const [activeTab, setActiveTab] = useState<'rules' | 'history'>('rules');

    // 타입 적용
    const [alertRules, setAlertRules] = useState<AlertRuleResponse[]>([]);
    const [alertHistory, setAlertHistory] = useState<AlertHistoryResponse[]>([]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [showCreateModal, setShowCreateModal] = useState(false);

    useEffect(() => {
        initData();
    }, []);

    // ✅ 데이터 초기화 (병렬 호출)
    const initData = async () => {
        setLoading(true);
        setError(null);
        try {
            // client.ts가 데이터를 언래핑(unwrap)해서 주므로 바로 변수에 할당
            const [rulesData, historyData] = await Promise.all([
                getAlertRules(),
                getAlertHistory(0, 50)
            ]);

            setAlertRules(rulesData);
            setAlertHistory(historyData);
        } catch (err: any) {
            console.error('데이터 로딩 실패:', err);
            // client.ts의 ApiError 클래스 메시지 활용
            setError(err.message || '데이터를 불러오는데 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // ✅ 알림 규칙 목록만 새로고침
    const refreshRules = async () => {
        try {
            const data = await getAlertRules();
            setAlertRules(data);
        } catch (err) {
            console.error(err);
        }
    };

    // ✅ 히스토리만 새로고침
    const refreshHistory = async () => {
        try {
            const data = await getAlertHistory(0, 50);
            setAlertHistory(data);
        } catch (err) {
            console.error(err);
        }
    };

    // ✅ 토글 핸들러
    const handleToggleAlert = async (id: number) => {
        try {
            await toggleAlertRule(id);
            await refreshRules(); // 목록 갱신
        } catch (err: any) {
            alert(err.message || '상태 변경에 실패했습니다.');
        }
    };

    // ✅ 삭제 핸들러
    const handleDeleteAlert = async (id: number) => {
        if (!confirm('정말 삭제하시겠습니까?')) return;

        try {
            await deleteAlertRule(id);
            await refreshRules();
        } catch (err: any) {
            alert(err.message || '삭제에 실패했습니다.');
        }
    };

    // ✅ 해결 처리 핸들러 (신규 기능 적용)
    const handleResolveAlert = async (historyId: number) => {
        if(!confirm('이 알림을 해결 처리하시겠습니까?')) return;

        try {
            await resolveAlert(historyId, '사용자 수동 해결');
            await refreshHistory();
        } catch (err: any) {
            alert(err.message || '해결 처리에 실패했습니다.');
        }
    };

    // --- Helper Functions (UI용) ---

    const formatDate = (dateString: string) => {
        try {
            return new Date(dateString).toLocaleString('ko-KR', {
                year: 'numeric', month: '2-digit', day: '2-digit',
                hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
            });
        } catch { return dateString; }
    };

    const getMetricName = (metricType: string) => {
        const names: Record<string, string> = {
            CPU_USAGE: 'CPU 사용률', HEAP_USAGE: 'Heap 메모리',
            TPS: 'TPS', ERROR_RATE: '에러율', DB_CONNECTIONS: 'DB 연결 수',
        };
        return names[metricType] || metricType;
    };

    const getSeverityColor = (active: boolean) =>
        active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800';

    if (loading) return <Loading />;
    if (error) return <ErrorMessage message={error} />;

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-3xl font-bold text-gray-900">알림 관리</h1>
                    <p className="mt-1 text-sm text-gray-500">시스템 알림 규칙 및 이력 관리</p>
                </div>
                <Button onClick={() => setShowCreateModal(true)}>새 알림 규칙 추가</Button>
            </div>

            {/* Tabs */}
            <div className="border-b border-gray-200">
                <nav className="-mb-px flex space-x-8">
                    <button
                        onClick={() => setActiveTab('rules')}
                        className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                            activeTab === 'rules' ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
                        }`}
                    >
                        알림 규칙 ({alertRules.length})
                    </button>
                    <button
                        onClick={() => setActiveTab('history')}
                        className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                            activeTab === 'history' ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
                        }`}
                    >
                        발생 이력 ({alertHistory.length})
                    </button>
                </nav>
            </div>

            {/* Rules Tab */}
            {activeTab === 'rules' && (
                <div className="grid gap-6">
                    {alertRules.length === 0 ? (
                        <Card><p className="text-center text-gray-500 py-8">등록된 규칙이 없습니다.</p></Card>
                    ) : (
                        alertRules.map((rule) => (
                            <Card key={rule.id} className="hover:shadow-lg transition-shadow">
                                <div className="flex items-start justify-between">
                                    <div className="flex-1">
                                        <div className="flex items-center space-x-3">
                                            <h3 className="text-lg font-semibold text-gray-900">{rule.name}</h3>
                                            <span className={`px-2 py-1 text-xs font-medium rounded-full ${getSeverityColor(rule.active)}`}>
                                                {rule.active ? '활성' : '비활성'}
                                            </span>
                                        </div>
                                        <div className="mt-2 space-y-1 text-sm text-gray-600">
                                            <p><span className="font-medium">앱:</span> {rule.application}</p>
                                            <p>
                                                <span className="font-medium">조건:</span> {getMetricName(rule.metricType)} {rule.condition} {rule.threshold}% ({rule.durationMinutes}분)
                                            </p>
                                            <p><span className="font-medium">알림:</span> {rule.notificationMethods.join(', ')}</p>
                                        </div>
                                    </div>
                                    <div className="flex space-x-2 ml-4">
                                        <button
                                            onClick={() => handleToggleAlert(rule.id)}
                                            className={`px-3 py-1 text-sm rounded transition-colors ${
                                                rule.active ? 'bg-gray-100 text-gray-600 hover:bg-gray-200' : 'bg-blue-50 text-blue-600 hover:bg-blue-100'
                                            }`}
                                        >
                                            {rule.active ? '비활성화' : '활성화'}
                                        </button>
                                        <button onClick={() => handleDeleteAlert(rule.id)} className="px-3 py-1 text-sm bg-red-50 text-red-600 rounded hover:bg-red-100">
                                            삭제
                                        </button>
                                    </div>
                                </div>
                            </Card>
                        ))
                    )}
                </div>
            )}

            {/* History Tab */}
            {activeTab === 'history' && (
                <div className="space-y-4">
                    {alertHistory.length === 0 ? (
                        <Card><p className="text-center text-gray-500 py-8">발생한 알림이 없습니다.</p></Card>
                    ) : (
                        alertHistory.map((history) => (
                            <Card key={history.id} className={history.resolved ? '' : 'border-l-4 border-red-500'}>
                                <div className="flex items-start justify-between">
                                    <div className="flex-1">
                                        <div className="flex items-center space-x-3">
                                            <h4 className="font-semibold text-gray-900">{history.alertRuleName}</h4>

                                            {/* 해결 여부 뱃지 (클릭 시 해결 처리) */}
                                            <button
                                                onClick={() => !history.resolved && handleResolveAlert(history.id)}
                                                disabled={history.resolved}
                                                className={`px-2 py-1 text-xs font-medium rounded-full transition-colors ${
                                                    history.resolved
                                                        ? 'bg-green-100 text-green-800 cursor-default'
                                                        : 'bg-red-100 text-red-800 hover:bg-red-200 cursor-pointer'
                                                }`}
                                                title={!history.resolved ? "클릭하여 해결 처리" : ""}
                                            >
                                                {history.resolved ? '해결됨' : '미해결 (Click to Resolve)'}
                                            </button>

                                            <span className="px-2 py-1 text-xs font-medium rounded-full bg-blue-100 text-blue-800">
                                                {history.application}
                                            </span>
                                        </div>
                                        <p className="mt-2 text-sm text-gray-600">{history.message}</p>
                                        <div className="mt-2 flex items-center space-x-4 text-xs text-gray-500">
                                            <span>발생: {formatDate(history.triggeredAt)}</span>
                                            {history.resolved && history.resolvedAt && (
                                                <span>해결: {formatDate(history.resolvedAt)}</span>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            </Card>
                        ))
                    )}
                </div>
            )}

            {/* Create Modal 자리 */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white p-6 rounded-lg shadow-xl">
                        <h2 className="text-xl font-bold mb-4">알림 규칙 생성</h2>
                        <p className="text-gray-500 mb-4">생성 모달 컴포넌트를 여기에 연결하세요.</p>
                        <Button onClick={() => setShowCreateModal(false)}>닫기</Button>
                    </div>
                </div>
            )}
        </div>
    );
}