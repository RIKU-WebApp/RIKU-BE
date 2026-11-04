package RIKU.server.domains.run.recruitment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record RecruitmentRequest(
        @NotBlank @Size(max=150) String title,
        @NotBlank @Size(max=200) String location,
        @NotNull LocalDateTime date,
        @NotBlank @Size(max=10000) String content,
        @Min(1) @Max(1000) Integer capacity,
        @NotEmpty @Size(max=20) List<@NotNull @Valid Group> groups) {
    public record Group(@NotBlank @Size(max=20) String name,
                        @NotBlank @Pattern(regexp="[0-9]{1,2}:[0-5][0-9]") String pace,
                        @NotNull @DecimalMin("0.1") @DecimalMax("200") BigDecimal distanceKm,
                        @Min(1) @Max(1000) Integer capacity) {
        public Group(String name, String pace, BigDecimal distanceKm) { this(name, pace, distanceKm, null); }
    }
}
