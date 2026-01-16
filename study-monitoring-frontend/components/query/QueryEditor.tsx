// components/query/QueryEditor.tsx
'use client';

import React, { useRef, useEffect, useState } from 'react';
import Editor, { useMonaco } from '@monaco-editor/react'; // useMonaco 훅 추가
import type { editor } from 'monaco-editor';
import { getMetricNames } from '@/lib/api/metrics';

interface QueryEditorProps {
    value: string;
    onChange: (value: string) => void;
    onExecute?: () => void;
    height?: string;
}

export default function QueryEditor({
                                        value,
                                        onChange,
                                        onExecute,
                                        height = '300px'
                                    }: QueryEditorProps) {
    const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null);
    const onExecuteRef = useRef(onExecute);
    const monaco = useMonaco(); // Monaco 인스턴스 가져오기
    const [suggestions, setSuggestions] = useState<string[]>([]);

    // 1. 최신 실행 함수 참조 유지
    useEffect(() => {
        onExecuteRef.current = onExecute;
    }, [onExecute]);

    // 2. 컴포넌트 마운트 시 메트릭 이름 목록 가져오기
    useEffect(() => {
        async function fetchMetrics() {
            try {
                // 실제로는 API 호출 (여기서는 예시 데이터로 테스트 가능)
                const metrics = await getMetricNames();
                // 만약 API가 아직 준비 안됐다면 아래 주석을 풀어 테스트해보세요
                // const metrics = ['up', 'process_cpu_seconds_total', 'jvm_memory_used_bytes', 'node_memory_Active_bytes'];
                setSuggestions(metrics as any);
            } catch (error) {
                console.error("메트릭 목록 로드 실패:", error);
            }
        }
        fetchMetrics();
    }, []);

    // 3. Monaco Editor에 언어 등록 및 자동완성 설정 (이 부분을 교체하세요)
    useEffect(() => {
        // monaco 인스턴스가 없거나, 메트릭 데이터가 아직 안 왔으면 중단
        if (!monaco || suggestions.length === 0) return;

        // [수정 포인트 1] 'promql' 언어가 등록되어 있는지 확인하고, 없으면 등록합니다.
        // 이 과정이 있어야 defaultLanguage="promql"이 정상 작동하고, provider가 연결됩니다.
        const languages = monaco.languages.getLanguages();
        const isPromqlRegistered = languages.some(lang => lang.id === 'promql');

        if (!isPromqlRegistered) {
            monaco.languages.register({ id: 'promql' });
        }

        // [수정 포인트 2] 자동완성 제공자 등록
        const provider = monaco.languages.registerCompletionItemProvider('promql', {
            // [옵션] 사용자가 '_'를 눌렀을 때도 자동완성 창이 뜨도록 설정
            triggerCharacters: ['_'],

            provideCompletionItems: (model, position) => {
                const word = model.getWordUntilPosition(position);

                // 현재 커서가 위치한 단어의 범위 계산
                const range = {
                    startLineNumber: position.lineNumber,
                    endLineNumber: position.lineNumber,
                    startColumn: word.startColumn,
                    endColumn: word.endColumn,
                };

                return {
                    suggestions: suggestions.map((metric) => ({
                        label: metric,
                        kind: monaco.languages.CompletionItemKind.Variable, // 변수 아이콘 사용
                        insertText: metric, // 실제 입력될 텍스트
                        range: range,
                        detail: "Metric", // 우측에 표시될 설명
                        // [중요] 필터링 텍스트 지정 (이게 없으면 검색이 잘 안될 수 있음)
                        filterText: metric
                    })),
                };
            },
        });

        // 컴포넌트가 사라질 때 리소스 정리
        return () => {
            provider.dispose();
        };
    }, [monaco, suggestions]);

    const handleEditorDidMount = (editor: editor.IStandaloneCodeEditor, monacoInstance: any) => {
        editorRef.current = editor;

        // Ctrl+Enter 실행 바인딩
        editor.addCommand(2048 | 3, () => {
            if (onExecuteRef.current) {
                onExecuteRef.current();
            }
        });

        editor.focus();
    };

    const handleChange = (value: string | undefined) => {
        if (value !== undefined) onChange(value);
    };

    return (
        <div className="border border-gray-300 rounded-lg overflow-hidden">
            <Editor
                height={height}
                // ✅ 중요: 여기서 언어를 'promql'로 설정해야 위에서 등록한 자동완성이 작동합니다.
                // Monaco는 기본적으로 promql을 모르지만, 우리가 위에서 provider를 'promql'에 등록했으므로 작동합니다.
                defaultLanguage="promql"

                // 만약 테마가 밋밋하면 'vs-dark'를 쓰되, 언어 정의가 없어서 색상은 안 나올 수 있습니다.
                // 텍스트만 나오면 된다면 문제 없습니다.
                value={value}
                onChange={handleChange}
                onMount={handleEditorDidMount}
                theme="vs-dark" // 깔끔하게 화이트 테마 (혹은 vs-white)
                options={{
                    minimap: { enabled: false },
                    fontSize: 14,
                    lineNumbers: 'on',
                    scrollBeyondLastLine: false,
                    automaticLayout: true,
                    tabSize: 2,
                    wordWrap: 'on',
                    quickSuggestions: true, // 자동완성 켜기
                }}
            />
        </div>
    );
}