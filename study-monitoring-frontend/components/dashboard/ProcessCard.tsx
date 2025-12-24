import { Activity, AlertCircle, CheckCircle, XCircle } from 'lucide-react';
import Card from '@/components/common/Card';

interface ProcessInfo {
    name: string;
    status: 'running' | 'stopped' | 'warning';
    uptime?: string;
    cpu?: number;
    memory?: number;
    pid?: number;
}

interface ProcessCardProps {
    processes: ProcessInfo[];
    title?: string;
}

export default function ProcessCard({ processes, title = '프로세스 상태' }: ProcessCardProps) {
    const getStatusIcon = (status: ProcessInfo['status']) => {
        switch (status) {
            case 'running':
                return <CheckCircle className="w-5 h-5 text-success" />;
            case 'warning':
                return <AlertCircle className="w-5 h-5 text-warning" />;
            case 'stopped':
                return <XCircle className="w-5 h-5 text-error" />;
            default:
                return <Activity className="w-5 h-5 text-secondary-400" />;
        }
    };

    const getStatusColor = (status: ProcessInfo['status']) => {
        switch (status) {
            case 'running':
                return 'bg-success/10 text-success border-success/20';
            case 'warning':
                return 'bg-warning/10 text-warning border-warning/20';
            case 'stopped':
                return 'bg-error/10 text-error border-error/20';
            default:
                return 'bg-secondary-100 text-secondary-600 border-secondary-200';
        }
    };

    return (
        <Card title={title}>
            <div className="space-y-3">
                {processes.length === 0 ? (
                    <div className="text-center py-8 text-secondary-500">
                        <Activity className="w-12 h-12 mx-auto mb-2 opacity-50" />
                        <p>프로세스 정보가 없습니다</p>
                    </div>
                ) : (
                    processes.map((process, index) => (
                        <div
                            key={index}
                            className={`p-4 rounded-lg border ${getStatusColor(process.status)} transition-all duration-200`}
                        >
                            <div className="flex items-start justify-between mb-2">
                                <div className="flex items-center space-x-3">
                                    {getStatusIcon(process.status)}
                                    <div>
                                        <h4 className="font-semibold text-gray-900">
                                            {process.name}
                                        </h4>
                                        {process.pid && (
                                            <p className="text-xs text-secondary-500">
                                                PID: {process.pid}
                                            </p>
                                        )}
                                    </div>
                                </div>
                                <span className="text-xs font-medium uppercase">
                                    {process.status}
                                </span>
                            </div>

                            {/* 프로세스 상세 정보 */}
                            {(process.uptime || process.cpu !== undefined || process.memory !== undefined) && (
                                <div className="grid grid-cols-3 gap-2 mt-3 pt-3 border-t border-current border-opacity-20">
                                    {process.uptime && (
                                        <div>
                                            <p className="text-xs text-secondary-600">Uptime</p>
                                            <p className="text-sm font-medium">{process.uptime}</p>
                                        </div>
                                    )}
                                    {process.cpu !== undefined && (
                                        <div>
                                            <p className="text-xs text-secondary-600">CPU</p>
                                            <p className="text-sm font-medium">{process.cpu.toFixed(1)}%</p>
                                        </div>
                                    )}
                                    {process.memory !== undefined && (
                                        <div>
                                            <p className="text-xs text-secondary-600">Memory</p>
                                            <p className="text-sm font-medium">{process.memory.toFixed(1)}%</p>
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    ))
                )}
            </div>
        </Card>
    );
}