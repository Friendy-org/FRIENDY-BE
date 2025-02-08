package friendy.community.domain.member.dto.response;

import friendy.community.domain.member.model.Member;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record FindMemberResponse(

        @Schema(description = "해당 프로필의 소유 여부", example = "true")
        Boolean me,

        @Schema(description = "memberId", example = "1")
        Long id,

        @Schema(description = "이메일", example = "example@friendy.com")
        String email,

        @Schema(description = "닉네임", example = "복성김")
        String nickname,

        @Schema(description = "생년월일", example = "2002-08-13")
        LocalDate birthDate

) {
    public static FindMemberResponse from(Member member, boolean isMe) {
        return new FindMemberResponse(
                isMe,
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getBirthDate()
        );
    }

}
