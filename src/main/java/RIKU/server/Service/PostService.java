package RIKU.server.Service;

import RIKU.server.Dto.Post.Request.CreatePostRequestDto;
import RIKU.server.Dto.Post.Response.ReadPostDetailResponseDto;
import RIKU.server.Dto.Post.Response.ReadPostsResponseDto;
import RIKU.server.Entity.Board.FlashPost;
import RIKU.server.Entity.Board.Post;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.ParticipantRepository;
import RIKU.server.Repository.PostRepository;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.PostException;
import RIKU.server.Util.Exception.Domain.UserException;
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
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final S3Uploader s3Uploader;

    // 게시글 생성
    @Transactional
    public Long save(Long userId, String runType, CreatePostRequestDto requestDto) {
        String postImageUrl = null;

        if (requestDto.getPostImage() != null && !requestDto.getPostImage().isEmpty()) {
            try {
                postImageUrl = s3Uploader.upload(requestDto.getPostImage(), "postImg"); // S3에 이미지 업로드
            } catch (IOException e) {
                log.error("File upload failed: {}", requestDto.getPostImage().getOriginalFilename(), e);
                throw new PostException(BaseResponseStatus.POST_IMAGE_UPLOAD_FAILED);
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        Post post;
        switch (runType.toLowerCase()) {
            case "flash":
                post = requestDto.flashToEntity(user, postImageUrl);
                break;
            default:
                throw new PostException(BaseResponseStatus.INVALID_RUN_TYPE);
        }

        try {
            // 게시글 저장
            Post savedPost = postRepository.save(post);

            // 생성자 참여자로 추가 및 출석으로 변경
            Participant participant = new Participant(savedPost, user);
            participant.attend();
            participantRepository.save(participant);

            return savedPost.getId();

        } catch (Exception e) {
            throw new PostException(BaseResponseStatus.POST_CREATION_FAILED);
        }
    }

    // 게시판별 전체 게시글 조회
    public List<ReadPostsResponseDto> getPostsByRunType(String runType) {
        List<? extends Post> posts;

        switch (runType.toLowerCase()) {
            case "flash":
                posts = postRepository.findAllFlashPosts();
                break;
            default:
                throw new PostException(BaseResponseStatus.INVALID_RUN_TYPE);
        }

        return posts.stream()
                .map(ReadPostsResponseDto::of)
                .collect(Collectors.toList());
    }

    // 게시글 상세 조회
    public ReadPostDetailResponseDto getPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        return ReadPostDetailResponseDto.of(post);
    }
}
