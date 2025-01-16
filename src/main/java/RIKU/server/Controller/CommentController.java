package RIKU.server.Controller;

import RIKU.server.Dto.Post.Request.CreateCommentRequestDto;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.CommentService;
import RIKU.server.Util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run/post/{postId}/comment")
public class CommentController {
    private final CommentService commentService;

    // 댓글 생성
    @PostMapping
    public BaseResponse<Map<String, Long>> createComment(
            @PathVariable Long postId,
            @Validated @RequestBody CreateCommentRequestDto requestDto,
            @AuthenticationPrincipal AuthMember authMember) {

        Long commentId = commentService.createComment(authMember.getId(), postId, requestDto);

        Map<String, Long> response = new HashMap<>();
        response.put("commentId", commentId);

        return new BaseResponse<>(response);
    }


}
