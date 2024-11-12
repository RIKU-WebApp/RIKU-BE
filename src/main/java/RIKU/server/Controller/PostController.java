package RIKU.server.Controller;

import RIKU.server.Dto.Post.Response.ReadPostDetailResponseDto;
import RIKU.server.Service.PostService;
import RIKU.server.Util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run/post")
public class PostController {

    private final PostService postService;

    // 게시글 상세 조회
    @GetMapping("/{postId}")
    public BaseResponse<ReadPostDetailResponseDto> getPost(@PathVariable Long postId) {
        ReadPostDetailResponseDto responseDto = postService.getPostDetail(postId);
        return new BaseResponse<>(responseDto);

    }

}
