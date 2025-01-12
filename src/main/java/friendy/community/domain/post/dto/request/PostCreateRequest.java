package friendy.community.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "게시판 생성")
public record PostCreateRequest(

        @Schema(description = "게시글 제목", example = "프렌디제목")
        @NotBlank(message = "게시글 제목이 입력되지 않았습니다")
        String title,

        @Schema(description = "게시글 내용", example = "프렌디게시글내용")
        @NotBlank(message = "게시글 내용이 입력되지 않았습니다")
        String content,

        @Schema(description = "생성날짜" ,example = "2024-08-13")
        @NotNull(message = "생성날짜가 입력되지않았습니다")
        LocalDate createdAt

    ) {
}
