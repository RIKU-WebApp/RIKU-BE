package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.User;
import lombok.Getter;

@Getter
public class ReadUserProfileDetailResponse {

    private String profileImageUrl;

    private String name;

    private String college;

    private String major;

    private String phone;

    private String studentId;

    private ReadUserProfileDetailResponse(String profileImageUrl, String name, String college, String major, String phone, String studentId) {
        this.profileImageUrl = profileImageUrl;
        this.name = name;
        this.college = college;
        this.major = major;
        this.phone = phone;
        this.studentId = studentId;
    }

    public static ReadUserProfileDetailResponse of(User user) {
        return new ReadUserProfileDetailResponse(
                user.getProfileImageUrl(),
                user.getName(),
                user.getCollege(),
                user.getMajor(),
                user.getPhone(),
                user.getStudentId()
        );
    }
}
