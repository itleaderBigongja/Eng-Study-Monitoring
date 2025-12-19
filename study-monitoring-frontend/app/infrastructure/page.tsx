'use client';
import React, { useState } from 'react';

export default function InfrastructurePage() {
    const [selectedNamespace, setSelectedNamespace] = useState('ALL');

    // Kubernetes 리소스 데이터
    const namespaces = ['eng-study', 'monitoring'];

    const pods = [
        {
            name: 'eng-study-backend-7f9d8c675-abc12',
            namespace: 'eng-study',
            status: 'Running',
            restarts: 0,
            age: '2h 15m',
            cpu: '250m',
            memory: '512Mi',
            ready: '1/1',
        },
        {
            name: 'eng-study-frontend-6b8f9c675-def34',
            namespace: 'eng-study',
            status: 'Running',
            restarts: 0,
            age: '2h 15m',
            cpu: '100m',
            memory: '256Mi',
            ready: '1/1',
        },
        {
            name: 'postgres-5c9d8f675-ghi56',
            namespace: 'eng-study',
            status: 'Running',
            restarts: 0,
            age: '2h 20m',
            cpu: '200m',
            memory: '512Mi',
            ready: '1/1',
        },
        {
            name: 'nginx-4b7e8d675-jkl78',
            namespace: 'eng-study',
            status: 'Running',
            restarts: 0,
            age: '2h 10m',
            cpu: '50m',
            memory: '128Mi',
            ready: '1/1',
        },
        {
            name: 'monitoring-backend-8e6f5d675-mno90',
            namespace: 'monitoring',
            status: 'Running',
            restarts: 0,
            age: '2h 10m',
            cpu: '200m',
            memory: '512Mi',
            ready: '1/1',
        },
        {
            name: 'monitoring-frontend-7d5c4b675-pqr12',
            namespace: 'monitoring',
            status: 'Running',
            restarts: 1,
            age: '2h 10m',
            cpu: '100m',
            memory: '256Mi',
            ready: '1/1',
        },
        {
            name: 'elasticsearch-9a8b7c675-stu34',
            namespace: 'monitoring',
            status: 'Running',
            restarts: 0,
            age: '2h 18m',
            cpu: '500m',
            memory: '2Gi',
            ready: '1/1',
        },
        {
            name: 'kibana-6f5e4d675-vwx56',
            namespace: 'monitoring',
            status: 'Running',
            restarts: 2,
            age: '2h 18m',
            cpu: '300m',
            memory: '768Mi',
            ready: '1/1',
        },
        {
            name: 'prometheus-3c2b1a675-yza78',
            namespace: 'monitoring',
            status: 'Running',
            restarts: 0,
            age: '2h 20m',
            cpu: '400m',
            memory: '512Mi',
            ready: '1/1',
        },
    ];

    const services = [
        { name: 'eng-study-backend-service', namespace: 'eng-study', type: 'ClusterIP', clusterIP: '10.96.1.10', port: '8080/TCP' },
        { name: 'eng-study-frontend-service', namespace: 'eng-study', type: 'ClusterIP', clusterIP: '10.96.1.11', port: '3000/TCP' },
        { name: 'postgres-service', namespace: 'eng-study', type: 'ClusterIP', clusterIP: '10.96.1.12', port: '5432/TCP' },
        { name: 'nginx-service', namespace: 'eng-study', type: 'NodePort', clusterIP: '10.96.1.13', port: '80:30080/TCP' },
        { name: 'monitoring-backend-service', namespace: 'monitoring', type: 'ClusterIP', clusterIP: '10.96.2.10', port: '8081/TCP' },
        { name: 'monitoring-frontend-service', namespace: 'monitoring', type: 'ClusterIP', clusterIP: '10.96.2.11', port: '3001/TCP' },
        { name: 'elasticsearch-service', namespace: 'monitoring', type: 'ClusterIP', clusterIP: '10.96.2.12', port: '9200/TCP' },
        { name: 'kibana-service', namespace: 'monitoring', type: 'NodePort', clusterIP: '10.96.2.13', port: '5601:30601/TCP' },
        { name: 'prometheus-service', namespace: 'monitoring', type: 'ClusterIP', clusterIP: '10.96.2.14', port: '9090/TCP' },
    ];

    const filteredPods = selectedNamespace === 'ALL'
        ? pods
        : pods.filter(p => p.namespace === selectedNamespace);

    const filteredServices = selectedNamespace === 'ALL'
        ? services
        : services.filter(s => s.namespace === selectedNamespace);

    // 리소스 통계
    const stats = {
        totalPods: pods.length,
        runningPods: pods.filter(p => p.status === 'Running').length,
        totalServices: services.length,
        nodePortServices: services.filter(s => s.type === 'NodePort').length,
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-purple-50 to-lavender-50 p-6">
            {/* 헤더 */}
            <div className="mb-8">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-600 to-purple-400 bg-clip-text text-transparent mb-2">
                    인프라 현황
                </h1>
                <p className="text-gray-600">Kubernetes 클러스터 리소스 모니터링</p>
            </div>

            {/* 통계 카드 */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
                <div className="card bg-gradient-to-br from-purple-50 to-white">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm text-gray-600 mb-1">Total Pods</p>
                            <p className="text-3xl font-bold text-purple-600">{stats.totalPods}</p>
                        </div>
                        <div className="text-4xl">🚀</div>
                    </div>
                </div>

                <div className="card bg-gradient-to-br from-green-50 to-white">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm text-gray-600 mb-1">Running Pods</p>
                            <p className="text-3xl font-bold text-green-600">{stats.runningPods}</p>
                        </div>
                        <div className="text-4xl">✅</div>
                    </div>
                </div>

                <div className="card bg-gradient-to-br from-blue-50 to-white">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm text-gray-600 mb-1">Services</p>
                            <p className="text-3xl font-bold text-blue-600">{stats.totalServices}</p>
                        </div>
                        <div className="text-4xl">🌐</div>
                    </div>
                </div>

                <div className="card bg-gradient-to-br from-yellow-50 to-white">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm text-gray-600 mb-1">NodePorts</p>
                            <p className="text-3xl font-bold text-yellow-600">{stats.nodePortServices}</p>
                        </div>
                        <div className="text-4xl">🔌</div>
                    </div>
                </div>
            </div>

            {/* Namespace 필터 */}
            <div className="card mb-6">
                <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-gray-700">Namespace:</span>
                    <button
                        onClick={() => setSelectedNamespace('ALL')}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                            selectedNamespace === 'ALL'
                                ? 'bg-gradient-to-r from-purple-500 to-purple-600 text-white shadow-md'
                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                    >
                        전체
                    </button>
                    {namespaces.map(ns => (
                        <button
                            key={ns}
                            onClick={() => setSelectedNamespace(ns)}
                            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                                selectedNamespace === ns
                                    ? 'bg-gradient-to-r from-purple-500 to-purple-600 text-white shadow-md'
                                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                            }`}
                        >
                            {ns}
                        </button>
                    ))}
                </div>
            </div>

            {/* Pods 테이블 */}
            <div className="card mb-6">
                <div className="flex items-center justify-between mb-4">
                    <h2 className="text-xl font-bold text-gray-800">Pods</h2>
                    <button className="btn-primary text-sm">🔄 새로고침</button>
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead>
                        <tr>
                            <th className="text-left">Pod Name</th>
                            <th className="text-left">Namespace</th>
                            <th className="text-center">Status</th>
                            <th className="text-center">Ready</th>
                            <th className="text-center">Restarts</th>
                            <th className="text-left">CPU</th>
                            <th className="text-left">Memory</th>
                            <th className="text-right">Age</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filteredPods.map((pod, idx) => (
                            <tr key={idx}>
                                <td className="font-mono text-sm">{pod.name}</td>
                                <td>
                    <span className="px-2 py-1 bg-purple-100 text-purple-700 rounded text-sm">
                      {pod.namespace}
                    </span>
                                </td>
                                <td className="text-center">
                    <span className={pod.status === 'Running' ? 'status-running' : 'status-error'}>
                      {pod.status}
                    </span>
                                </td>
                                <td className="text-center font-medium text-gray-700">{pod.ready}</td>
                                <td className="text-center">
                    <span className={`font-medium ${pod.restarts > 0 ? 'text-yellow-600' : 'text-gray-600'}`}>
                      {pod.restarts}
                    </span>
                                </td>
                                <td className="font-mono text-sm text-gray-600">{pod.cpu}</td>
                                <td className="font-mono text-sm text-gray-600">{pod.memory}</td>
                                <td className="text-right text-gray-600">{pod.age}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Services 테이블 */}
            <div className="card">
                <h2 className="text-xl font-bold text-gray-800 mb-4">Services</h2>

                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead>
                        <tr>
                            <th className="text-left">Service Name</th>
                            <th className="text-left">Namespace</th>
                            <th className="text-center">Type</th>
                            <th className="text-left">Cluster IP</th>
                            <th className="text-left">Port(s)</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filteredServices.map((svc, idx) => (
                            <tr key={idx}>
                                <td className="font-mono text-sm">{svc.name}</td>
                                <td>
                    <span className="px-2 py-1 bg-purple-100 text-purple-700 rounded text-sm">
                      {svc.namespace}
                    </span>
                                </td>
                                <td className="text-center">
                    <span className={`px-2 py-1 rounded text-sm font-medium ${
                        svc.type === 'NodePort'
                            ? 'bg-blue-100 text-blue-700'
                            : 'bg-gray-100 text-gray-700'
                    }`}>
                      {svc.type}
                    </span>
                                </td>
                                <td className="font-mono text-sm text-gray-600">{svc.clusterIP}</td>
                                <td className="font-mono text-sm text-gray-600">{svc.port}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* 접속 정보 */}
            <div className="card mt-6 bg-gradient-to-br from-purple-50 to-white">
                <h2 className="text-xl font-bold text-gray-800 mb-4">🌐 외부 접속 정보</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="bg-white p-4 rounded-lg border-2 border-purple-200">
                        <div className="flex items-center gap-2 mb-2">
                            <span className="text-2xl">📚</span>
                            <h3 className="font-semibold text-gray-800">영어 학습 플랫폼</h3>
                        </div>
                        <a
                            href="http://localhost:30080"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-purple-600 hover:text-purple-700 font-mono text-sm"
                        >
                            http://localhost:30080
                        </a>
                    </div>

                    <div className="bg-white p-4 rounded-lg border-2 border-purple-200">
                        <div className="flex items-center gap-2 mb-2">
                            <span className="text-2xl">📊</span>
                            <h3 className="font-semibold text-gray-800">모니터링 대시보드</h3>
                        </div>
                        <a
                            href="http://localhost:30080/monitoring"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-purple-600 hover:text-purple-700 font-mono text-sm"
                        >
                            http://localhost:30080/monitoring
                        </a>
                    </div>

                    <div className="bg-white p-4 rounded-lg border-2 border-purple-200">
                        <div className="flex items-center gap-2 mb-2">
                            <span className="text-2xl">🔍</span>
                            <h3 className="font-semibold text-gray-800">Kibana (로그)</h3>
                        </div>
                        <a
                            href="http://localhost:30601"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-purple-600 hover:text-purple-700 font-mono text-sm"
                        >
                            http://localhost:30601
                        </a>
                    </div>

                    <div className="bg-white p-4 rounded-lg border-2 border-purple-200">
                        <div className="flex items-center gap-2 mb-2">
                            <span className="text-2xl">📈</span>
                            <h3 className="font-semibold text-gray-800">Prometheus (메트릭)</h3>
                        </div>
                        <p className="text-gray-600 font-mono text-sm">
                            Port Forward 필요: kubectl port-forward...
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}