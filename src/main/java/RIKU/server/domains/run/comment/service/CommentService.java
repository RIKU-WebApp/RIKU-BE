package RIKU.server.domains.run.comment.service;

import RIKU.server.domains.run.comment.dto.request.CreateCommentRequest;
import RIKU.server.global.entity.BaseStatus;
import RIKU.server.domains.run.comment.entity.Comment;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.PostType;
import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.run.comment.repository.CommentRepository;
import RIKU.server.domains.run.post.repository.PostRepository;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.security.AuthMember;
import RIKU.server.global.response.BaseResponseStatus;
import RIKU.server.domains.run.post.exception.PostException;
import RIKU.server.domains.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    // 댓글 생성
    @Transactional
    public Long createComment(AuthMember authMember, String runType, Long postId, CreateCommentRequest request) {
        // 1. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 2. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 3. runType과 게시글의 postType 비교
        validatePostType(runType, post.getPostType());

        // 4. 대댓글 대상 조회 (Optional)
        Long targetCommentId = request.getTargetId() != null
                ? commentRepository.findById(request.getTargetId())
                .orElseThrow(() -> new PostException(BaseResponseStatus.COMMENT_NOT_FOUND))
                .getId()
                : null;

        // 5. 댓글 엔티티 생성 및 저장
        Comment comment = request.toEntity(user, post, targetCommentId);
        return commentRepository.save(comment).getId();
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(AuthMember authMember, String runType, Long postId, Long commentId) {
        // 1. 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.COMMENT_NOT_FOUND));

        // 2. 해당 댓글이 속한 게시글 조회
        Post post = comment.getPost();

        // 3. runType과 게시글의 postType 비교
        validatePostType(runType, post.getPostType());

        // 4. 해당 댓글이 해당 게시글에 속하는 지 검증
        if (!post.getId().equals(postId)) {
            throw new PostException(BaseResponseStatus.INVALID_COMMENT_FOR_POST);
        }

        // 5. 해당 댓글을 쓴 당사자가 맞는 지 검증
        if (!comment.getUser().getId().equals(authMember.getId())) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        // 6. 댓글 삭제 처리
        comment.updateStatus(BaseStatus.INACTIVE);
    }

    private void validatePostType(String runType, PostType postType) {
        try {
            PostType requestedType = PostType.valueOf(runType.toUpperCase());
            if (!postType.equals(requestedType)) {
                throw new PostException(BaseResponseStatus.INVALID_POST_TYPE);
            }
        } catch (IllegalArgumentException e) {
            throw new PostException(BaseResponseStatus.INVALID_RUN_TYPE);
        }

    }
}
