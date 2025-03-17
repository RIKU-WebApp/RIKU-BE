package RIKU.server.Controller.Post;

import RIKU.server.Dto.Post.Response.ReadPostListResponse;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.Post.PostService;
import RIKU.server.Util.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run/{runType}")
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

//    // 게시글 수정하기
//    @PutMapping("/post/{postId}")
//    public BaseResponse<Map<String, Long>> updatePost(
//            @PathVariable Long postId,
//            @ModelAttribute @Validated CreatePostRequestDto requestDto,
//            BindingResult bindingResult,
//            @AuthenticationPrincipal AuthMember authMember) {
//
//        // 유효성 검증 실패 시 처리
//        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);
//
//        Long updatedPostId = postService.updatePost(authMember.getId(), postId, requestDto);
//
//        Map<String, Long> response = new HashMap<>();
//        response.put("postId", updatedPostId);
//
//        return new BaseResponse<>(response);
//    }

    @Operation(summary = "게시글 러닝 취소", description = """
            
            유저가 게시글을 취소합니다.(번개런은 생성자 권한, 나머지는 운영진 권한)
            
            """)
    @PatchMapping("/post/{postId}/cancel")
    public BaseResponse<Map<String, Long>> cancelPost(
            @PathVariable String runType,
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {

        Long canceledPostId = postService.cancelPost(authMember, runType, postId);

        Map<String, Long> response = new HashMap<>();
        response.put("canceledPostId", canceledPostId);

        return new BaseResponse<>(response);
    }

}
