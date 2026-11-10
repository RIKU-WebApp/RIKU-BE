package RIKU.server.domains.run.post.repository;

import RIKU.server.domains.run.post.entity.PostStatus;
import java.time.LocalDateTime;

public record PostPreviewRow(Long id, String title, LocalDateTime date, PostStatus postStatus, String postImageUrl, Boolean showFullImage) {}
