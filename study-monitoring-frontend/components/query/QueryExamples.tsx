'use client';

import { useState, useEffect } from 'react';
import { Lightbulb, RotateCcw, Edit2, Save, Trash2, Plus } from 'lucide-react';

interface QueryExample {
    id: string;
    name: string;
    query: string;
    description: string;
}

interface QueryExamplesProps {
    onSelectExample: (query: string) => void;
}

// ✅ [변경 1] 실무용 필수 PromQL 20개로 확장
const DEFAULT_EXAMPLES: QueryExample[] = [
    // --- 시스템 기초 ---
    { id: 'sys-1', name: '전체 인스턴스 상태 (UP)', query: 'up', description: '모든 타겟의 생존 여부 (1: Up, 0: Down)' },
    { id: 'sys-2', name: '시스템 가동 시간 (Uptime)', query: 'sum(time() - process_start_time_seconds) by (application)', description: '애플리케이션별 가동 시간(초)' },

    // --- CPU & 메모리 (Node) ---
    { id: 'node-1', name: '노드 CPU 사용률 (%)', query: '100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100)', description: '전체 CPU 사용량 백분율' },
    { id: 'node-2', name: '메모리 사용률 (%)', query: '(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100', description: '전체 시스템 메모리 점유율' },
    { id: 'node-3', name: '디스크 여유 공간 (GB)', query: 'node_filesystem_avail_bytes{mountpoint="/"} / 1024 / 1024 / 1024', description: '루트 파티션의 남은 용량' },

    // --- JVM (Java) ---
    { id: 'jvm-1', name: 'JVM 힙 메모리 사용률', query: 'sum(jvm_memory_used_bytes{area="heap"}) by (application) / sum(jvm_memory_max_bytes{area="heap"}) by (application) * 100', description: '자바 힙 메모리 사용 비중' },
    { id: 'jvm-2', name: 'GC 발생 빈도 (1분)', query: 'rate(jvm_gc_pause_seconds_count[1m])', description: '1분당 가비지 컬렉션 발생 횟수' },
    { id: 'jvm-3', name: '활성 스레드 수', query: 'jvm_threads_live_threads', description: '현재 실행 중인 데몬/비데몬 스레드 수' },

    // --- HTTP 트래픽 (Spring Boot) ---
    { id: 'http-1', name: '초당 요청 수 (TPS)', query: 'sum(rate(http_server_requests_seconds_count[1m])) by (application)', description: '애플리케이션별 초당 처리량' },
    { id: 'http-2', name: 'HTTP 500 에러율', query: 'sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m])) by (application) / sum(rate(http_server_requests_seconds_count[1m])) by (application) * 100', description: '전체 요청 대비 5xx 에러 비율' },
    { id: 'http-3', name: '응답 속도 (P95)', query: 'histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application))', description: '상위 95% 요청의 응답 시간(초)' },
    { id: 'http-4', name: '느린 요청 (Slow Req)', query: 'rate(http_server_requests_seconds_count{le="+Inf"}[1m]) - rate(http_server_requests_seconds_count{le="1.0"}[1m])', description: '1초 이상 걸리는 요청의 빈도' },

    // --- 로그 & 예외 ---
    { id: 'log-1', name: '로그 에러 발생 건수', query: 'increase(logback_events_total{level="error"}[1m])', description: '최근 1분간 발생한 ERROR 로그 개수' },

    // --- DB (PostgreSQL / HikariCP) ---
    { id: 'db-1', name: 'DB 활성 커넥션 수', query: 'hikaricp_connections_active', description: '현재 사용 중인 DB 연결 수' },
    { id: 'db-2', name: 'DB 커넥션 획득 대기 시간', query: 'rate(hikaricp_connection_acquire_seconds_sum[1m]) / rate(hikaricp_connection_acquire_seconds_count[1m])', description: '커넥션 풀에서 연결을 얻는 데 걸린 평균 시간' },
    { id: 'db-3', name: '트랜잭션 롤백 빈도', query: 'rate(pg_stat_database_xact_rollback[5m])', description: 'PostgreSQL 트랜잭션 롤백 발생률' },

    // --- 네트워크 ---
    { id: 'net-1', name: '네트워크 수신량 (MB/s)', query: 'rate(node_network_receive_bytes_total[1m]) / 1024 / 1024', description: '초당 네트워크 다운로드 속도' },
    { id: 'net-2', name: '네트워크 송신량 (MB/s)', query: 'rate(node_network_transmit_bytes_total[1m]) / 1024 / 1024', description: '초당 네트워크 업로드 속도' },

    // --- 기타 ---
    { id: 'etc-1', name: 'CPU 코어 수', query: 'count(node_cpu_seconds_total{mode="system"}) by (instance)', description: '서버별 CPU 코어 개수 확인' }
];

export default function QueryExamples({ onSelectExample }: QueryExamplesProps) {
    const [examples, setExamples] = useState<QueryExample[]>(DEFAULT_EXAMPLES);
    const [isEditing, setIsEditing] = useState(false);
    const [isLoaded, setIsLoaded] = useState(false);

    useEffect(() => {
        const saved = localStorage.getItem('my-query-examples');
        if (saved) {
            try {
                setExamples(JSON.parse(saved));
            } catch (e) {
                console.error("저장된 예제 불러오기 실패", e);
            }
        }
        setIsLoaded(true);
    }, []);

    useEffect(() => {
        if (isLoaded) {
            localStorage.setItem('my-query-examples', JSON.stringify(examples));
        }
    }, [examples, isLoaded]);

    const handleReset = () => {
        if (window.confirm('모든 커스텀 예제를 삭제하고 기본값(20개)으로 초기화하시겠습니까?')) {
            setExamples(DEFAULT_EXAMPLES);
            setIsEditing(false);
        }
    };

    const handleAdd = () => {
        const newExample: QueryExample = {
            id: Date.now().toString(),
            name: '새 쿼리',
            query: '',
            description: '설명을 입력하세요'
        };
        setExamples([...examples, newExample]);
    };

    const handleDelete = (id: string) => {
        setExamples(examples.filter(ex => ex.id !== id));
    };

    const handleUpdate = (id: string, field: keyof QueryExample, value: string) => {
        setExamples(examples.map(ex =>
            ex.id === id ? { ...ex, [field]: value } : ex
        ));
    };

    if (!isLoaded) return null;

    return (
        // ✅ [변경 1] h-full 제거: 부모 높이를 꽉 채우지 않고 내용물만큼만 차지하게 함
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden flex flex-col">

            {/* 헤더 */}
            <div className="p-4 border-b border-gray-100 bg-gray-50 flex justify-between items-center flex-shrink-0">
                <h3 className="font-bold text-gray-800 flex items-center gap-2">
                    <Lightbulb className="w-5 h-5 text-yellow-500" />
                    예제 쿼리
                </h3>
                <div className="flex gap-2">
                    {isEditing ? (
                        <>
                            <button onClick={handleReset} className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded" title="기본값 초기화">
                                <RotateCcw className="w-4 h-4" />
                            </button>
                            <button onClick={() => setIsEditing(false)} className="px-3 py-1.5 bg-green-600 text-white text-xs font-bold rounded hover:bg-green-700 flex items-center gap-1">
                                <Save className="w-3 h-3" /> 저장
                            </button>
                        </>
                    ) : (
                        <button onClick={() => setIsEditing(true)} className="p-1.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded" title="편집">
                            <Edit2 className="w-4 h-4" />
                        </button>
                    )}
                </div>
            </div>

            {/* ✅ [변경 2] 목록 영역에 높이 제한(max-h) 추가
               - max-h-[500px]: 목록의 높이가 500px를 넘으면 스크롤바가 생깁니다.
               - min-h-[200px]: 데이터가 없어도 최소 높이는 유지합니다.
            */}
            <div className="overflow-y-auto p-2 space-y-2 max-h-[500px] min-h-[200px]">
                {examples.map((example) => (
                    <div
                        key={example.id}
                        className={`group rounded-lg border transition-all duration-200 ${
                            isEditing
                                ? 'border-blue-200 bg-blue-50 p-3'
                                : 'border-transparent hover:bg-gray-50 hover:border-gray-200 cursor-pointer p-3'
                        }`}
                        onClick={() => !isEditing && onSelectExample(example.query)}
                    >
                        {isEditing ? (
                            // 편집 모드
                            <div className="space-y-2">
                                <div className="flex justify-between items-start gap-2">
                                    <input
                                        type="text"
                                        value={example.name}
                                        onChange={(e) => handleUpdate(example.id, 'name', e.target.value)}
                                        className="flex-1 px-2 py-1 text-sm font-bold border border-gray-300 rounded"
                                        placeholder="제목"
                                    />
                                    <button onClick={(e) => { e.stopPropagation(); handleDelete(example.id); }} className="text-red-400 hover:text-red-600 p-1">
                                        <Trash2 className="w-4 h-4" />
                                    </button>
                                </div>
                                <textarea
                                    value={example.query}
                                    onChange={(e) => handleUpdate(example.id, 'query', e.target.value)}
                                    className="w-full px-2 py-1 text-xs font-mono border border-gray-300 rounded bg-white"
                                    placeholder="PromQL 쿼리"
                                    rows={2}
                                />
                                <input
                                    type="text"
                                    value={example.description}
                                    onChange={(e) => handleUpdate(example.id, 'description', e.target.value)}
                                    className="w-full px-2 py-1 text-xs text-gray-600 border border-gray-300 rounded"
                                    placeholder="설명"
                                />
                            </div>
                        ) : (
                            // 보기 모드
                            <div>
                                <div className="flex justify-between items-center mb-1">
                                    <span className="font-semibold text-sm text-gray-800 group-hover:text-primary-700">
                                        {example.name}
                                    </span>
                                </div>
                                <code className="block text-xs text-gray-600 font-mono bg-gray-100 p-2 rounded break-all group-hover:bg-white group-hover:text-primary-600 border border-transparent group-hover:border-primary-100">
                                    {example.query}
                                </code>
                                <p className="text-xs text-gray-500 mt-1">
                                    {example.description}
                                </p>
                            </div>
                        )}
                    </div>
                ))}

                {isEditing && (
                    <button onClick={handleAdd} className="w-full py-2 border-2 border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-blue-500 hover:text-blue-600 flex justify-center items-center gap-2 text-sm font-medium">
                        <Plus className="w-4 h-4" /> 새 쿼리
                    </button>
                )}
            </div>
        </div>
    );
}