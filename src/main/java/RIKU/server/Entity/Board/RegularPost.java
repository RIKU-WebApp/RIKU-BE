package RIKU.server.Entity.Board;

import RIKU.server.Entity.User.User;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("regular_post")
@NoArgsConstructor
public class RegularPost extends Post {

    @Column(name = "course_image_url")
    private String courseImageUrl;

}
