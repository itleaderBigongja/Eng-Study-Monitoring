export default function Footer() {
    return (
        <footer className="bg-white border-t border-primary-100 mt-auto">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
                <div className="flex flex-col sm:flex-row justify-between items-center space-y-2 sm:space-y-0">
                    <p className="text-sm text-secondary-600">
                        © 2025 Study Monitoring System. All rights reserved.
                    </p>
                    <div className="flex items-center space-x-4 text-sm text-secondary-600">
                        <span>Backend: Spring Boot 3.5.7</span>
                        <span>•</span>
                        <span>Frontend: Next.js 15</span>
                    </div>
                </div>
            </div>
        </footer>
    );
}