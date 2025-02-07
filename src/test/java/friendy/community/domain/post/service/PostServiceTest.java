package friendy.community.domain.post.service;

import friendy.community.domain.member.dto.request.MemberSignUpRequest;
import friendy.community.domain.member.fixture.MemberFixture;
import friendy.community.domain.member.model.Member;
import friendy.community.domain.member.repository.MemberRepository;
import friendy.community.domain.member.service.MemberService;
import friendy.community.domain.post.dto.request.PostCreateRequest;
import friendy.community.domain.post.dto.request.PostUpdateRequest;
import friendy.community.domain.post.dto.response.FindAllPostResponse;
import friendy.community.domain.post.dto.response.FindPostResponse;
import friendy.community.domain.post.fixture.PostFixture;
import friendy.community.domain.post.model.Post;
import friendy.community.domain.post.repository.PostRepository;
import friendy.community.global.exception.ErrorCode;
import friendy.community.global.exception.FriendyException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.List;

import static friendy.community.domain.auth.fixtures.TokenFixtures.CORRECT_ACCESS_TOKEN;
import static friendy.community.domain.auth.fixtures.TokenFixtures.OTHER_USER_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
@DirtiesContext
class PostServiceTest {

    @Autowired
    private PostService postService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private EntityManager entityManager;

    private Member member;
    private MockHttpServletRequest httpServletRequest;

    @BeforeEach
    void setup() {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.addHeader("Authorization", CORRECT_ACCESS_TOKEN);

        member = MemberFixture.memberFixture();
        memberService.signUp(new MemberSignUpRequest(
                member.getEmail(), member.getNickname(), member.getPassword(), member.getBirthDate()));

        resetPostIdSequence();
    }

    private void resetPostIdSequence() {
        entityManager.createNativeQuery("ALTER TABLE post AUTO_INCREMENT = 1").executeUpdate();
    }

    private Long createPost() {
        Post post = PostFixture.postFixture();
        return postService.savePost(new PostCreateRequest(post.getContent(), List.of("프렌디", "개발", "스터디")), httpServletRequest);
    }

    private void signUpOtherUser() {
        memberService.signUp(new MemberSignUpRequest(
                "user@example.com", "홍길동", "password123!", LocalDate.parse("2002-08-13")));
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.addHeader("Authorization", OTHER_USER_TOKEN);
    }

    @Test
    @DisplayName("게시글 생성 성공 시 게시글 ID 반환")
    void createPostSuccessfullyReturnsPostId() {
        // Given & When
        Long postId = createPost();

        // Then
        assertThat(postId).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 게시글 생성 시 예외 발생")
    void throwsExceptionWhenEmailNotFound() {
        // Given
        memberRepository.deleteAll();

        // When & Then
        assertThatThrownBy(this::createPost)
                .isInstanceOf(FriendyException.class)
                .hasMessageContaining("해당 이메일의 회원이 존재하지 않습니다.")
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_EMAIL);
    }

    @Test
    @DisplayName("게시글 수정 성공 시 게시글 ID 반환")
    void updatePostSuccessfullyReturnsPostId() {
        // Given
        createPost();
        PostUpdateRequest request = new PostUpdateRequest("Updated content", List.of("업데이트"));

        // When
        Long postId = postService.updatePost(request, httpServletRequest, 1L);
        Post updatedPost = postRepository.findById(1L)
                .orElseThrow(() -> new FriendyException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 게시글입니다."));

        // Then
        assertThat(postId).isEqualTo(1L);
        assertThat(updatedPost.getContent()).isEqualTo("Updated content");
    }

    @Test
    @DisplayName("존재하지 않는 게시글 수정 시 예외 발생")
    void throwsExceptionWhenPostNotFoundOnUpdate() {
        // Given
        PostUpdateRequest request = new PostUpdateRequest("Updated content", List.of("업데이트"));

        // When & Then
        assertThatThrownBy(() -> postService.updatePost(request, httpServletRequest, 999L))
                .isInstanceOf(FriendyException.class)
                .hasMessageContaining("존재하지 않는 게시글입니다.")
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 작성자가 아닌 사용자가 수정 시 예외 발생")
    void throwsExceptionWhenNotPostAuthorOnUpdate() {
        // Given
        createPost();

        // When
        signUpOtherUser();

        // Then
        assertThatThrownBy(() -> postService.updatePost(
                new PostUpdateRequest("Updated content", List.of("업데이트")), httpServletRequest, 1L))
                .isInstanceOf(FriendyException.class)
                .hasMessageContaining("게시글은 작성자 본인만 관리할 수 있습니다.")
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_ACCESS);
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePostSuccessfullyDeletesPost() {
        // Given
        createPost();

        // When
        postService.deletePost(httpServletRequest, 1L);

        // Then
        assertThat(postRepository.existsById(1L)).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 게시글 삭제 시 예외 발생")
    void throwsExceptionWhenPostNotFoundOnDelete() {
        // When & Then
        assertThatThrownBy(() -> postService.deletePost(httpServletRequest, 999L))
                .isInstanceOf(FriendyException.class)
                .hasMessageContaining("존재하지 않는 게시글입니다.")
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 작성자가 아닌 사용자가 삭제 시 예외 발생")
    void throwsExceptionWhenNotPostAuthorOnDelete() {
        // Given
        createPost();

        // When
        signUpOtherUser();

        // Then
        assertThatThrownBy(() -> postService.deletePost(httpServletRequest, 1L))
                .isInstanceOf(FriendyException.class)
                .hasMessageContaining("게시글은 작성자 본인만 관리할 수 있습니다.")
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_ACCESS);
    }

    @Test
    @DisplayName("게시글 조회 요청이 성공적으로 수행되면 FindPostResponse를 리턴한다")
    void getPostSuccessfullyReturnsFindPostResponse() {
        // Given
        createPost();

        // When
        FindPostResponse response = postService.getPost(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("This is a sample post content.");
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 예외 발생")
    void getPostWithNonExistentIdThrowsException() {
        // When & Then
        assertThatThrownBy(() -> postService.getPost(999L))
                .isInstanceOf(FriendyException.class)
                .hasMessageContaining("존재하지 않는 게시글입니다.");
    }

    @Test
    @DisplayName("게시글 목록 조회 성공")
    void getAllPostsSuccessfullyReturnsFindAllPostResponse() {
        // Given
        createPost();
        createPost();

        // When
        FindAllPostResponse response = postService.getAllPosts(PageRequest.of(0, 10));

        // Then
        assertThat(response).isNotNull();
        assertThat(response.posts()).extracting("content")
                .containsExactlyInAnyOrder("This is a sample post content.", "This is a sample post content.");
    }

    @Test
    @DisplayName("존재하지 않는 페이지 요청 시 예외 발생")
    void requestingNonExistentPageThrowsException() {
        // When & Then
        assertThatThrownBy(() -> postService.getAllPosts(PageRequest.of(10, 10)))
                .isInstanceOf(FriendyException.class)
                .hasMessageContaining("요청한 페이지가 존재하지 않습니다.")
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }
}
