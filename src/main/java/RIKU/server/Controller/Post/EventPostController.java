package RIKU.server.Controller.Post;

import RIKU.server.Dto.Post.Request.CreateEventPostRequest;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.Post.EventPostService;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.Exception.Validation.FieldValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run")
@Tag(name = "EventPost", description = "행사 게시글 관련 API")
public class EventPostController {

    private final EventPostService eventPostService;

    @Operation(summary = "행사 게시글 생성", description = """
            
            유저가 행사 게시글을 생성합니다.(운영진 권한)
            
            """)
    @PostMapping(value = "/event/post", consumes = "multipart/form-data")
    public BaseResponse<Map<String, Long>> createPost(
            @ModelAttribute @Validated CreateEventPostRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal AuthMember authMember) {

        // 유효성 검증 실패 시 처리
        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        Long postId = eventPostService.createPost(authMember, request);

        Map<String, Long> response = new HashMap<>();
        response.put("postId", postId);

        return new BaseResponse<>(response);
    }
}
