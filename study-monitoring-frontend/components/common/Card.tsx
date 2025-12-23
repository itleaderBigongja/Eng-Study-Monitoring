import { ReactNode } from 'react';

interface CardProps {
    title?: string;
    subtitle?: string;
    children: ReactNode;
    className?: string;
    headerAction?: ReactNode;
}

export default function Card({
                                 title,
                                 subtitle,
                                 children,
                                 className = '',
                                 headerAction,
                             }: CardProps) {
    return (
        <div className={`card fade-in ${className}`}>
            {(title || headerAction) && (
                <div className="flex justify-between items-start mb-4">
                    <div>
                        {title && <h3 className="text-lg font-semibold text-primary-700">{title}</h3>}
                        {subtitle && <p className="text-sm text-secondary-500 mt-1">{subtitle}</p>}
                    </div>
                    {headerAction && <div>{headerAction}</div>}
                </div>
            )}
            {children}
        </div>
    );
}