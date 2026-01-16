// app/metrics/custom-query/page.tsx
'use client';

import { useState } from 'react';
import { Play, Info, BookOpen, Database } from 'lucide-react';
import Button from '@/components/common/Button';
import QueryEditor from '@/components/query/QueryEditor';
import QueryExamples from '@/components/query/QueryExamples';
import QueryHistory from '@/components/query/QueryHistory';
import ResultViewer from '@/components/query/ResultViewer';
import { useQueryExecution } from '@/hooks/useQueryExecution';

/**
 * 커스텀 PromQL 쿼리 페이지
 *
 * 기능:
 * - Monaco Editor로 PromQL 작성
 * - 예제 쿼리 제공
 * - 실행 히스토리 관리
 * - 결과 테이블/JSON 뷰
 * - Ctrl+Enter로 실행
 *
 * 사용자: 관리자, 고급 사용자
 */
export default function CustomQueryPage() {
    const [query, setQuery] = useState('# PromQL 쿼리를 입력하세요\nup');
    const { result, loading, error, history, execute, clearHistory } = useQueryExecution();

    const handleExecute = async () => {
        await execute(query);
    };

    const handleSelectExample = (exampleQuery: string) => {
        setQuery(exampleQuery);
    };

    const handleSelectHistory = (historyQuery: string) => {
        setQuery(historyQuery);
    };

    return (
        // ✅ [변경 1] 최상위 컨테이너의 중앙 정렬(max-w-7xl mx-auto) 및 패딩 제거
        // 화면 전체 너비를 사용하기 위해 flex 레이아웃으로 변경합니다.
        // h-[calc(100vh-4rem)]은 헤더 높이(예: 4rem)를 제외한 전체 높이를 의미합니다. 필요시 조정하세요.
        <div className="flex flex-col lg:flex-row h-full min-h-[calc(100vh-4rem)] items-stretch bg-gray-50">

            {/* ================= 왼쪽 메인 영역 (에디터 + 결과) ================= */}
            {/* flex-1로 남은 공간을 모두 차지하게 하고, 별도의 패딩을 줍니다. */}
            <div className="flex-1 p-6 lg:p-8 overflow-y-auto">
                {/* 헤더 */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-primary-700 mb-2">커스텀 PromQL 쿼리</h1>
                    <p className="text-secondary-600">Prometheus 메트릭을 직접 쿼리하고 분석합니다.</p>
                </div>

                {/* 안내 메시지 */}
                <div className="mb-6 bg-white border border-blue-200 rounded-lg p-4 shadow-sm">
                    <div className="flex">
                        <Info className="w-5 h-5 text-blue-600 mr-3 flex-shrink-0 mt-0.5" />
                        <div className="flex-1">
                            <h4 className="text-sm font-semibold text-blue-900 mb-1">사용 방법</h4>
                            <ul className="text-sm text-blue-800 space-y-1">
                                <li>• 에디터에 PromQL을 입력하고 <kbd className="px-2 py-0.5 bg-blue-100 rounded text-xs font-mono">Ctrl+Enter</kbd>로 실행하세요.</li>
                                <li>• 오른쪽의 예제 쿼리를 클릭하여 빠르게 시작할 수 있습니다.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div className="space-y-6">
                    {/* 쿼리 에디터 카드 */}
                    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                        <div className="flex justify-between items-center mb-4">
                            <h2 className="text-lg font-bold text-gray-800 flex items-center gap-2">
                                <BookOpen className="w-5 h-5 text-primary-500" />
                                쿼리 에디터
                            </h2>
                            <Button
                                variant="primary"
                                icon={loading ? undefined : <Play className="w-4 h-4 fill-current" />}
                                onClick={handleExecute}
                                disabled={loading}
                                className={loading ? "opacity-80 cursor-not-allowed" : ""}
                            >
                                {loading ? (
                                    <span className="flex items-center">
                                        <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                        </svg>
                                        실행 중...
                                    </span>
                                ) : '쿼리 실행 (Ctrl+Enter)'}
                            </Button>
                        </div>

                        <QueryEditor
                            value={query}
                            onChange={setQuery}
                            onExecute={handleExecute}
                            height="350px"
                        />

                        {/* 에러 메시지 */}
                        {error && (
                            <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm animate-shake">
                                <p className="font-semibold flex items-center">
                                    <svg className="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd"></path></svg>
                                    쿼리 실행 오류
                                </p>
                                <p className="mt-1 ml-6">{error}</p>
                            </div>
                        )}
                    </div>

                    {/* ✅ [변경 2] 실행 결과 영역 개선 */}
                    {/* ResultViewer를 감싸는 컨테이너에 overflow-x-auto를 적용하여 */}
                    {/* 내용이 길어질 경우 잘리지 않고 가로 스크롤이 생기도록 합니다. */}
                    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                        <div className="px-6 py-4 border-b border-gray-100 bg-gray-50 flex items-center gap-2">
                            <Database className="w-5 h-5 text-primary-500" />
                            <h2 className="text-lg font-bold text-gray-800">실행 결과</h2>
                        </div>
                        {/* 여기가 핵심입니다: w-full과 overflow-x-auto 조합 */}
                        <div className="w-full overflow-x-auto p-6">
                            <ResultViewer
                                data={result}
                                executionTime={
                                    history.length > 0 && history[0].success
                                        ? history[0].executionTime
                                        : undefined
                                }
                            />
                        </div>
                    </div>
                </div>
            </div>

            {/* ================= 오른쪽 사이드바 영역 (예제 + 히스토리) ================= */}
            {/* 오른쪽 벽에 붙는 사이드바 스타일 적용 */}
            {/* w-96 (고정 너비), 테두리(border-l), 배경색 구분, 높이 꽉 채우기 */}
            <div className="w-full lg:w-[450px] border-l border-gray-200 bg-white p-6 overflow-y-auto lg:sticky lg:top-0 lg:h-[calc(100vh-4rem)] shadow-sm z-10 flex-shrink-0">
                <div className="space-y-8">
                    {/* 예제 쿼리 컴포넌트 */}
                    <div className="space-y-4">
                        <h3 className="text-lg font-bold text-gray-900">예제 쿼리</h3>
                        <QueryExamples onSelectExample={handleSelectExample} />
                    </div>

                    {/* 구분선 */}
                    <hr className="border-gray-100" />

                    {/* 쿼리 히스토리 컴포넌트 */}
                    <div className="space-y-4">
                        <div className="flex justify-between items-center">
                            <h3 className="text-lg font-bold text-gray-900">실행 내역</h3>
                            {history.length > 0 && (
                                <button
                                    onClick={clearHistory}
                                    className="text-xs text-secondary-500 hover:text-red-600 transition-colors"
                                >
                                    기록 비우기
                                </button>
                            )}
                        </div>
                        <QueryHistory
                            history={history}
                            onSelectQuery={handleSelectHistory}
                            onClearHistory={clearHistory} // QueryHistory 내부 구현에 따라 필요 없을 수도 있음
                        />
                    </div>
                </div>
            </div>
        </div>
    );
}