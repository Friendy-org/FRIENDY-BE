package friendy.community.domain.post.model;

import friendy.community.domain.post.dto.request.PostCreateRequest;
import friendy.community.domain.post.repository.PostRepository;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDate createdAt;

    public Post(final Long memberId , final String title, final String content, final LocalDate createdAt) {
        this(null, memberId, title, content, createdAt);
    }

    public static Post of(final PostCreateRequest request,final Long memberId) {
        return new Post(memberId, request.title(), request.content(), request.createdAt());
    }



}
