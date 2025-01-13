package RIKU.server.Service;

import RIKU.server.Dto.Post.Response.ReadHomeResponseDto;
import RIKU.server.Entity.Board.FlashPost;
import RIKU.server.Entity.Board.Post;
import RIKU.server.Entity.Board.RegularPost;
import RIKU.server.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {
    private final PostRepository postRepository;

    public List<ReadHomeResponseDto> getHome() {

        LocalDateTime today = LocalDateTime.now();

        // 모든 게시글 오름차순 정렬 조회
        List<Post> posts = postRepository.findByDateAfterOrderByDateAsc(today);

        // 타입별 예정일이 가장 가까운 게시글 선택
        FlashPost flashPost = posts.stream()
                .filter(post -> post instanceof FlashPost)
                .map(post -> (FlashPost) post)
                .findFirst().orElse(null);

        RegularPost regularPost = posts.stream()
                .filter(post -> post instanceof RegularPost)
                .map(post -> (RegularPost) post)
                .findFirst().orElse(null);

        return Stream.of(
                    flashPost != null ? ReadHomeResponseDto.of("번개런", flashPost) : null,
                    regularPost != null ? ReadHomeResponseDto.of("정규런", regularPost): null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
