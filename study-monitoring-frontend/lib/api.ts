const API_BASE_URL = process.env.NEXT_PUBLIC_MONITORING_API || 'http://localhost:8081/api';

class ApiClient {
    private baseURL: string;

    constructor(baseURL: string) {
        this.baseURL = baseURL;
    }

    private async request<T>(
        endpoint: string,
        options: RequestInit = {}
    ): Promise<T> {
        const url = `${this.baseURL}${endpoint}`;

        // Headers 객체 사용
        const headers = new Headers(options.headers);

        if (!headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json');
        }

        // 토큰 추가
        if (typeof window !== 'undefined') {
            const token = localStorage.getItem('token');
            if (token) {
                headers.set('Authorization', `Bearer ${token}`);
            }
        }

        const config: RequestInit = {
            ...options,
            headers,
        };

        try {
            const response = await fetch(url, config);

            if (response.status === 401 && typeof window !== 'undefined') {
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                window.location.href = '/login';
                throw new Error('Unauthorized');
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP ${response.status}`);
            }

            if (response.status === 204) {
                return {} as T;
            }

            return response.json();
        } catch (error) {
            console.error(`API Error [${endpoint}]:`, error);
            throw error;
        }
    }

    async get<T = any>(endpoint: string): Promise<T> {
        return this.request<T>(endpoint, { method: 'GET' });
    }

    async post<T = any>(endpoint: string, data?: any): Promise<T> {
        return this.request<T>(endpoint, {
            method: 'POST',
            body: data ? JSON.stringify(data) : undefined,
        });
    }

    async put<T = any>(endpoint: string, data?: any): Promise<T> {
        return this.request<T>(endpoint, {
            method: 'PUT',
            body: data ? JSON.stringify(data) : undefined,
        });
    }

    async delete<T = any>(endpoint: string): Promise<T> {
        return this.request<T>(endpoint, { method: 'DELETE' });
    }
}

const apiClient = new ApiClient(API_BASE_URL);

export default apiClient;