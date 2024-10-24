package RIKU.server.Controller.Post;

import RIKU.server.Dto.Post.Response.ReadPostsResponseDto;
import RIKU.server.Service.Post.FlashPostService;
import RIKU.server.Util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run/flash")
public class FlashPostController {

    private final FlashPostService flashPostService;

    @GetMapping("")
    public BaseResponse<List<ReadPostsResponseDto>> getFlashPosts() {
        List<ReadPostsResponseDto> posts = flashPostService.getAllFlashPosts();
        return new BaseResponse<>(posts);
    }


}
