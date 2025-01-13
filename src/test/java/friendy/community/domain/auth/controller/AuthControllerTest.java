package friendy.community.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import friendy.community.domain.auth.dto.request.LoginRequest;
import friendy.community.domain.auth.dto.request.PasswordRequest;
import friendy.community.domain.auth.dto.response.TokenResponse;
import friendy.community.domain.auth.jwt.JwtTokenExtractor;
import friendy.community.domain.auth.service.AuthService;
import friendy.community.global.exception.ErrorCode;
import friendy.community.global.exception.FriendyException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenExtractor jwtTokenExtractor;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("로그인 요청이 성공적으로 처리되면 200 OK와 함께 토큰 헤더가 반환된다")
    void loginSuccessfullyReturnsTokensInHeaders() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("example@friendy.com", "password123!");
        TokenResponse loginResponse = TokenResponse.of("accessToken", "refreshToken");

        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer accessToken"))
                .andExpect(header().string("Authorization-Refresh", "Bearer refreshToken"));
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 401 UNAUTHORIZED 반환")
    void loginWithNonExistentEmailReturns401() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "password123!");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new FriendyException(ErrorCode.UNAUTHORIZED_EMAIL, "해당 이메일의 회원이 존재하지 않습니다."));

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(result ->
                        assertThat(result.getResolvedException().getMessage())
                                .contains("해당 이메일의 회원이 존재하지 않습니다."));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 401 UNAUTHORIZED 반환")
    void loginWithIncorrectPasswordReturns401() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("example@friendy.com", "password123!");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new FriendyException(ErrorCode.UNAUTHORIZED_PASSWORD, "로그인에 실패하였습니다. 비밀번호를 확인해주세요."));

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(result ->
                        assertThat(result.getResolvedException().getMessage())
                                .contains("로그인에 실패하였습니다. 비밀번호를 확인해주세요."));
    }

    @Test
    @DisplayName("이메일이 없으면 400 Bad Request를 반환한다")
    void loginWithoutEmailReturns400BadRequest() throws Exception {
        // Given
        LoginRequest request = new LoginRequest(null, "password123!");

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 400 Bad Request를 반환한다")
    void loginWithInvalidEmailReturns400BadRequest() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("invalid-email", "password123!");

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호가 없으면 400 Bad Request를 반환한다")
    void loginWithoutPasswordReturns400BadRequest() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("example@friendy.com", null);

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @DisplayName("비밀번호가 숫자, 영문자, 특수문자를 포함하지 않으면 400 Bad Request를 반환한다")
    @CsvSource({
            "example@friendy.com, simplepassword, 숫자, 영문자, 특수문자(~!@#$%^&*?)를 포함해야 합니다.",
            "example@friendy.com, password123, 숫자, 영문자, 특수문자(~!@#$%^&*?)를 포함해야 합니다.",
            "example@friendy.com, 12345678, 숫자, 영문자, 특수문자(~!@#$%^&*?)를 포함해야 합니다."
    })
    void loginWithInvalidPasswordPatternReturns400BadRequest(String email, String password, String expectedMessage) throws Exception {
        // Given
        LoginRequest request = new LoginRequest(email, password);

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException().getMessage()).contains(expectedMessage));
    }

    @ParameterizedTest
    @DisplayName("비밀번호가 길이 제한을 벗어나면 400 Bad Request를 반환한다")
    @CsvSource({
            "example@friendy.com, short, 비밀번호는 8~16자 사이로 입력해주세요.",
            "example@friendy.com, thispasswordiswaytoolong123!, 비밀번호는 8~16자 사이로 입력해주세요."
    })
    void loginWithInvalidPasswordLengthReturns400BadRequest(String email, String password, String expectedMessage) throws Exception {
        // Given
        LoginRequest request = new LoginRequest(email, password);

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException().getMessage()).contains(expectedMessage));
    }

    @Test
    @DisplayName("비밀번호 변경이 완료되면 200 OK가 반환된다")
    void resetPasswordSuccessfullyReturns200() throws Exception {
        // Given
        PasswordRequest passwordRequest = new PasswordRequest("example@friendy.com", "newPassword123!");

        // When & Then
        mockMvc.perform(post("/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(passwordRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("요청 이메일이 존재하지 않으면 401 UNAUTHORIZED를 반환한다")
    void emailDosentExistReturns401() throws Exception {
        // Given
        PasswordRequest passwordRequest = new PasswordRequest("wrongEmail@friendy.com", "newPassword123!");

        doThrow(new FriendyException(ErrorCode.UNAUTHORIZED_EMAIL, "해당 이메일의 회원이 존재하지 않습니다."))
                .when(authService)
                .resetPassword(any(PasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(passwordRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(result ->
                        assertThat(result.getResolvedException().getMessage())
                                .contains("해당 이메일의 회원이 존재하지 않습니다."));
    }

    @Test
    @DisplayName("토큰 재발급 요청이 성공적으로 처리되면 200 OK와 함께 새로운 토큰 헤더가 반환된다")
    void reissueTokenSuccessfullyReturnsNewTokensInHeaders() throws Exception {
        // Given
        String refreshToken = "Bearer refreshToken";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization-Refresh", "Bearer refreshToken");

        when(jwtTokenExtractor.extractRefreshToken(any(HttpServletRequest.class)))
                .thenReturn("refreshToken");

        TokenResponse tokenResponse = TokenResponse.of("newAccessToken", "newRefreshToken");
        when(authService.reissueToken("refreshToken")).thenReturn(tokenResponse);

        // When & Then
        mockMvc.perform(post("/token/reissue")
                        .header("Authorization-Refresh", refreshToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer newAccessToken"))
                .andExpect(header().string("Authorization-Refresh", "Bearer newRefreshToken"));
    }

    @Test
    @DisplayName("잘못된 리프레시 토큰으로 토큰 재발급 요청 시 401 UNAUTHORIZED 반환")
    void reissueTokenWithInvalidRefreshTokenReturns401() throws Exception {
        // Given
        String refreshToken = "Bearer invalidRefreshToken";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization-Refresh", refreshToken);

        when(jwtTokenExtractor.extractRefreshToken(any(HttpServletRequest.class)))
                .thenReturn("invalidRefreshToken");

        when(authService.reissueToken("invalidRefreshToken"))
                .thenThrow(new FriendyException(ErrorCode.UNAUTHORIZED_USER, "인증 실패(잘못된 리프레시 토큰) - 토큰 : invalidRefreshToken"));

        // When & Then
        mockMvc.perform(post("/token/reissue")
                        .header("Authorization-Refresh", refreshToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(result.getResolvedException().getMessage())
                        .contains("인증 실패(잘못된 리프레시 토큰) - 토큰 : invalidRefreshToken"));
    }

    @Test
    @DisplayName("만료된 리프레시 토큰으로 토큰 재발급 요청 시 401 UNAUTHORIZED 반환")
    void reissueTokenWithExpiredRefreshTokenReturns401() throws Exception {
        // Given
        String refreshToken = "Bearer expiredRefreshToken";

        when(jwtTokenExtractor.extractRefreshToken(any(HttpServletRequest.class)))
                .thenReturn("expiredRefreshToken");

        when(authService.reissueToken("expiredRefreshToken"))
                .thenThrow(new FriendyException(ErrorCode.UNAUTHORIZED_USER, "인증 실패(만료된 리프레시 토큰) - 토큰 : expiredRefreshToken"));

        // When & Then
        mockMvc.perform(post("/token/reissue")
                        .header("Authorization-Refresh", refreshToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(result.getResolvedException().getMessage())
                        .contains("인증 실패(만료된 리프레시 토큰) - 토큰 : expiredRefreshToken"));
    }

    @Test
    @DisplayName("리프레시 토큰에 이메일 클레임이 없으면 401 UNAUTHORIZED 반환")
    void reissueTokenWithMissingEmailClaimReturns401() throws Exception {
        // Given
        String refreshToken = "Bearer missingEmailClaimToken";

        when(jwtTokenExtractor.extractRefreshToken(any(HttpServletRequest.class)))
                .thenReturn("missingEmailClaimToken");

        when(authService.reissueToken("missingEmailClaimToken"))
                .thenThrow(new FriendyException(ErrorCode.UNAUTHORIZED_USER, "인증 실패(JWT 리프레시 토큰 Payload 이메일 누락) - 토큰 : missingEmailClaimToken"));

        // When & Then
        mockMvc.perform(post("/token/reissue")
                        .header("Authorization-Refresh", refreshToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(result.getResolvedException().getMessage())
                        .contains("인증 실패(JWT 리프레시 토큰 Payload 이메일 누락) - 토큰 : missingEmailClaimToken"));
    }

}
