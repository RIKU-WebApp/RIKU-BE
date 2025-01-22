package RIKU.server.Service;

import RIKU.server.Dto.Post.Request.CreatePostRequestDto;
import RIKU.server.Dto.Post.Response.ReadCommentsResponseDto;
import RIKU.server.Dto.Post.Response.ReadPostDetailResponseDto;
import RIKU.server.Dto.Post.Response.ReadPostsResponseDto;
import RIKU.server.Entity.BaseStatus;
import RIKU.server.Entity.Board.Comment;
import RIKU.server.Entity.Board.Post;
import RIKU.server.Entity.Board.PostStatus;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.CommentRepository;
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
import java.time.LocalDateTime;
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
    private final CommentRepository commentRepository;
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

        LocalDateTime now = LocalDateTime.now();
        if(requestDto.getDate().isBefore(now)) {
            throw new PostException(BaseResponseStatus.INVALID_DATE_AND_TIME);
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
        // 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 댓글 조회
        List<ReadCommentsResponseDto> comments = commentRepository.findByPost(post).stream()
                .filter(comment -> comment.getTargetId() == null) // 최상위 댓글만 조회
                .map(this::mapCommentToDto) // 댓글 -> DTO 변환
                .toList();
        return ReadPostDetailResponseDto.of(post, comments);
    }

    // 댓글 -> DTO 변환
    private ReadCommentsResponseDto mapCommentToDto(Comment comment) {
        // 대댓글 리스트 변환
        List<ReadCommentsResponseDto> replies = commentRepository.findByTargetId(comment.getId()).stream()
                .map(this::mapCommentToDto)
                .collect(Collectors.toList());

        return ReadCommentsResponseDto.of(comment, replies);
    }

    // 게시글 수정하기
    @Transactional
    public Long updatePost(Long userId, Long postId, CreatePostRequestDto requestDto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 게시글 작성자 검증
        if (!post.getCreatedBy().getId().equals(userId)) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        // 유효한 집합 날짜인지 확인
        LocalDateTime now = LocalDateTime.now();
        if (requestDto.getDate().isBefore(now)) {
            throw new PostException(BaseResponseStatus.INVALID_DATE_AND_TIME);
        }

        String postImageUrl = post.getPostImageUrl();

        // 이미지 처리
        if (requestDto.getPostImage() != null) {
            if (!requestDto.getPostImage().isEmpty()) {
                // 새로운 이미지 업로드
                try {
                    log.debug("Received post image: {}", requestDto.getPostImage().getOriginalFilename());
                    postImageUrl = s3Uploader.upload(requestDto.getPostImage(), "postImg");
                    log.debug("post image uploaded: {}", postImageUrl);

                } catch (IOException e) {
                    log.error("File upload failed: {}", requestDto.getPostImage().getOriginalFilename(), e);
                    throw new PostException(BaseResponseStatus.POST_IMAGE_UPLOAD_FAILED);
                }
            }
        } else {
            // 이미지를 없애는 경우
            postImageUrl = null;

        }

        post.updatePost(requestDto.getTitle(), requestDto.getLocation(), requestDto.getDate(), requestDto.getContent(), postImageUrl);

        return post.getId();
    }



    // 게시글 삭제하기
    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 게시글 작성자 검증
        if (!post.getCreatedBy().getId().equals(userId)) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        post.updateStatus(PostStatus.CANCELED);
    }
}
