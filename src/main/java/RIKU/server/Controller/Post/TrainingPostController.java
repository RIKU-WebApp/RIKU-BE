package RIKU.server.Controller.Post;

import RIKU.server.Dto.Post.Request.CreateTrainingPostRequest;
import RIKU.server.Dto.Post.Response.ReadRegularPostDetailResponse;
import RIKU.server.Dto.Post.Response.ReadTrainingPostDetailResponse;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.Post.TrainingPostService;
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
@RequestMapping("/run")
@Tag(name = "TrainingPost", description = "훈련 게시글 관련 API")
public class TrainingPostController {

    private final TrainingPostService trainingPostService;

    @Operation(summary = "훈련 게시글 생성", description = """
            
            유저가 훈련 게시글을 생성합니다.(운영진 권한)
            
            """)
    @PostMapping(value = "/training/post", consumes = "multipart/form-data")
    public BaseResponse<Map<String, Long>> createPost(
            @ModelAttribute @Validated CreateTrainingPostRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal AuthMember authMember) {

        // 유효성 검증 실패 시 처리
        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        Long postId = trainingPostService.createPost(authMember, request);

        Map<String, Long> response = new HashMap<>();
        response.put("postId", postId);

        return new BaseResponse<>(response);
    }

    @Operation(summary = "훈련 게시글 상세조회", description = """
            
            유저가 훈련 게시글을 조회합니다.(권한 제한 없음)
            
            """)
    @GetMapping("/training/post/{postId}")
    public BaseResponse<ReadTrainingPostDetailResponse> getPostDetail(@PathVariable Long postId) {
        ReadTrainingPostDetailResponse response = trainingPostService.getPostDetail(postId);
        return new BaseResponse<>(response);
    }
}
