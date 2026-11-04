package RIKU.server.domains.run.recruitment;
import RIKU.server.security.AuthMember;
import RIKU.server.global.response.BaseResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequiredArgsConstructor @RequestMapping("/run/regular")
public class RecruitmentController {
    private final RecruitmentService service;
    @PostMapping(value="/recruitment", consumes="application/json")
    BaseResponse<Map<String,Long>> create(@AuthenticationPrincipal AuthMember user,@Valid @RequestBody RecruitmentRequest request) {
        return new BaseResponse<>(Map.of("postId",service.create(user,request)));
    }
    @PostMapping(value="/recruitment", consumes="multipart/form-data")
    BaseResponse<Map<String,Long>> createWithImages(@AuthenticationPrincipal AuthMember user,
            @Valid @RequestPart("request") RecruitmentRequest request,
            @RequestPart(value="postImage",required=false) org.springframework.web.multipart.MultipartFile image,
            @RequestPart(value="attachments",required=false) java.util.List<org.springframework.web.multipart.MultipartFile> attachments) throws java.io.IOException {
        return new BaseResponse<>(Map.of("postId",service.createWithImages(user,request,image,attachments)));
    }
    @GetMapping("/post/{id}/recruitment")
    BaseResponse<RecruitmentService.Summary> read(@PathVariable Long id,@AuthenticationPrincipal AuthMember user) {
        return new BaseResponse<>(service.read(id,user));
    }
    public record JoinRequest(@NotNull RunEnrollment.Role role,@NotBlank String group) {}
    @PutMapping("/post/{id}/enrollment")
    BaseResponse<RecruitmentService.Summary> join(@PathVariable Long id,@AuthenticationPrincipal AuthMember user,@Valid @RequestBody JoinRequest request) {
        return new BaseResponse<>(service.join(id,user,request.role(),request.group()));
    }
    @DeleteMapping("/post/{id}/enrollment")
    BaseResponse<RecruitmentService.Summary> cancel(@PathVariable Long id,@AuthenticationPrincipal AuthMember user) {
        return new BaseResponse<>(service.cancel(id,user));
    }
}
