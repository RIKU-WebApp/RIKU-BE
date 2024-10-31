package RIKU.server.Entity.Board;

import RIKU.server.Entity.User.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("Flash")
@NoArgsConstructor
public class FlashPost extends Post {
    @Builder
    public FlashPost(User createdBy, String location, LocalDateTime date, String title, String content, String postImageUrl) {
        super(createdBy, title, location, date, content, postImageUrl);
    }
}
