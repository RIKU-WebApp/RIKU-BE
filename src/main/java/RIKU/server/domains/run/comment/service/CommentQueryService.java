package RIKU.server.domains.run.comment.service;

import RIKU.server.domains.run.comment.dto.response.ReadCommentsResponse;
import RIKU.server.domains.run.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {
    private final CommentRepository commentRepository;

    public List<ReadCommentsResponse> getComments(Long postId) {
        var comments = commentRepository.findWithUserByPostId(postId);
        Map<Long, ReadCommentsResponse> byId = new HashMap<>();
        for (var comment : comments) {
            byId.put(comment.getId(), ReadCommentsResponse.of(comment, new ArrayList<>()));
        }
        List<ReadCommentsResponse> roots = new ArrayList<>();
        for (var comment : comments) {
            var response = byId.get(comment.getId());
            if (comment.getTargetId() == null) {
                roots.add(response);
            } else {
                var parent = byId.get(comment.getTargetId());
                // Keep the query within this post; malformed cross-post/orphan replies stay hidden.
                if (parent != null) parent.getReplies().add(response);
            }
        }
        return roots;
    }
}
