package friendy.community.domain.member.controller;

import friendy.community.domain.member.dto.request.MemberSignUpRequest;
import friendy.community.domain.member.dto.request.PasswordRequest;
import friendy.community.domain.member.dto.response.FindMemberResponse;
import friendy.community.global.swagger.error.ApiErrorResponse;
import friendy.community.global.swagger.error.ErrorCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "회원 API", description = "회원 API")
public interface SpringDocMemberController {
    @Operation(summary = "회원가입")
    @ApiResponse(responseCode = "201", description = "회원가입 성공")
    @ApiErrorResponse(status = HttpStatus.BAD_REQUEST, instance = "/signup", errorCases = {
            @ErrorCase(description = "이메일 입력 없음", exampleMessage = "이메일이 입력되지 않았습니다."),
            @ErrorCase(description = "이메일 형식 오류", exampleMessage = "이메일 형식으로 입력해주세요."),
            @ErrorCase(description = "닉네임 입력 없음", exampleMessage = "닉네임이 입력되지 않았습니다."),
            @ErrorCase(description = "닉네임 글자수 오류", exampleMessage = "닉네임은 2~20자 사이로 입력해주세요."),
            @ErrorCase(description = "비밀번호 입력 없음", exampleMessage = "비밀번호가 입력되지 않았습니다."),
            @ErrorCase(description = "비밀번호 형식 오류", exampleMessage = "숫자, 영문자, 특수문자(~!@#$%^&*?)를 포함해야 합니다."),
            @ErrorCase(description = "비밀번호 글자수 오류", exampleMessage = "비밀번호는 8~16자 사이로 입력해주세요."),
            @ErrorCase(description = "생년월일 입력 없음", exampleMessage = "생년월일이 입력되지 않았습니다."),
    })
    @ApiErrorResponse(status = HttpStatus.CONFLICT, instance = "/signup", errorCases = {
            @ErrorCase(description = "이메일 중복", exampleMessage = "이미 가입된 이메일입니다."),
            @ErrorCase(description = "닉네임 중복", exampleMessage = "닉네임이 이미 존재합니다."),
    })
    ResponseEntity<Void> signUp(MemberSignUpRequest request);

    @Operation(summary = "비밀번호 변경")
    @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공")
    @ApiErrorResponse(status = HttpStatus.UNAUTHORIZED, instance = "/auth/password", errorCases = {
            @ErrorCase(description = "이메일 불일치", exampleMessage = "해당 이메일의 회원이 존재하지 않습니다.")
    })
    ResponseEntity<Void> password(PasswordRequest passwordRequest);

    @Operation(summary = "프로필 조회")
    @ApiResponse(responseCode = "200", description = "프로필 조회 성공")
    @ApiErrorResponse(status = HttpStatus.NOT_FOUND, instance = "/member/{memberId}", errorCases = {
            @ErrorCase(description = "존재하지 않는 회원", exampleMessage = "존재하지 않는 회원입니다.")
    })
    ResponseEntity<FindMemberResponse> findMember(
            HttpServletRequest httpServletRequest,
            @PathVariable Long memberId
    );

}
