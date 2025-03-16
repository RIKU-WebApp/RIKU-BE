package RIKU.server.Service;

import RIKU.server.Dto.Post.Response.ReadHomeCardResponse;
import RIKU.server.Dto.Post.Response.ReadHomeResponse;
import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.Post.PostType;
import RIKU.server.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final PostRepository postRepository;

    public ReadHomeResponse getHome() {
        //1. 현재 날짜 기준
        LocalDateTime now = LocalDateTime.now();

        // 러닝 유형별 가장 가까운 날짜의 게시글 1개씩 조회
        ReadHomeCardResponse regularRun = getClosestPostByType(PostType.REGULAR, now);
        ReadHomeCardResponse flashRun = getClosestPostByType(PostType.FLASH, now);
        ReadHomeCardResponse trainingRun = getClosestPostByType(PostType.TRAINING, now);
        ReadHomeCardResponse eventRun = getClosestPostByType(PostType.EVENT, now);

        return ReadHomeResponse.of(regularRun, flashRun, trainingRun, eventRun);
    }

    private ReadHomeCardResponse getClosestPostByType(PostType postType, LocalDateTime now) {
        Optional<Post> closestPost = postRepository.findTopByStatusAndPostTypeAndDateAfterOrderByDateAsc(BaseStatus.ACTIVE, postType, now);
        return closestPost.map(ReadHomeCardResponse::of).orElse(null);
    }
}
