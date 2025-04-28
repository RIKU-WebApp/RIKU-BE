package RIKU.server.Service;

import RIKU.server.Dto.Post.Response.ReadHomeCardResponse;
import RIKU.server.Dto.Post.Response.ReadHomeResponse;
import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.Post.PostType;
import RIKU.server.Entity.Board.PostStatus;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.PostRepository;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Security.AuthMember;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public ReadHomeResponse getHome(AuthMember authMember) {
        // 1. User 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 2. 현재 날짜 기준
        LocalDateTime now = LocalDateTime.now();

        // 3. 러닝 유형별 가장 가까운 날짜의 게시글 1개씩 조회
        ReadHomeCardResponse flashRun = getClosestPostByType(PostType.FLASH, now);
        ReadHomeCardResponse regularRun = getClosestPostByType(PostType.REGULAR, now);
        ReadHomeCardResponse trainingRun = getClosestPostByType(PostType.TRAINING, now);
        ReadHomeCardResponse eventRun = getClosestPostByType(PostType.EVENT, now);

        return ReadHomeResponse.of(user, flashRun, regularRun, trainingRun, eventRun);
    }

    private ReadHomeCardResponse getClosestPostByType(PostType postType, LocalDateTime now) {
        Optional<Post> closestPost = postRepository.findTopByStatusAndPostTypeAndPostStatusAndDateAfterOrderByDateAsc(BaseStatus.ACTIVE, postType, PostStatus.NOW, now);
        return closestPost.map(ReadHomeCardResponse::of).orElse(null);
    }
}
