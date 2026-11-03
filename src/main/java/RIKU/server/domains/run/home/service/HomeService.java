package RIKU.server.domains.run.home.service;

import RIKU.server.domains.run.home.dto.response.ReadHomeCardResponse;
import RIKU.server.domains.run.home.dto.response.ReadHomeResponse;
import RIKU.server.global.entity.BaseStatus;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.PostType;
import RIKU.server.domains.run.post.entity.PostStatus;
import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.run.post.repository.PostRepository;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.security.AuthMember;
import RIKU.server.global.response.BaseResponseStatus;
import RIKU.server.domains.user.exception.UserException;
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
        ReadHomeCardResponse flashRun = getClosestPostByType(PostType.FLASH);
        ReadHomeCardResponse regularRun = getClosestPostByType(PostType.REGULAR);
        ReadHomeCardResponse trainingRun = getClosestPostByType(PostType.TRAINING);
        ReadHomeCardResponse eventRun = getClosestPostByType(PostType.EVENT);

        return ReadHomeResponse.of(user, flashRun, regularRun, trainingRun, eventRun);
    }

    private ReadHomeCardResponse getClosestPostByType(PostType postType) {
        return postRepository.findTopByStatusAndPostTypeAndPostStatusOrderByDateAsc(
                BaseStatus.ACTIVE, postType, PostStatus.NOW)
                .map(ReadHomeCardResponse::of)
                .orElse(null);
    }
}
