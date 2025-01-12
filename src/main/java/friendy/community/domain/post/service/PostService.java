package friendy.community.domain.post.service;

import friendy.community.domain.member.repository.MemberRepository;
import friendy.community.domain.post.dto.request.PostCreateRequest;
import friendy.community.domain.post.model.Post;
import friendy.community.domain.post.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    public Post createPost(PostCreateRequest postCreateRequest, Long memberId) {
        final Post post = Post.of(postCreateRequest,memberId);
        postRepository.save(post);
        return post;
    }
}
