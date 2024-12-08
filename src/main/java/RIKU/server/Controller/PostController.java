package RIKU.server.Controller;

import RIKU.server.Dto.Post.Request.CreatePostRequestDto;
import RIKU.server.Dto.Post.Response.ReadPostDetailResponseDto;
import RIKU.server.Dto.Post.Response.ReadPostsResponseDto;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.PostService;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.Exception.Validation.FieldValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run")
public class PostController {

    private final PostService postService;

    // 게시글 생성
    @PostMapping("/{runType}/post")
    public BaseResponse<Map<String, Long>> createFlashPost(
            @ModelAttribute @Validated
            CreatePostRequestDto requestDto,
            BindingResult bindingResult,
            @PathVariable String runType,
            @AuthenticationPrincipal AuthMember authMember) {

        // 유효성 검증 실패 시 처리
        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        Long postId = postService.save(authMember.getId(), runType, requestDto);

        Map<String, Long> response = new HashMap<>();
        response.put("postId", postId);

        return new BaseResponse<>(response);
    }

    // 게시판별 전체 게시글 조회
    @GetMapping("/{runType}")
    public BaseResponse<List<ReadPostsResponseDto>> getPosts(@PathVariable String runType) {
        List<ReadPostsResponseDto> posts = postService.getPostsByRunType(runType);
        return new BaseResponse<>(posts);
    }

    // 게시글 상세 조회
    @GetMapping("/post/{postId}")
    public BaseResponse<ReadPostDetailResponseDto> getPost(@PathVariable Long postId) {
        ReadPostDetailResponseDto responseDto = postService.getPostDetail(postId);
        return new BaseResponse<>(responseDto);

    }

}
