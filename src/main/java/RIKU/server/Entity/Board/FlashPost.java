package RIKU.server.Entity.Board;

import RIKU.server.Entity.User.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("Flash")
@NoArgsConstructor
public class FlashPost extends Post {
    public FlashPost(User createdBy, String location, LocalDateTime date, String title, String content) {
        super(createdBy, title, location, date, content);
    }
}
