package RIKU.server.Controller.Post;

import RIKU.server.Dto.Post.Request.CreatePostRequestDto;
import RIKU.server.Dto.Post.Response.ReadPostDetailResponseDto;
import RIKU.server.Dto.Post.Response.ReadPostsResponseDto;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.Post.FlashPostService;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.Exception.Validation.ValidationException;
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
@RequestMapping("/run/flash")
public class FlashPostController {

    private final FlashPostService flashPostService;

    // 번개런 게시글 조회
    @GetMapping("")
    public BaseResponse<List<ReadPostsResponseDto>> getFlashPosts() {
        List<ReadPostsResponseDto> posts = flashPostService.getAllFlashPosts();
        return new BaseResponse<>(posts);
    }

    // 번개런 게시글 생성
    @PostMapping("/post")
    public BaseResponse<Map<String, Long>> createFlashPost(
            @ModelAttribute @Validated
            CreatePostRequestDto requestDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal AuthMember authMember) {

        // 유효성 검증 실패 시 처리
        if (bindingResult.hasErrors()) throw new ValidationException(bindingResult);

        Long postId = flashPostService.save(authMember.getId(), requestDto);

        Map<String, Long> response = new HashMap<>();
        response.put("postId", postId);

        return new BaseResponse<>(response);

    }
}