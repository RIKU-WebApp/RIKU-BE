package RIKU.server.domains.auth.recovery;

import RIKU.server.domains.community.Feedback;
import RIKU.server.global.response.BaseResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PasswordInquiryController {
    private final PasswordInquiryService service;

    public record SubmitRequest(
            @NotBlank(message = "가입할 때 입력한 학번을 입력해 주세요.")
            @Size(max = 255, message = "학번은 255자 이내로 입력해 주세요.") String studentId,
            @NotBlank(message = "가입할 때 입력한 이름을 입력해 주세요.")
            @Size(max = 255, message = "이름은 255자 이내로 입력해 주세요.") String name,
            @NotBlank(message = "가입할 때 입력한 학과를 입력해 주세요.")
            @Size(max = 255, message = "학과는 255자 이내로 입력해 주세요.") String major,
            @Pattern(regexp = "^(?:01[016789]-?[0-9]{3,4}-?[0-9]{4})?$",
                    message = "휴대전화 번호를 010-1234-5678 형식으로 입력하거나 비워 두세요.") String phone) {}

    public record StatusRequest(@NotNull Feedback.Status status) {}

    @PostMapping("/user/password-inquiries")
    public BaseResponse<Boolean> submit(@Valid @RequestBody SubmitRequest request) {
        service.submit(request);
        return new BaseResponse<>(true);
    }

    @GetMapping("/admin/password-inquiries")
    public BaseResponse<Page<PasswordInquiry>> list(@RequestParam(defaultValue = "0") int page) {
        return new BaseResponse<>(service.list(page));
    }

    @PatchMapping("/admin/password-inquiries/{id}")
    public BaseResponse<PasswordInquiry> update(@PathVariable long id, @Valid @RequestBody StatusRequest request) {
        return new BaseResponse<>(service.update(id, request.status()));
    }
}
