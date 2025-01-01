package friendy.community.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import friendy.community.domain.member.dto.request.MemberSignUpRequest;
import friendy.community.domain.member.service.MemberService;
import friendy.community.global.exception.ErrorCode;
import friendy.community.global.exception.FriendyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @Test
    @DisplayName("회원가입 요청이 성공적으로 처리되면 201 Created와 함께 응답을 반환한다")
    void signUpSuccessfullyReturns201Created() throws Exception {
        // given
        MemberSignUpRequest memberSignUpRequest = new MemberSignUpRequest("example@friendy.com", "bokSungKim", "password123!", LocalDate.parse("2002-08-13"));

        // Mock Service
        when(memberService.signUp(any(MemberSignUpRequest.class))).thenReturn(1L);

        // When & Then
        mockMvc.perform(post("/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberSignUpRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/users/1"));
    }

    @Test
    @DisplayName("이메일이 없으면 400 Bad Request를 반환한다")
    void signUpWithoutEmailReturns400BadRequest() throws Exception {
        // Given
        MemberSignUpRequest memberSignUpRequest = new MemberSignUpRequest(null, "bokSungKim", "password123!", LocalDate.parse("2002-08-13"));

        // When & Then
        mockMvc.perform(post("/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberSignUpRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 400 Bad Request를 반환한다")
    void signUpWithInvalidEmailReturns400BadRequest() throws Exception {
        // Given
        MemberSignUpRequest memberSignUpRequest = new MemberSignUpRequest("invalid-email", "bokSungKim", "password123!", LocalDate.parse("2002-08-13"));

        // When & Then
        mockMvc.perform(post("/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberSignUpRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이메일이 중복되면 409 Conflict를 반환한다")
    void signUpWithDuplicateEmailReturns409Conflict() throws Exception {
        // Given
        MemberSignUpRequest memberSignUpRequest = new MemberSignUpRequest("duplicate@friendy.com", "bokSungKim", "password123!", LocalDate.parse("2002-08-13"));

        // Mock Service
        when(memberService.signUp(any(MemberSignUpRequest.class)))
                .thenThrow(new FriendyException(ErrorCode.DUPLICATE_EMAIL, "이미 가입된 이메일입니다."));

        // When & Then
        mockMvc.perform(post("/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberSignUpRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(result ->
                            assertThat(result.getResolvedException().getMessage())
                                    .contains("이미 가입된 이메일입니다."));
    }

    @Test
    @DisplayName("닉네임이 없으면 400 Bad Request를 반환한다")
    void signUpWithoutNicknameReturns400BadRequest() throws Exception {
        // Given
        MemberSignUpRequest memberSignUpRequest = new MemberSignUpRequest("example@friendy.com", null, "password123!", LocalDate.parse("2002-08-13"));

        // When & Then
        mockMvc.perform(post("/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberSignUpRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @DisplayName("닉네임이 길이 제한을 벗어나면 400 Bad Request를 반환한다")
    @CsvSource({
            "example@friendy.com, a, password123!, 닉네임은 2~20자 사이로 입력해주세요.",
            "example@friendy.com, thisisaveryverylongnickname, password123!, 닉네임은 2~20자 사이로 입력해주세요."
    })
    void signUpWithInvalidNicknameLengthReturns400BadRequest(
            String email, String nickname, String password, String expectedMessage) throws Exception {
        // Given
        MemberSignUpRequest request = new MemberSignUpRequest(email, nickname, password, LocalDate.parse("2002-08-13"));

        // When & Then
        mockMvc.perform(post("/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(result ->
                            assertThat(result.getResolvedException().getMessage())
                                    .contains(expectedMessage));
    }

    @Test
    @DisplayName("닉네임이 중복되면 409 Conflict를 반환한다")
    void signUpWithDuplicateNicknameReturns409Conflict() throws Exception {
        // Given
        MemberSignUpRequest memberSignUpRequest = new MemberSignUpRequest("example@friendy.com", "duplicateNickname", "password123!", LocalDate.parse("2002-08-13"));

        // Mock Service
        when(memberService.signUp(any(MemberSignUpRequest.class)))
                .thenThrow(new FriendyException(ErrorCode.DUPLICATE_NICKNAME, "닉네임이 이미 존재합니다."));

        // When & Then
        mockMvc.perform(post("/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(memberSignUpRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(result ->
                            assertThat(result.getResolvedException().getMessage())
                                    .contains("닉네임이 이미 존재합니다."));
    }

    @Test
    @DisplayName("비밀번호가 없으면 400 Bad Request를 반환한다")
    void signUpWithoutPasswordReturns400BadRequest() throws Exception {
        // Given
        MemberSignUpRequest memberSignUpRequest = new MemberSignUpRequest("example@friendy.com", "bokSungKim", null, LocalDate.parse("2002-08-13"));

        // When & Then
        mockMvc.perform(post("/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberSignUpRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @DisplayName("비밀번호가 숫자, 영문자, 특수문자를 포함하지 않으면 400 Bad Request를 반환한다")
    @CsvSource({
            "example@friendy.com, validNickname, simplepassword, 숫자, 영문자, 특수문자(~!@#$%^&*?)를 포함해야 합니다.",
            "example@friendy.com, validNickname, password123, 숫자, 영문자, 특수문자(~!@#$%^&*?)를 포함해야 합니다.",
            "example@friendy.com, validNickname, 12345678, 숫자, 영문자, 특수문자(~!@#$%^&*?)를 포함해야 합니다."
    })
    void signUpWithInvalidPasswordPatternReturns400BadRequest(
            String email, String nickname, String password, String expectedMessage) throws Exception {
        // Given
        MemberSignUpRequest request = new MemberSignUpRequest(email, nickname, password, LocalDate.parse("2002-08-13"));

        // When & Then
        mockMvc.perform(post("/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(result ->
                            assertThat(result.getResolvedException().getMessage()).contains(expectedMessage));
    }

    @ParameterizedTest
    @DisplayName("비밀번호가 길이 제한을 벗어나면 400 Bad Request를 반환한다")
    @CsvSource({
            "example@friendy.com, bokSungKim, short, 비밀번호는 8~16자 사이로 입력해주세요.",
            "example@friendy.com, bokSungKim, thispasswordiswaytoolong123!, 비밀번호는 8~16자 사이로 입력해주세요."
    })
    void signUpWithInvalidPasswordLengthReturns400BadRequest(
            String email, String nickname, String password, String expectedMessage) throws Exception {
        // Given
        MemberSignUpRequest request = new MemberSignUpRequest(email, nickname, password, LocalDate.parse("2002-08-13"));

        // When & Then
        mockMvc.perform(post("/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(result ->
                            assertThat(result.getResolvedException().getMessage())
                                    .contains(expectedMessage));
    }

    @Test
    @DisplayName("생년월일이 없으면 400 Bad Request를 반환한다")
    void signUpWithoutBirthDateReturns400BadRequest() throws Exception {
        // Given
        MemberSignUpRequest memberSignUpRequest = new MemberSignUpRequest("example@friendy.com", "bokSungKim", "password123!", null);

        // When & Then
        mockMvc.perform(post("/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberSignUpRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

}