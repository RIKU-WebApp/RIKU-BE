package RIKU.server.Service.Post;

import RIKU.server.Dto.Post.Request.CreatePostRequestDto;
import RIKU.server.Dto.Post.Response.ReadPostDetailResponseDto;
import RIKU.server.Dto.Post.Response.ReadPostsResponseDto;
import RIKU.server.Entity.Board.FlashPost;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.Participant.ParticipantStatus;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.ParticipantRepository;
import RIKU.server.Repository.PostRepository;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Service.S3Uploader;
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
public class FlashPostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final S3Uploader s3Uploader;

    // 번개런 게시글 조회
    public List<ReadPostsResponseDto> getAllFlashPosts() {
        List<FlashPost> posts = postRepository.findAllFlashPosts();

        return posts.stream()
                .map(ReadPostsResponseDto::of)
                .collect(Collectors.toList());
    }

    // 번개런 게시글 생성
    @Transactional
    public Long save(Long userId, CreatePostRequestDto requestDto) {
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

        FlashPost post = requestDto.flashToEntity(user, postImageUrl);

        try {
            // 게시글 저장
            FlashPost savedPost = postRepository.save(post);

            // 생성자 참여자로 추가 및 출석으로 변경
            Participant participant = new Participant(savedPost, user);
            participant.attend();
            participantRepository.save(participant);

            return savedPost.getId();

        } catch (Exception e) {
            log.error("Failed to create FlashPost", e);
            throw new PostException(BaseResponseStatus.POST_CREATION_FAILED);
        }
    }
}
