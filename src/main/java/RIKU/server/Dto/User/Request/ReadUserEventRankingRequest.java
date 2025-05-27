package RIKU.server.Dto.User.Request;

import RIKU.server.Entity.User.PointType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class ReadUserEventRankingRequest {

    @NotNull(message = "시작 날짜는 필수입니다.")
    @Schema(description = "조회 시작 날짜 (KST 기준)", example = "2025-05-01")
    private LocalDate startDate;

    @NotNull(message = "종료 날짜는 필수입니다.")
    @Schema(description = "조회 종료 날짜 (KST 기준)", example = "2025-05-31")
    private LocalDate endDate;

    @NotEmpty(message = "포인트 타입 목록은 비어 있을 수 없습니다.")
    @Schema(description = "조회할 포인트 타입 리스트", example = "[\"ADD_FLASH_CREATE\", \"ADD_FLASH_JOIN\"]")
    private List<PointType> pointTypes;
}
