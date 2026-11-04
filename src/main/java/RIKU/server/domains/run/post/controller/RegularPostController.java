package RIKU.server.domains.run.post.controller;

import RIKU.server.domains.run.post.dto.response.ReadRegularPostDetailResponse;
import RIKU.server.security.AuthMember;
import RIKU.server.domains.run.post.service.RegularPostService;
import RIKU.server.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/run")
@Tag(name = "RegularPost", description = "정규런 게시글 관련 API")
public class RegularPostController {

    private final RegularPostService regularPostService;

    @Deprecated
    @PostMapping(value = "/regular/post", consumes = "multipart/form-data")
    public void createWithoutCapacity() {
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                "새로고침 후 총정원을 입력해주세요.");
    }

    @Operation(summary = "정규런 게시글 상세조회", description = """
            
            유저가 정규런 게시글을 조회합니다.(권한 제한 없음)
            
            """)
    @GetMapping("/regular/post/{postId}")
    public BaseResponse<ReadRegularPostDetailResponse> getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        ReadRegularPostDetailResponse response = regularPostService.getPostDetail(postId, authMember);
        return new BaseResponse<>(response);
    }
}
