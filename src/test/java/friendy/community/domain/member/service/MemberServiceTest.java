package friendy.community.domain.member.service;

import friendy.community.domain.auth.service.AuthService;
import friendy.community.domain.member.dto.request.MemberSignUpRequest;
import friendy.community.domain.member.dto.request.PasswordRequest;
import friendy.community.domain.member.fixture.MemberFixture;
import friendy.community.domain.member.model.Member;
import friendy.community.domain.member.repository.MemberRepository;
import friendy.community.global.exception.ErrorCode;
import friendy.community.global.exception.FriendyException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DirtiesContext
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    AuthService authService;

    @Test
    @DisplayName("회원가입이 성공적으로 처리되면 회원 ID를 반환한다")
    void signUpSuccessfullyReturnsMemberId() {
        // Given
        Member member = MemberFixture.memberFixture();
        MemberSignUpRequest memberSignUpRequest = new MemberSignUpRequest(member.getEmail(), member.getNickname(), member.getPassword(), member.getBirthDate());

        // When
        Long savedId = memberService.signUp(memberSignUpRequest);
        Optional<Member> actualMember = memberRepository.findById(savedId);

        // Then
        assertThat(actualMember).isPresent();
        assertThat(actualMember.get().getEmail()).isEqualTo(member.getEmail());
    }

    @Test
    @DisplayName("이메일이 중복되면 FriendyException을 던진다")
    void throwsExceptionWhenDuplicateEmail() {
        // Given
        Member savedMember = memberRepository.save(MemberFixture.memberFixture());

        // When & Then
        assertThatThrownBy(() -> memberService.assertUniqueEmail(savedMember.getEmail()))
            .isInstanceOf(FriendyException.class)
            .hasMessageContaining("이미 가입된 이메일입니다.")
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("닉네임이 중복되면 FriendyException을 던진다")
    void throwsExceptionWhenDuplicateNickname() {
        // Given
        Member savedMember = memberRepository.save(MemberFixture.memberFixture());

        // When & Then
        assertThatThrownBy(() -> memberService.assertUniqueName(savedMember.getNickname()))
            .isInstanceOf(FriendyException.class)
            .hasMessageContaining("닉네임이 이미 존재합니다.")
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("비밀번호 변경 성공 시 해당 객체의 비밀번호가 변경된다")
    void resetPasswordSuccessfullyPasswordIsChanged() {
        // Given
        Member savedMember = memberRepository.save(MemberFixture.memberFixture());
        PasswordRequest request = new PasswordRequest(savedMember.getEmail(), "newPassword123!");
        String originPassword = savedMember.getPassword();

        // When
        memberService.resetPassword(request);
        Member changedMember = authService.getMemberByEmail(savedMember.getEmail());

        //Then
        assertThat(originPassword).isNotEqualTo(changedMember.getPassword());
    }

    @Test
    @DisplayName("요청받은 이메일이 존재하지 않으면 예외를 던진다")
    void throwsExceptionWhenEmailDosentExists() {
        // Given
        Member savedMember = memberRepository.save(MemberFixture.memberFixture());
        PasswordRequest request = new PasswordRequest("wrongEmail@friendy.com", "newPassword123!");

        // When & Then
        assertThatThrownBy(() -> memberService.resetPassword(request))
                .isInstanceOf(FriendyException.class)
                .hasMessageContaining("해당 이메일의 회원이 존재하지 않습니다.");
    }
}