'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import AddressInput from '@/components/common/AddressInput';

interface AddressData {
    postalCode: string;
    address: string;
    addressDetail: string;
    addressType: string;
    sido: string;
    sigungu: string;
    bname: string;
}

export default function RegisterPage() {
    const router = useRouter();
    const [formData, setFormData] = useState({
        loginId: '',
        password: '',
        confirmPassword: '',
        email: '',
        fullName: '',
    });

    const [addressData, setAddressData] = useState<AddressData>({
        postalCode: '',
        address: '',
        addressDetail: '',
        addressType: '',
        sido: '',
        sigungu: '',
        bname: '',
    });

    const [errors, setErrors] = useState<Record<string, string>>({});
    const [isLoading, setIsLoading] = useState(false);
    const [checkingLoginId, setCheckingLoginId] = useState(false);
    const [loginIdAvailable, setLoginIdAvailable] = useState<boolean | null>(null);

    // 로그인 ID 중복 확인
    const checkLoginIdAvailability = async (loginId: string) => {
        if (!loginId || loginId.length < 3) return;

        setCheckingLoginId(true);
        try {
            // 올바른 경로: NEXT_PUBLIC_API_URL = http://localhost:8080/api
            // 최종 URL: http://localhost:8080/api/auth/check-loginId
            const response = await fetch(
                `${process.env.NEXT_PUBLIC_API_URL}/auth/check-loginId?loginId=${loginId}`,
                {
                    credentials: 'include',
                    method: 'GET',
                }
            );

            if (!response.ok) {
                console.error('API 응답 오류:', response.status);
                return;
            }

            const data = await response.json();
            console.log('중복 확인 응답:', data);
            setLoginIdAvailable(data.available);
        } catch (error) {
            console.error('Login ID check error:', error);
            setLoginIdAvailable(null);
        } finally {
            setCheckingLoginId(false);
        }
    };

    const validateForm = () => {
        const newErrors: Record<string, string> = {};

        // LoginId validation
        if (!formData.loginId) {
            newErrors.loginId = '로그인 ID는 필수입니다';
        } else if (formData.loginId.length < 3 || formData.loginId.length > 50) {
            newErrors.loginId = '로그인 ID는 3-50자 사이여야 합니다';
        } else if (!/^[a-zA-Z0-9_-]+$/.test(formData.loginId)) {
            newErrors.loginId = '영문, 숫자, -, _만 사용 가능합니다';
        } else if (loginIdAvailable === false) {
            newErrors.loginId = '이미 사용중인 ID입니다';
        }

        // FullName validation
        if (!formData.fullName) {
            newErrors.fullName = '이름은 필수입니다';
        } else if (formData.fullName.length > 20) {
            newErrors.fullName = '이름은 20자 이하여야 합니다';
        }

        // Email validation
        if (!formData.email) {
            newErrors.email = '이메일은 필수입니다';
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
            newErrors.email = '올바른 이메일 형식이 아닙니다';
        }

        // Password validation - 백엔드 요구사항에 맞춤
        if (!formData.password) {
            newErrors.password = '비밀번호는 필수입니다';
        } else if (formData.password.length < 8) {
            newErrors.password = '비밀번호는 최소 8자 이상이어야 합니다';
        } else if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/.test(formData.password)) {
            newErrors.password = '비밀번호는 대소문자, 숫자, 특수문자(@$!%*?&)를 포함해야 합니다';
        }

        // Confirm password validation
        if (formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = '비밀번호가 일치하지 않습니다';
        }

        // Address validation (선택사항이지만, 입력 시작했으면 필수)
        if (addressData.postalCode && !addressData.address) {
            newErrors.address = '주소를 완성해주세요';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!validateForm()) return;

        setIsLoading(true);

        try {
            const requestData = {
                loginId: formData.loginId,
                password: formData.password,
                email: formData.email,
                fullName: formData.fullName,
                // 주소 정보 (입력된 경우에만 포함)
                ...(addressData.postalCode && {
                    postalCode: addressData.postalCode,
                    address: addressData.address,
                    addressDetail: addressData.addressDetail,
                    addressType: addressData.addressType,
                    sido: addressData.sido,
                    sigungu: addressData.sigungu,
                    bname: addressData.bname,
                }),
            };

            console.log('회원가입 요청 데이터:', requestData);

            const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/auth/register`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(requestData),
            });

            const data = await response.json();
            console.log('회원가입 응답:', data);

            if (data.success) {
                alert('회원가입이 완료되었습니다!');
                router.push('/login');
            } else {
                alert(data.message || '회원가입에 실패했습니다.');
            }
        } catch (error) {
            console.error('Register error:', error);
            alert('서버와의 연결에 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));

        // Clear error when user starts typing
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: '' }));
        }

        // Check loginId availability
        if (name === 'loginId') {
            setLoginIdAvailable(null);
            if (value.length >= 3 && /^[a-zA-Z0-9_-]+$/.test(value)) {
                checkLoginIdAvailability(value);
            }
        }
    };

    const handleAddressChange = (newAddress: AddressData) => {
        setAddressData(newAddress);
        // Clear address error when user inputs address
        if (errors.address) {
            setErrors(prev => ({ ...prev, address: '' }));
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-cyan-50 via-white to-blue-50 flex items-center justify-center px-4 py-12">
            <div className="max-w-2xl w-full">
                {/* Logo & Title */}
                <div className="text-center mb-8">
                    <div className="inline-block p-3 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-2xl mb-4 shadow-lg">
                        <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                        </svg>
                    </div>
                    <h1 className="text-3xl font-bold text-gray-800 mb-2">
                        회원가입
                    </h1>
                    <p className="text-gray-600">
                        영어 학습 플랫폼에 오신 것을 환영합니다
                    </p>
                </div>

                {/* Register Form */}
                <div className="bg-white rounded-2xl shadow-xl p-8 border border-cyan-100">
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {/* 기본 정보 섹션 */}
                        <div className="space-y-5">
                            <h2 className="text-lg font-semibold text-gray-800 border-b pb-2">
                                기본 정보
                            </h2>

                            {/* Login ID */}
                            <div>
                                <label htmlFor="loginId" className="block text-sm font-semibold text-gray-700 mb-2">
                                    로그인 ID <span className="text-cyan-500">*</span>
                                </label>
                                <div className="relative">
                                    <input
                                        type="text"
                                        id="loginId"
                                        name="loginId"
                                        value={formData.loginId}
                                        onChange={handleChange}
                                        pattern="[a-zA-Z0-9_-]+"
                                        minLength={3}
                                        maxLength={50}
                                        className={`w-full px-4 py-3 rounded-xl border-2 ${
                                            errors.loginId
                                                ? 'border-red-300 focus:border-red-500'
                                                : loginIdAvailable === true
                                                    ? 'border-green-300 focus:border-green-500'
                                                    : 'border-gray-200 focus:border-cyan-400'
                                        } focus:outline-none transition-colors duration-200`}
                                        placeholder="영문, 숫자, -, _ 사용 가능"
                                    />
                                    {checkingLoginId && (
                                        <div className="absolute right-3 top-3.5">
                                            <div className="animate-spin h-5 w-5 border-2 border-cyan-500 border-t-transparent rounded-full"></div>
                                        </div>
                                    )}
                                    {!checkingLoginId && loginIdAvailable === true && (
                                        <div className="absolute right-3 top-3.5 text-green-500">
                                            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                            </svg>
                                        </div>
                                    )}
                                </div>
                                {errors.loginId && (
                                    <p className="mt-1 text-sm text-red-500">{errors.loginId}</p>
                                )}
                                {!errors.loginId && loginIdAvailable === true && (
                                    <p className="mt-1 text-sm text-green-500">사용 가능한 ID입니다</p>
                                )}
                            </div>

                            {/* Full Name */}
                            <div>
                                <label htmlFor="fullName" className="block text-sm font-semibold text-gray-700 mb-2">
                                    이름 <span className="text-cyan-500">*</span>
                                </label>
                                <input
                                    type="text"
                                    id="fullName"
                                    name="fullName"
                                    value={formData.fullName}
                                    onChange={handleChange}
                                    maxLength={20}
                                    className={`w-full px-4 py-3 rounded-xl border-2 ${
                                        errors.fullName
                                            ? 'border-red-300 focus:border-red-500'
                                            : 'border-gray-200 focus:border-cyan-400'
                                    } focus:outline-none transition-colors duration-200`}
                                    placeholder="홍길동"
                                />
                                {errors.fullName && (
                                    <p className="mt-1 text-sm text-red-500">{errors.fullName}</p>
                                )}
                            </div>

                            {/* Email */}
                            <div>
                                <label htmlFor="email" className="block text-sm font-semibold text-gray-700 mb-2">
                                    이메일 <span className="text-cyan-500">*</span>
                                </label>
                                <input
                                    type="email"
                                    id="email"
                                    name="email"
                                    value={formData.email}
                                    onChange={handleChange}
                                    className={`w-full px-4 py-3 rounded-xl border-2 ${
                                        errors.email
                                            ? 'border-red-300 focus:border-red-500'
                                            : 'border-gray-200 focus:border-cyan-400'
                                    } focus:outline-none transition-colors duration-200`}
                                    placeholder="example@email.com"
                                />
                                {errors.email && (
                                    <p className="mt-1 text-sm text-red-500">{errors.email}</p>
                                )}
                            </div>
                        </div>

                        {/* 비밀번호 섹션 */}
                        <div className="space-y-5">
                            <h2 className="text-lg font-semibold text-gray-800 border-b pb-2">
                                비밀번호 설정
                            </h2>

                            {/* Password */}
                            <div>
                                <label htmlFor="password" className="block text-sm font-semibold text-gray-700 mb-2">
                                    비밀번호 <span className="text-cyan-500">*</span>
                                </label>
                                <input
                                    type="password"
                                    id="password"
                                    name="password"
                                    value={formData.password}
                                    onChange={handleChange}
                                    minLength={8}
                                    className={`w-full px-4 py-3 rounded-xl border-2 ${
                                        errors.password
                                            ? 'border-red-300 focus:border-red-500'
                                            : 'border-gray-200 focus:border-cyan-400'
                                    } focus:outline-none transition-colors duration-200`}
                                    placeholder="대소문자, 숫자, 특수문자 포함 8자 이상"
                                />
                                {errors.password && (
                                    <p className="mt-1 text-sm text-red-500">{errors.password}</p>
                                )}
                                {!errors.password && formData.password && (
                                    <p className="mt-1 text-xs text-gray-500">
                                        ✓ 대소문자, 숫자, 특수문자(@$!%*?&)를 포함해야 합니다
                                    </p>
                                )}
                            </div>

                            {/* Confirm Password */}
                            <div>
                                <label htmlFor="confirmPassword" className="block text-sm font-semibold text-gray-700 mb-2">
                                    비밀번호 확인 <span className="text-cyan-500">*</span>
                                </label>
                                <input
                                    type="password"
                                    id="confirmPassword"
                                    name="confirmPassword"
                                    value={formData.confirmPassword}
                                    onChange={handleChange}
                                    className={`w-full px-4 py-3 rounded-xl border-2 ${
                                        errors.confirmPassword
                                            ? 'border-red-300 focus:border-red-500'
                                            : 'border-gray-200 focus:border-cyan-400'
                                    } focus:outline-none transition-colors duration-200`}
                                    placeholder="비밀번호를 다시 입력하세요"
                                />
                                {errors.confirmPassword && (
                                    <p className="mt-1 text-sm text-red-500">{errors.confirmPassword}</p>
                                )}
                            </div>
                        </div>

                        {/* 주소 섹션 (선택사항) */}
                        <div className="space-y-5">
                            <h2 className="text-lg font-semibold text-gray-800 border-b pb-2">
                                주소 정보 <span className="text-sm font-normal text-gray-500">(선택사항)</span>
                            </h2>

                            <AddressInput
                                value={addressData}
                                onChange={handleAddressChange}
                                error={errors.address}
                            />
                        </div>

                        {/* Submit Button */}
                        <button
                            type="submit"
                            disabled={isLoading || loginIdAvailable === false}
                            className="w-full bg-gradient-to-r from-cyan-400 to-blue-500 text-white font-semibold py-3 px-6 rounded-xl hover:from-cyan-500 hover:to-blue-600 focus:outline-none focus:ring-4 focus:ring-cyan-300 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg hover:shadow-xl"
                        >
                            {isLoading ? '처리 중...' : '회원가입'}
                        </button>
                    </form>

                    {/* Login Link */}
                    <div className="mt-6 text-center">
                        <p className="text-gray-600">
                            이미 계정이 있으신가요?{' '}
                            <Link
                                href="/login"
                                className="text-cyan-500 hover:text-cyan-600 font-semibold transition-colors"
                            >
                                로그인
                            </Link>
                        </p>
                    </div>
                </div>

                {/* Security Info */}
                <div className="mt-6 text-center text-sm text-gray-500">
                    <p>🔒 모든 정보는 안전하게 암호화되어 저장됩니다</p>
                </div>
            </div>
        </div>
    );
}