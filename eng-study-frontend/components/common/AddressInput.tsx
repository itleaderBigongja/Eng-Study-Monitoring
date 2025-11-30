'use client';

import { useEffect, useState } from 'react';
import { DaumPostcodeData } from '@/types/daum';

interface AddressInputProps {
    value: {
        postalCode: string;
        address: string;
        addressDetail: string;
        addressType: string;
        sido: string;
        sigungu: string; // RegisterPage의 상태와 이름 통일
        bname: string;
    };
    onChange: (address: AddressInputProps['value']) => void;
    error?: string;
}

export default function AddressInput({ value, onChange, error }: AddressInputProps) {
    const [isScriptLoaded, setIsScriptLoaded] = useState(false);

    useEffect(() => {
        // Daum 우편번호 서비스 스크립트 로드
        const script = document.createElement('script');
        script.src = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
        script.async = true;
        script.onload = () => setIsScriptLoaded(true);
        document.body.appendChild(script);

        return () => {
            document.body.removeChild(script);
        };
    }, []);

    const handleSearchAddress = () => {
        if (!isScriptLoaded || !window.daum) {
            alert('주소 검색 서비스를 불러오는 중입니다. 잠시 후 다시 시도해주세요.');
            return;
        }

        new window.daum.Postcode({
            oncomplete: (data: DaumPostcodeData) => {
                // 사용자가 선택한 주소 타입(R:도로명, J:지번)에 따라 주소 값 설정
                const address = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress;

                onChange({
                    postalCode: data.zonecode,
                    address: address,
                    addressDetail: '', // 주소 검색 시 상세주소 초기화
                    addressType: data.addressType,
                    sido: data.sido,
                    sigungu: data.sigungu, // API의 sigungu를 상태의 sigungu에 매핑
                    bname: data.bname,
                });
            },
        }).open();
    };

    const handleDetailChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        onChange({
            ...value,
            addressDetail: e.target.value,
        });
    };

    return (
        <div className="space-y-3">
            {/* 우편번호 + 검색 버튼 */}
            <div className="flex gap-2">
                <input
                    type="text"
                    value={value.postalCode}
                    readOnly
                    placeholder="우편번호"
                    className={`w-1/3 px-4 py-3 rounded-xl border-2 ${
                        error
                            ? 'border-red-300 focus:border-red-500'
                            : 'border-gray-200 focus:border-cyan-400'
                    } focus:outline-none transition-colors duration-200 bg-gray-50`}
                />
                <button
                    type="button"
                    onClick={handleSearchAddress}
                    className="flex-1 bg-gradient-to-r from-cyan-400 to-blue-500 text-white font-semibold rounded-xl hover:from-cyan-500 hover:to-blue-600 transition-all shadow-md hover:shadow-lg whitespace-nowrap"
                >
                    주소 검색
                </button>
            </div>

            {/* 기본 주소 */}
            <input
                type="text"
                value={value.address}
                readOnly
                placeholder="주소"
                className={`w-full px-4 py-3 rounded-xl border-2 ${
                    error
                        ? 'border-red-300 focus:border-red-500'
                        : 'border-gray-200 focus:border-cyan-400'
                } focus:outline-none transition-colors duration-200 bg-gray-50`}
            />

            {/* 상세 주소 */}
            <input
                type="text"
                value={value.addressDetail}
                onChange={handleDetailChange}
                placeholder="상세 주소 (선택사항)"
                className={`w-full px-4 py-3 rounded-xl border-2 ${
                    error
                        ? 'border-red-300 focus:border-red-500'
                        : 'border-gray-200 focus:border-cyan-400'
                } focus:outline-none transition-colors duration-200`}
            />

            {/* 에러 메시지 */}
            {error && (
                <p className="text-sm text-red-500">{error}</p>
            )}

            {/* (선택) 주소 정보 디버깅용 표시 - 배포 시 제거 가능 */}
            {value.sido && (
                <p className="text-xs text-gray-400 pl-1">
                    {value.sido} {value.sigungu} {value.bname}
                </p>
            )}
        </div>
    );
}