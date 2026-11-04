package RIKU.server.domains.community;
import RIKU.server.security.AuthMember;
import RIKU.server.global.response.BaseResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController @RequiredArgsConstructor
public class CommunityController {
    private final CommunityService service;
    public record RecordRequest(@NotBlank @Size(max=80) String title,@NotNull @PastOrPresent LocalDate date,
             @NotNull @DecimalMin("0.1") @DecimalMax("200") BigDecimal distanceKm,@Min(1) @Max(2880) int minutes) {}
    public record AwardRequest(@NotBlank @Size(max=40) String name,@NotBlank @Size(max=200) String description) {}
    @GetMapping("/user/{id}/public-profile")
    public BaseResponse<?> profile(@PathVariable Long id) { return new BaseResponse<>(service.profile(id)); }
    @GetMapping("/user/activity/badges")
    public BaseResponse<?> activity(@AuthenticationPrincipal AuthMember actor) { return new BaseResponse<>(service.activity(actor.getId())); }
    @PostMapping("/user/records")
    public BaseResponse<?> record(@AuthenticationPrincipal AuthMember actor,@Valid @RequestBody RecordRequest request) { return new BaseResponse<>(service.record(actor,request)); }
    @DeleteMapping("/user/records/{id}")
    public BaseResponse<?> deleteRecord(@AuthenticationPrincipal AuthMember actor,@PathVariable Long id) { service.deleteRecord(actor,id);return new BaseResponse<>(Map.of("deleted",true)); }
    @PostMapping("/admin/users/{id}/badges")
    public BaseResponse<?> award(@AuthenticationPrincipal AuthMember actor,@PathVariable Long id,@Valid @RequestBody AwardRequest request) { service.award(actor,id,request);return new BaseResponse<>(Map.of("awarded",true)); }
}
