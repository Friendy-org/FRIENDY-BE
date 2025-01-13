package friendy.community.domain.post.controller;

import friendy.community.domain.post.dto.request.PostCreateRequest;
import friendy.community.global.swagger.error.ApiErrorResponse;
import friendy.community.global.swagger.error.ErrorCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "게시글 API", description = "게시글 생성 API")
public interface SpringDocPostController {

    @Operation(summary = "게시글 생성", description = "새 게시글을 생성합니다.")
    @ApiResponse(responseCode = "201", description = "게시글 생성 성공")
    @ApiErrorResponse(status = HttpStatus.BAD_REQUEST, instance = "/posts/create", errorCases = {
        @ErrorCase(description = "게시글 제목 없음", exampleMessage = "게시글 제목을 입력해주세요."),
        @ErrorCase(description = "게시글 내용 없음", exampleMessage = "게시글 내용을 입력해주세요.")
    })
    ResponseEntity<Void> createPost(
        HttpServletRequest httpServletRequest,
        @RequestBody PostCreateRequest postRequest
    );
}
