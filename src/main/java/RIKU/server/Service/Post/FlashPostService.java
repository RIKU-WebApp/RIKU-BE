package RIKU.server.Service.Post;

import RIKU.server.Dto.Post.Response.ReadPostsResponseDto;
import RIKU.server.Entity.Board.FlashPost;
import RIKU.server.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlashPostService {

    private final PostRepository postRepository;

    public List<ReadPostsResponseDto> getAllFlashPosts() {
        List<FlashPost> posts = postRepository.findAllFlashPosts();

        return posts.stream()
                .map(ReadPostsResponseDto::of)
                .collect(Collectors.toList());
    }



}
