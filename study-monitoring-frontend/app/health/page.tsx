'use client';

import { useEffect, useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import {
    getHealthStatus,
    getElasticsearchHealth,
    getDatabaseHealth,
    getPrometheusHealth,
    HealthStatus,
} from '@/lib/api/health';
import { CheckCircle2, XCircle, RefreshCw, AlertCircle } from 'lucide-react';

export default function HealthPage() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [lastUpdate, setLastUpdate] = useState<Date>(new Date());

    const [overallHealth, setOverallHealth] = useState<HealthStatus | null>(null);
    const [elasticsearchHealth, setElasticsearchHealth] = useState<HealthStatus | null>(null);
    const [databaseHealth, setDatabaseHealth] = useState<HealthStatus | null>(null);
    const [prometheusHealth, setPrometheusHealth] = useState<HealthStatus | null>(null);

    const fetchHealthData = async () => {
        setLoading(true);
        setError(null);

        try {
            const [overall, es, db, prom] = await Promise.all([
                getHealthStatus(),
                getElasticsearchHealth(),
                getDatabaseHealth(),
                getPrometheusHealth(),
            ]);

            setOverallHealth(overall);
            setElasticsearchHealth(es);
            setDatabaseHealth(db);
            setPrometheusHealth(prom);
            setLastUpdate(new Date());
        } catch (err: any) {
            setError(err.message || '헬스체크 데이터를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    // useEffect 실행 : 페이지가 처음 렌더링 된 직후 useEffect 안에 있는 fetchHealthData() 메서드 실행
    useEffect(() => {
        fetchHealthData();

        // 30초마다 자동 새로고침
        const interval = setInterval(fetchHealthData, 30000);
        return () => clearInterval(interval);
    }, []);

    if (loading && !overallHealth) {
        return (
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <Loading text="헬스체크 정보를 불러오는 중..." />
            </div>
        );
    }

    if (error && !overallHealth) {
        return (
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <ErrorMessage message={error} onRetry={fetchHealthData} />
            </div>
        );
    }

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {/* 헤더 */}
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-primary-700 mb-2">
                        시스템 헬스체크
                    </h1>
                    <p className="text-secondary-600">
                        마지막 업데이트: {lastUpdate.toLocaleString('ko-KR')}
                    </p>
                </div>
                <Button
                    variant="outline"
                    icon={<RefreshCw className="w-4 h-4" />}
                    onClick={fetchHealthData}
                    loading={loading}
                >
                    새로고침
                </Button>
            </div>

            {/* 전체 상태 */}
            <Card className="mb-6">
                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-4">
                        {overallHealth?.status === 'UP' ? (
                            <CheckCircle2 className="w-12 h-12 text-success" />
                        ) : (
                            <XCircle className="w-12 h-12 text-error" />
                        )}
                        <div>
                            <h2 className="text-2xl font-bold text-primary-700">
                                전체 시스템 상태
                            </h2>
                            <p className="text-secondary-600">
                                {overallHealth?.status === 'UP'
                                    ? '모든 시스템이 정상 작동 중입니다'
                                    : '일부 시스템에 문제가 발생했습니다'}
                            </p>
                        </div>
                    </div>
                    <div className={`px-6 py-3 rounded-lg font-bold text-xl ${
                        overallHealth?.status === 'UP'
                            ? 'bg-success/10 text-success'
                            : 'bg-error/10 text-error'
                    }`}>
                        {overallHealth?.status}
                    </div>
                </div>
            </Card>

            {/* 개별 컴포넌트 상태 */}
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
                <HealthCard
                    title="Elasticsearch"
                    status={elasticsearchHealth?.status || 'UNKNOWN'}
                    details={elasticsearchHealth}
                />

                <HealthCard
                    title="PostgreSQL"
                    status={databaseHealth?.status || 'UNKNOWN'}
                    details={databaseHealth}
                />

                <HealthCard
                    title="Prometheus"
                    status={prometheusHealth?.status || 'UNKNOWN'}
                    details={prometheusHealth}
                />
            </div>
        </div>
    );
}

function HealthCard({
                        title,
                        status,
                        details,
                    }: {
    title: string;
    status: string;
    details: HealthStatus | null;
}) {
    const isUp = status === 'UP';

    return (
        <Card>
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-primary-700">{title}</h3>
                {isUp ? (
                    <CheckCircle2 className="w-6 h-6 text-success" />
                ) : (
                    <AlertCircle className="w-6 h-6 text-error" />
                )}
            </div>

            <div className={`px-4 py-2 rounded-lg font-semibold mb-4 ${
                isUp ? 'bg-success/10 text-success' : 'bg-error/10 text-error'
            }`}>
                {status}
            </div>

            {details && (
                <div className="space-y-2 text-sm">
                    {Object.entries(details)
                        .filter(([key]) => key !== 'status')
                        .map(([key, value]) => (
                            <div key={key} className="flex justify-between">
                                <span className="text-secondary-600">{key}:</span>
                                <span className="font-medium text-secondary-900">
                  {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                </span>
                            </div>
                        ))}
                </div>
            )}
        </Card>
    );
}