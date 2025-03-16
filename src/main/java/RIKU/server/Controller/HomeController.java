package RIKU.server.Controller;

import RIKU.server.Dto.Post.Response.ReadHomeResponse;
import RIKU.server.Service.HomeService;
import RIKU.server.Util.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run")
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "홈 조회", description = """
            
            홈을 조회합니다.(정규런, 번개런, 훈련, 행사)
            
            """)
    @GetMapping("")
    public BaseResponse<ReadHomeResponse> getHome() {
        ReadHomeResponse response = homeService.getHome();
        return new BaseResponse<>(response);
    }
}
