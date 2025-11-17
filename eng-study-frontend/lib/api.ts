/**
 * Next.js 환경에서 API호출을 공통으로 관리하는 모듈(api.ts)
 * React의 Axios 인스턴스와 유사한 역할을 한다.
 */

// 백엔드 API 서버의 기본 URL
// 실제 배포 시에는 환경 변수(process.env.NEXT_PUBLIC_API_URL)를 사용하는 것이 좋습니다.
// eng-study의 tomcat 포트번호 : 8080
const BASE_URL = 'http://localhost:8080/api';

/**
 * fetch 함수를 감싸서 공통 헤더, 에러 처리 등을 추가
 * @param path API 엔드포인트 경로( 예: '/hello' )
 * @param options fetch 함수에 전달할 옵션( method, body 등)
 * @return Promise<Response>
 */
const apiFetch = async (path: string, options: RequestInit = {}) => {
    const url = `${BASE_URL}${path}`;

    // 기본 헤더 설정( JSON 통신 가정 )
    const defaultHeaders = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    try {
        const response = await fetch(url, {
            ...options,
            headers: defaultHeaders,
            /**
             * Next.js 13+의 fetch 캐싱 설정(중요!)
             * no-store : 항상 최신 데이터를 서버에서 가져온다.(캐시 사용 안함)
             * force-cache : (기본값) 최대한 캐시를 사용
             * { next: {revalidate:60} } : 60초마다 캐시를 갱신한다.
             *
             * 데이터가 자주 바뀌지 않는 페이지는 캐시를,
             * 실시간성이 중요한 데이터는 no-store를 사용한다.
             */
            cache: 'no-store',
        })

        // HTTP 응답이 200-299 범위가 아닐 경우 에러 처리
        if(!response.ok) {
            console.error('API Error : ', response.status, response.statusText);
            // 나중에 공통 에러 UI 처리 로직을 추가할 수 있다.
            throw new Error(`API call failed with status ${response.status}`);
        }

        // JSON 응답을 파싱하여 반환
        return response.json();

    } catch (error) {
        console.error('Fatch Error', error);
        throw error;    // 이제 다시 던져서 호출한 곳에서 처리할 수 있게 함
    }
};

// 사용 예시를 위해 공통 API 함수들을 미리 정의

// ( 나중에 컴포넌트에서 이 함수들을 import해서 사용 )
export const api = {
    /**
     * GET /api/hello 테스트용 함수
     **/
    getHello: async () => {
        return await apiFetch('/hello', {method: 'GET'})
    },

    /**
     * POST /api/auth/login( 로그인 예시 )
     * @param email
     * @param password
     **/
    login: async (email:string, password:string) => {
        return await apiFetch('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password})
        })
    }
}