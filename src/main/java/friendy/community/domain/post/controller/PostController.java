package friendy.community.domain.post.controller;

import friendy.community.domain.auth.jwt.JwtTokenExtractor;
import friendy.community.domain.auth.jwt.JwtTokenProvider;
import friendy.community.domain.post.dto.request.PostCreateRequest;
import friendy.community.domain.post.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class PostController implements SpringDocPostController {

    private final PostService postService;
    private final JwtTokenExtractor jwtTokenExtractor;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/posts")
    public ResponseEntity<Void> createPost(
            HttpServletRequest httpServletRequest,
            @Valid @RequestBody PostCreateRequest postCreateRequest
            ) {

        return ResponseEntity.created(URI.create("/posts/" + postService.savePost(postCreateRequest, jwtTokenProvider.extractEmailFromAccessToken(jwtTokenExtractor.extractAccessToken(httpServletRequest))))).build();

    }
}
