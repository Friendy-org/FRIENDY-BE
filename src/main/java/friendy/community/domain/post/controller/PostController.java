//package friendy.community.domain.post.controller;
//
//import friendy.community.domain.post.dto.request.PostCreateRequest;
//import friendy.community.domain.post.service.PostService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequiredArgsConstructor
//public class PostController implements SpringDocPostController {
//
//    private final PostService postService;
//
//    @PostMapping("/auth/posts/create/{memberId}")
//    public ResponseEntity<Void> createPost(
//            @PathVariable Long memberId,
//            @Valid @RequestBody PostCreateRequest postCreateRequest
//            ) {
//        postService.createPost(postCreateRequest, memberId);
//        return ResponseEntity.status(HttpStatus.CREATED).build();
//    }
//}
