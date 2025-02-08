package friendy.community.domain.member.controller;

import friendy.community.domain.member.dto.request.PasswordRequest;
import friendy.community.domain.member.dto.request.MemberSignUpRequest;
import friendy.community.domain.member.dto.response.FindMemberResponse;
import friendy.community.domain.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class MemberController implements SpringDocMemberController{

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody MemberSignUpRequest request) {
        return ResponseEntity.created(URI.create("/users/" + memberService.signUp(request))).build();
    }

    @PostMapping("/password")
    public ResponseEntity<Void> password(
            @Valid @RequestBody PasswordRequest passwordRequest
    ) {
        memberService.resetPassword(passwordRequest);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<FindMemberResponse> findMember(
            HttpServletRequest httpServletRequest,
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(memberService.getMember(httpServletRequest, memberId));
    }

}
