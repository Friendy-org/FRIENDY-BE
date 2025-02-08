package friendy.community.domain.member.service;

import friendy.community.domain.auth.service.AuthService;
import friendy.community.domain.member.dto.request.MemberSignUpRequest;
import friendy.community.domain.member.dto.request.PasswordRequest;
import friendy.community.domain.member.dto.response.FindMemberResponse;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static friendy.community.domain.auth.fixtures.TokenFixtures.CORRECT_ACCESS_TOKEN;
import static friendy.community.domain.auth.fixtures.TokenFixtures.OTHER_USER_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("회원 조회 요청이 성공하면 FindMemberResponse를 반환한다")
    void getMemberSuccessfullyReturnsFindMemberResponse() {
        // Given
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.addHeader("Authorization", CORRECT_ACCESS_TOKEN);
        Member savedMember = memberRepository.save(MemberFixture.memberFixture());
        Long memberId = savedMember.getId();

        // When
        FindMemberResponse response = memberService.getMember(httpServletRequest, memberId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(memberId);
        assertThat(response.email()).isEqualTo(savedMember.getEmail());
        assertThat(response.nickname()).isEqualTo(savedMember.getNickname());
        assertThat(response.birthDate()).isEqualTo(savedMember.getBirthDate());
    }

    @Test
    @DisplayName("존재하지 않는 회원을 조회하면 예외를 던진다")
    void throwsExceptionWhenMemberNotFound() {
        // Given
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.addHeader("Authorization", CORRECT_ACCESS_TOKEN);
        Long nonExistentMemberId = 999L;

        // When & Then
        assertThatThrownBy(() -> memberService.getMember(httpServletRequest, nonExistentMemberId))
                .isInstanceOf(FriendyException.class)
                .hasMessageContaining("존재하지 않는 회원입니다.")
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("현재 로그인된 사용자와 조회하는 사용자가 같으면 isMe가 true를 반환한다")
    void getMemberIdentifiesCurrentUserCorrectly() {
        // Given
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.addHeader("Authorization", CORRECT_ACCESS_TOKEN);
        Member savedMember = memberRepository.save(MemberFixture.memberFixture());

        // When
        FindMemberResponse response = memberService.getMember(httpServletRequest, savedMember.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(savedMember.getId());
        assertThat(response.me()).isTrue();
    }

    @Test
    @DisplayName("현재 로그인된 사용자와 조회하는 사용자가 다르면 isMe가 false를 반환한다")
    void getMemberIdentifiesDifferentUserCorrectly() {
        // Given
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.addHeader("Authorization", OTHER_USER_TOKEN);
        Member savedMember = memberRepository.save(MemberFixture.memberFixture());

        // When
        FindMemberResponse response = memberService.getMember(httpServletRequest, savedMember.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(savedMember.getId());
        assertThat(response.me()).isFalse();
    }

}