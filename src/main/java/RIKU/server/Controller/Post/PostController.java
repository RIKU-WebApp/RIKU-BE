package RIKU.server.Controller.Post;

import RIKU.server.Dto.Post.Request.UpdatePostRequest;
import RIKU.server.Dto.Post.Response.ReadPaceGroupResponse;
import RIKU.server.Dto.Post.Response.ReadPostListResponse;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.Post.PostService;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.Exception.Validation.FieldValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run/{runType}")
@Tag(name = "Post", description = "게시글 통합 관련 API")
public class PostController {

    private final PostService postService;


    @Operation(summary = "러닝 유형별 게시글 리스트 조회", description = """
            
            러닝 유형별 게시글 리스트를 조회합니다.(오늘의 러닝, 예정된 러닝, 지난 러닝)
            
            """)
    @GetMapping("")
    public BaseResponse<ReadPostListResponse> getPostList(@PathVariable String runType) {
        ReadPostListResponse posts = postService.getPostsByRunType(runType);
        return new BaseResponse<>(posts);
    }

    @Operation(summary = "게시글 수정", description = """
            
            유저가 게시글을 수정합니다.(생성자 권한)
            
            """)
    @PatchMapping(value = "/post/{postId}", consumes = "multipart/form-data")
    public BaseResponse<Map<String, Object>> updatePost(
            @PathVariable String runType,
            @PathVariable Long postId,
            @ModelAttribute @Validated UpdatePostRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal AuthMember authMember) {

        // 유효성 검증 실패 시 처리
        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        postService.updatePost(authMember, runType, postId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "게시글이 수정되었습니다.");

        return new BaseResponse<>(response);
    }

    @Operation(summary = "게시글 러닝 취소", description = """
            
            유저가 게시글을 취소합니다.(생성자 권한)
            
            """)
    @PatchMapping("/post/{postId}/cancel")
    public BaseResponse<Map<String, Object>> cancelPost(
            @PathVariable String runType,
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {

        postService.cancelPost(authMember, runType, postId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "게시글이 취소되었습니다.");

        return new BaseResponse<>(response);
    }

    @Operation(summary = "페이스 그룹 조회", description = """
        
            게시글에 등록된 페이서들의 그룹 목록을 조회합니다.
            
            """)
    @GetMapping("/post/{postId}/group")
    public BaseResponse<List<ReadPaceGroupResponse>> getPaceGroups (
            @PathVariable String runType,
            @PathVariable Long postId) {

        List<ReadPaceGroupResponse> responses = postService.getPaceGroups(runType, postId);

        return new BaseResponse<>(responses);
    }

    @Operation(summary = "게시글 삭제", description = """
            
            유저가 게시글을 삭제하며 관련된 페이서, 참여자, 첨부파일, 댓글 등도 함께 삭제됩니다. (관리자 권한)
            
            """)
    @DeleteMapping("/post/{postId}")
    public BaseResponse<Map<String, Object>> deletePost(
            @PathVariable String runType,
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {

        postService.deletePost(authMember, runType, postId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "게시글이 삭제되었습니다.");

        return new BaseResponse<>(response);
    }
}
