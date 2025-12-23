import { Loader2 } from 'lucide-react';

interface LoadingProps {
    text?: string;
    size?: 'sm' | 'md' | 'lg';
    fullScreen?: boolean;
}

export default function Loading({
                                    text = '로딩 중...',
                                    size = 'md',
                                    fullScreen = false
                                }: LoadingProps) {
    const sizeClasses = {
        sm: 'w-6 h-6',
        md: 'w-10 h-10',
        lg: 'w-16 h-16',
    };

    const content = (
        <div className="flex flex-col items-center justify-center space-y-4">
            <Loader2 className={`${sizeClasses[size]} text-primary-500 animate-spin`} />
            <p className="text-secondary-600">{text}</p>
        </div>
    );

    if (fullScreen) {
        return (
            <div className="fixed inset-0 bg-white/80 backdrop-blur-sm flex items-center justify-center z-50">
                {content}
            </div>
        );
    }

    return (
        <div className="flex items-center justify-center py-12">
            {content}
        </div>
    );
}