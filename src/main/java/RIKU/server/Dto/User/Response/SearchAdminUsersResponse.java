package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.User.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchAdminUsersResponse {

    private Long userId;

    private String name;

    private String phoneNumber;

    private String studentId;

    private String college;

    private String major;

    private String role;

    private Boolean isPacer;

    public static SearchAdminUsersResponse of(User user) {
        return SearchAdminUsersResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .phoneNumber(user.getPhone())
                .studentId(user.getStudentId())
                .college(user.getCollege())
                .major(user.getMajor())
                .role(getUserRole(user))
                .isPacer(user.getIsPacer())
                .build();
    }

    private static String getUserRole(User user) {
        if (user.getStatus() == BaseStatus.INACTIVE) {
            return "INACTIVE";
        }
        return user.getUserRole().name();
    }
}
