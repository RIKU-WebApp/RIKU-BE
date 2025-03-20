package RIKU.server.Controller.Post;

import RIKU.server.Dto.Post.Request.UpdatePostRequest;
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

//    // 게시글 상세 조회
//    @GetMapping("/post/{postId}")
//    public BaseResponse<ReadPostDetailResponseDto> getPost(@PathVariable Long postId) {
//        ReadPostDetailResponseDto responseDto = postService.getPostDetail(postId);
//        return new BaseResponse<>(responseDto);
//
//    }

    @Operation(summary = "게시글 수정", description = """
            
            유저가 게시글을 수정합니다.(번개런은 생성자 권한, 나머지는 운영진 권한)
            
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

//        postService.updatePost(authMember, runType, postId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "게시글이 수정되었습니다.");

        return new BaseResponse<>(response);
    }

    @Operation(summary = "게시글 러닝 취소", description = """
            
            유저가 게시글을 취소합니다.(번개런은 생성자 권한, 나머지는 운영진 권한)
            
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

}
