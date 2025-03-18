package RIKU.server.Controller;

import RIKU.server.Dto.Post.Request.CreateCommentRequest;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.CommentService;
import RIKU.server.Util.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run/{runType}/post/{postId}/comment")
@Tag(name = "Comment", description = "(대)댓글 관련 API")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "(대)댓글 생성", description = """
            
            유저가 게시글에 댓글을 답니다.
            
            """)
    @PostMapping("")
    public BaseResponse<Map<String, Long>> createComment(
            @PathVariable String runType,
            @PathVariable Long postId,
            @Validated @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal AuthMember authMember) {

        Long commentId = commentService.createComment(authMember, runType, postId, request);

        Map<String, Long> response = new HashMap<>();
        response.put("commentId", commentId);

        return new BaseResponse<>(response);
    }

    @Operation(summary = "(대)댓글 삭제", description = """
            
            유저가 게시글에 댓글을 삭제합니다.
            
            """)
    @PatchMapping("/{commentId}")
    public BaseResponse<Map<String, Object>> deleteComment(
            @PathVariable String runType,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthMember authMember) {

        commentService.deleteComment(authMember, runType, postId, commentId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "댓글이 삭제되었습니다.");
        return new BaseResponse<>(response);
    }
}
