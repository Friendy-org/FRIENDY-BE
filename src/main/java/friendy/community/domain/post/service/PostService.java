package friendy.community.domain.post.service;

import friendy.community.domain.member.model.Member;
import friendy.community.domain.member.repository.MemberRepository;
import friendy.community.domain.post.dto.request.PostCreateRequest;
import friendy.community.domain.post.model.Post;
import friendy.community.domain.post.repository.PostRepository;
import friendy.community.global.exception.ErrorCode;
import friendy.community.global.exception.FriendyException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    public long savePost(PostCreateRequest postCreateRequest, String email) {

        Member member = findMemberByEmail(email);

        final Post post = Post.of(postCreateRequest,member.getId());
        postRepository.save(post);
        return post.getId();
    }

    public Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new FriendyException(
                ErrorCode.UNAUTHORIZED_EMAIL,
                "해당 이메일의 회원이 존재하지 않습니다."
            ));
    }

}
