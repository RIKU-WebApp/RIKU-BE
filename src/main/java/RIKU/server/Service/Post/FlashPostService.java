package RIKU.server.Service.Post;

import RIKU.server.Dto.Post.Request.CreatePostRequestDto;
import RIKU.server.Dto.Post.Response.ReadPostsResponseDto;
import RIKU.server.Entity.Board.FlashPost;
import RIKU.server.Repository.PostRepository;
import RIKU.server.Service.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlashPostService {

    private final PostRepository postRepository;
    private final S3Uploader s3Uplader;

    public List<ReadPostsResponseDto> getAllFlashPosts() {
        List<FlashPost> posts = postRepository.findAllFlashPosts();

        return posts.stream()
                .map(ReadPostsResponseDto::of)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long save(CreatePostRequestDto requestDto) {
        String postImageUrl = null;
        log.info("Received CreatePostRequestDto with postImage: {}", requestDto.getPostImage());
        if (requestDto.getPostImage() != null && !requestDto.getPostImage().isEmpty()) {
            try {
                log.info("Received file: {}", requestDto.getPostImage().getOriginalFilename());
                postImageUrl = s3Uplader.upload(requestDto.getPostImage(), "postImg"); // S3에 이미지 업로드
                log.info("Post Image URL after upload: {}", postImageUrl);
            } catch (IOException e) {
                log.error("File upload failed: {}", requestDto.getPostImage().getOriginalFilename(), e);
                throw new RuntimeException("Failed to upload file", e);
            }
        } else {
            log.warn("postImage is null or empty");
        }

        FlashPost post = requestDto.flashToEntity(postImageUrl);
        log.info("Creating FlashPost entity with image URL: {}", post.getPostImageUrl());

        FlashPost savedPost = postRepository.save(post);
        log.info("FlashPost saved with ID: {} and Image URL: {}", savedPost.getId(), savedPost.getPostImageUrl());

        return savedPost.getId();
    }

}
