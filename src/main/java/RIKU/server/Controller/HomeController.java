package RIKU.server.Controller;

import RIKU.server.Dto.Post.Response.ReadHomeResponse;
import RIKU.server.Service.HomeService;
import RIKU.server.Util.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run")
@Tag(name = "Home", description = "홈 관련 API")
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "홈 조회", description = """
            
            홈을 조회합니다.(번개런, 정규런, 훈련, 행사)
            
            """)
    @GetMapping("")
    public BaseResponse<ReadHomeResponse> getHome() {
        ReadHomeResponse response = homeService.getHome();
        return new BaseResponse<>(response);
    }
}
