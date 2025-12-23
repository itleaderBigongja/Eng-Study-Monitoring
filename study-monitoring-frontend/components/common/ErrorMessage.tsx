import { AlertCircle, RefreshCw } from 'lucide-react';
import Button from './Button';

interface ErrorMessageProps {
    message: string;
    onRetry?: () => void;
}

export default function ErrorMessage({ message, onRetry }: ErrorMessageProps) {
    return (
        <div className="card border-error/20 bg-error/5">
            <div className="flex items-start space-x-3">
                <AlertCircle className="w-6 h-6 text-error flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                    <h3 className="text-lg font-semibold text-error mb-2">오류 발생</h3>
                    <p className="text-secondary-700 mb-4">{message}</p>
                    {onRetry && (
                        <Button
                            variant="outline"
                            size="sm"
                            icon={<RefreshCw className="w-4 h-4" />}
                            onClick={onRetry}
                        >
                            다시 시도
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
}