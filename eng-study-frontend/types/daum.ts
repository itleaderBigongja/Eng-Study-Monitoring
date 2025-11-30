/**
 * Daum 우편번호 서비스 타입 정의
 */

declare global {
    interface Window {
        daum: {
            Postcode: new (options: DaumPostcodeOptions) => DaumPostcodeEmbed;
        };
    }
}

export interface DaumPostcodeData {
    zonecode: string;          // 우편번호
    address: string;           // 기본 주소
    addressEnglish: string;    // 영문 주소
    addressType: 'R' | 'J';    // R: 도로명, J: 지번
    userSelectedType: 'R' | 'J';
    roadAddress: string;       // 도로명 주소
    jibunAddress: string;      // 지번 주소
    bname: string;             // 법정동/법정리 이름
    buildingName: string;      // 건물명
    apartment: 'Y' | 'N';      // 아파트 여부
    sido: string;              // 시/도
    sigungu: string;           // 시/군/구
    sigunguCode: string;       // 시/군/구 코드
    roadnameCode: string;      // 도로명 코드
    bcode: string;             // 법정동/법정리 코드
    roadname: string;          // 도로명
}

export interface DaumPostcodeOptions {
    oncomplete: (data: DaumPostcodeData) => void;
    onclose?: () => void;
    width?: string | number;
    height?: string | number;
}

export interface DaumPostcodeEmbed {
    embed: (element: HTMLElement) => void;
    open: () => void;
}