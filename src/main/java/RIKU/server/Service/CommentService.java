package RIKU.server.Service;

import RIKU.server.Dto.Post.Request.CreateCommentRequestDto;
import RIKU.server.Entity.Board.Comment;
import RIKU.server.Entity.Board.Post;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.CommentRepository;
import RIKU.server.Repository.PostRepository;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.PostException;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    // 댓글 생성
    @Transactional
    public Long createComment(Long userId, Long postId, CreateCommentRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        Comment targetComment = null;

        if (requestDto.getTargetId() != null) {
            targetComment = commentRepository.findById(requestDto.getTargetId())
                    .orElseThrow(() -> new PostException(BaseResponseStatus.COMMENT_NOT_FOUND));
        }

        Comment comment = requestDto.toEntity(user, post, targetComment);
        return commentRepository.save(comment).getId();
    }

    // 댓글 삭제
    @Transactional
    public Long deleteComment(Long userId, Long commentId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        comment.updateInactive();
        return comment.getId();
    }
}
