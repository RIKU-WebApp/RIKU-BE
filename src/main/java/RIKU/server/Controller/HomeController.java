package RIKU.server.Controller;

import RIKU.server.Dto.Post.Response.ReadHomeResponseDto;
import RIKU.server.Service.HomeService;
import RIKU.server.Util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/home")
public class HomeController {
    private final HomeService homeService;
    @GetMapping("")
    public BaseResponse<List<ReadHomeResponseDto>> getHome() {
        List<ReadHomeResponseDto> homePosts = homeService.getHome();
        return new BaseResponse<>(homePosts);
    }
}
