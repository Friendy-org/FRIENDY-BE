package friendy.community.domain.post.service;

import friendy.community.domain.auth.jwt.JwtTokenExtractor;
import friendy.community.domain.auth.jwt.JwtTokenProvider;
import friendy.community.domain.auth.service.AuthService;
import friendy.community.domain.hashtag.service.HashtagService;
import friendy.community.domain.member.model.Member;
import friendy.community.domain.post.dto.request.PostCreateRequest;
import friendy.community.domain.post.dto.request.PostUpdateRequest;
import friendy.community.domain.post.dto.response.FindPostResponse;
import friendy.community.domain.post.dto.response.FindAllPostResponse;
import friendy.community.domain.post.model.Post;
import friendy.community.domain.post.repository.PostQueryDSLRepository;
import friendy.community.domain.post.repository.PostRepository;
import friendy.community.global.exception.ErrorCode;
import friendy.community.global.exception.FriendyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final PostQueryDSLRepository postQueryDSLRepository;
    private final JwtTokenExtractor jwtTokenExtractor;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final HashtagService hashtagService;

    public long savePost(final PostCreateRequest postCreateRequest, final HttpServletRequest httpServletRequest) {
        final Member member = getMemberFromRequest(httpServletRequest);
        final Post post = Post.of(postCreateRequest, member);
        postRepository.save(post);

        hashtagService.saveHashtags(post, postCreateRequest.hashtags());

        return post.getId();
    }

    public long updatePost(
            final PostUpdateRequest postUpdateRequest,
            final HttpServletRequest httpServletRequest,
            final Long postId
    ) {
        final Member member = getMemberFromRequest(httpServletRequest);
        final Post post = validatePostExistence(postId);
        validatePostAuthor(member,post);

        post.updatePost(postUpdateRequest);
        postRepository.save(post);

        hashtagService.updateHashtags(post, postUpdateRequest.hashtags());

        return post.getId();
    }

    public void deletePost(final HttpServletRequest httpServletRequest, final Long postId) {
        final Member member = getMemberFromRequest(httpServletRequest);
        final Post post = validatePostExistence(postId);
        validatePostAuthor(member,post);

        hashtagService.deleteHashtags(postId);
        postRepository.delete(post);
    }

    public FindPostResponse getPost(final Long postId){
        Post post = postQueryDSLRepository.findPostById(postId)
                .orElseThrow(() -> new FriendyException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 게시글입니다."));

        return FindPostResponse.from(post);
    }

    public FindAllPostResponse getAllPosts(Pageable pageable) {
        Pageable defaultPageable = PageRequest.of(pageable.getPageNumber(), 10);
        Page<Post> postPage = postQueryDSLRepository.findAllPosts(defaultPageable);

        validatePageNumber(defaultPageable.getPageNumber(), postPage);
        List<FindPostResponse> findPostResponses = postPage.getContent().stream()
                .map(FindPostResponse::from)
                .toList();

        return new FindAllPostResponse(findPostResponses, postPage.getTotalPages());
    }

    private Post validatePostExistence(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new FriendyException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 게시글입니다."));
    }

    private void validatePostAuthor(Member member, Post post) {
        if (!post.getMember().getId().equals(member.getId())) {
            throw new FriendyException(ErrorCode.FORBIDDEN_ACCESS, "게시글은 작성자 본인만 관리할 수 있습니다.");
        }
    }

    private void validatePageNumber(int requestedPage, Page<?> page) {
        if (requestedPage >= page.getTotalPages()) {
            throw new FriendyException(ErrorCode.RESOURCE_NOT_FOUND, "요청한 페이지가 존재하지 않습니다.");
        }
    }

    private Member getMemberFromRequest(HttpServletRequest httpServletRequest) {
        final String accessToken = jwtTokenExtractor.extractAccessToken(httpServletRequest);
        final String email = jwtTokenProvider.extractEmailFromAccessToken(accessToken);
        return authService.getMemberByEmail(email);
    }

}