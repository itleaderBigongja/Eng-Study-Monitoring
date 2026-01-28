package com.eng.study.engstudy;

import com.eng.study.engstudy.controller.AuthController;
import com.eng.study.engstudy.model.dto.request.LoginRequestDTO;
import com.eng.study.engstudy.model.dto.request.RegisterRequestDTO;
import com.eng.study.engstudy.model.dto.response.AuthResponseDTO;
import com.eng.study.engstudy.service.AuthService;
import com.eng.study.engstudy.util.CookieUtil;
import com.eng.study.engstudy.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class) // AuthController만 로드하여 테스트
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청을 시뮬레이션하는 객체

    @Autowired
    private ObjectMapper objectMapper; // JSON 직렬화/역직렬화 유틸리티

    @MockitoBean
    private AuthService authService; // AuthController의 의존성 Mocking

    @MockitoBean
    private CookieUtil cookieUtil; // AuthController의 의존성 Mocking

    @MockitoBean
    private JwtUtil jwtUtil; // AuthController의 의존성 Mocking

    @Autowired
    private WebApplicationContext ctx;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(new CharacterEncodingFilter("UTF-8", true)) // 한글 깨짐 방지
                .alwaysDo(print()) // 모든 요청/응답 내용 출력
                .build();
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void registerSuccess() throws Exception {
        RegisterRequestDTO requestDTO = new RegisterRequestDTO();
        requestDTO.setLoginId("testUser");
        requestDTO.setEmail("test@example.com");
        requestDTO.setPassword("Password123!");
        requestDTO.setFullName("테스트 사용자");

        AuthResponseDTO mockResponse = AuthResponseDTO.builder()
                .usersId(1L)
                .loginId("testUser")
                .accessToken("mockAccessToken")
                .refreshToken("mockRefreshToken")
                .build();

        // authService.register() 호출 시 mockResponse 반환하도록 설정
        when(authService.register(any(RegisterRequestDTO.class))).thenReturn(mockResponse);
        // cookieUtil.addAccessTokenCookie() 호출 시 아무것도 하지 않도록 설정
        doNothing().when(cookieUtil).addAccessTokenCookie(any(), anyString(), anyLong());
        doNothing().when(cookieUtil).addRefreshTokenCookie(any(), anyString(), anyLong());
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L); // 예시 만료 시간
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(604800000L); // 예시 만료 시간

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated()) // HTTP 201 Created
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist()) // 응답에 토큰은 포함되지 않음
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());

        // authService.register가 한 번 호출되었는지 검증
        verify(authService, times(1)).register(any(RegisterRequestDTO.class));
        // 쿠키가 추가되었는지 검증
        verify(cookieUtil, times(1)).addAccessTokenCookie(any(), anyString(), anyLong());
        verify(cookieUtil, times(1)).addRefreshTokenCookie(any(), anyString(), anyLong());
    }

    @Test
    @DisplayName("회원가입 실패_입력값_오류 테스트")
    void registerFailureInvalidInput() throws Exception {
        RegisterRequestDTO requestDTO = new RegisterRequestDTO();
        // 유효하지 않은 비밀번호 (8자 미만)
        requestDTO.setLoginId("testUser");
        requestDTO.setEmail("test@example.com");
        requestDTO.setPassword("short");
        requestDTO.setFullName("테스트 사용자");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest()) // HTTP 400 Bad Request
                .andExpect(jsonPath("$.success").value(false));

        // authService.register는 호출되지 않았을 것임 (컨트롤러 @Valid에서 걸러짐)
        verify(authService, times(0)).register(any(RegisterRequestDTO.class));
    }

    @Test
    @DisplayName("회원가입 실패_서비스_로직_오류 테스트")
    void registerFailureServiceError() throws Exception {
        RegisterRequestDTO requestDTO = new RegisterRequestDTO();
        requestDTO.setLoginId("testUser");
        requestDTO.setEmail("test@example.com");
        requestDTO.setPassword("Password123!");
        requestDTO.setFullName("테스트 사용자");

        // authService.register() 호출 시 IllegalArgumentException 발생하도록 설정
        doThrow(new IllegalArgumentException("이미 사용중인 로그인 ID입니다."))
                .when(authService).register(any(RegisterRequestDTO.class));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest()) // HTTP 400 Bad Request
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용중인 로그인 ID입니다."));

        verify(authService, times(1)).register(any(RegisterRequestDTO.class));
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void loginSuccess() throws Exception {
        LoginRequestDTO requestDTO = new LoginRequestDTO();
        requestDTO.setLoginId("testUser");
        requestDTO.setPassword("Password123!");

        AuthResponseDTO mockResponse = AuthResponseDTO.builder()
                .usersId(1L)
                .loginId("testUser")
                .accessToken("mockAccessToken")
                .refreshToken("mockRefreshToken")
                .build();

        when(authService.login(any(LoginRequestDTO.class))).thenReturn(mockResponse);
        doNothing().when(cookieUtil).addAccessTokenCookie(any(), anyString(), anyLong());
        doNothing().when(cookieUtil).addRefreshTokenCookie(any(), anyString(), anyLong());
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(604800000L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk()) // HTTP 200 OK
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());

        verify(authService, times(1)).login(any(LoginRequestDTO.class));
        verify(cookieUtil, times(1)).addAccessTokenCookie(any(), anyString(), anyLong());
        verify(cookieUtil, times(1)).addRefreshTokenCookie(any(), anyString(), anyLong());
    }

    @Test
    @DisplayName("로그인 실패_비밀번호_불일치 테스트")
    void loginFailureUnauthorized() throws Exception {
        LoginRequestDTO requestDTO = new LoginRequestDTO();
        requestDTO.setLoginId("testUser");
        requestDTO.setPassword("wrongPassword");

        doThrow(new IllegalArgumentException("비밀번호가 일치하지 않습니다."))
                .when(authService).login(any(LoginRequestDTO.class));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isUnauthorized()) // HTTP 401 Unauthorized
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));

        verify(authService, times(1)).login(any(LoginRequestDTO.class));
    }

    @Test
    @DisplayName("로그아웃 성공 테스트")
    void logoutSuccess() throws Exception {
        doNothing().when(cookieUtil).deleteAllAuthCookies(any());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk()) // HTTP 200 OK
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));

        verify(cookieUtil, times(1)).deleteAllAuthCookies(any());
    }

    @Test
    @DisplayName("토큰 갱신 성공 테스트")
    void refreshTokenRenewalSuccess() throws Exception {
        String mockRefreshToken = "validRefreshToken";
        AuthResponseDTO mockResponse = AuthResponseDTO.builder()
                .accessToken("newAccessToken")
                .refreshToken("newRefreshToken")
                .build();

        when(cookieUtil.getRefreshToken(any())).thenReturn(Optional.of(mockRefreshToken));
        when(authService.refreshTokenRenewal(anyString())).thenReturn(mockResponse);
        doNothing().when(cookieUtil).addAccessTokenCookie(any(), anyString(), anyLong());
        doNothing().when(cookieUtil).addRefreshTokenCookie(any(), anyString(), anyLong());
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(604800000L);

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("토큰이 갱신되었습니다."));

        verify(cookieUtil, times(1)).getRefreshToken(any());
        verify(authService, times(1)).refreshTokenRenewal(mockRefreshToken);
        verify(cookieUtil, times(1)).addAccessTokenCookie(any(), anyString(), anyLong());
        verify(cookieUtil, times(1)).addRefreshTokenCookie(any(), anyString(), anyLong());
    }

    @Test
    @DisplayName("내 정보 조회 성공 테스트")
    void getMyInfoSuccess() throws Exception {
        String mockAccessToken = "validAccessToken";
        AuthResponseDTO.UserInfo mockUserInfo = AuthResponseDTO.UserInfo.builder()
                .usersId(1L)
                .loginId("testUser")
                .fullName("테스트 사용자")
                .email("test@example.com")
                .build();

        when(cookieUtil.getAccessToken(any())).thenReturn(Optional.of(mockAccessToken));
        when(jwtUtil.validateToken(mockAccessToken)).thenReturn(true);
        when(jwtUtil.getUsersId(mockAccessToken)).thenReturn(1L);
        when(authService.getMyInfo(1L)).thenReturn(mockUserInfo);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("testUser"));

        verify(cookieUtil, times(1)).getAccessToken(any());
        verify(jwtUtil, times(1)).validateToken(mockAccessToken);
        verify(jwtUtil, times(1)).getUsersId(mockAccessToken);
        verify(authService, times(1)).getMyInfo(1L);
    }
}
