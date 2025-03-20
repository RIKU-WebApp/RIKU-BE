package RIKU.server.Service.Post;

import RIKU.server.Dto.Post.Request.UpdatePostRequest;
import RIKU.server.Dto.Post.Response.ReadPostListResponse;
import RIKU.server.Dto.Post.Response.ReadPostPreviewResponse;
import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.Post.PostType;
import RIKU.server.Entity.Board.PostStatus;
import RIKU.server.Repository.CommentRepository;
import RIKU.server.Repository.ParticipantRepository;
import RIKU.server.Repository.PostRepository;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.S3Uploader;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.PostException;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
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


    // 러닝 타입별 게시글 리스트 조회
    public ReadPostListResponse getPostsByRunType(String runType) {
        // 1. runType에 해당하는 PostType 정의
        PostType postType;
        try {
            postType = PostType.valueOf(runType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 runType입니다: " + runType);
        }

        // 2. DB에서 ACTIVE인 해당 postType의 모든 게시글 조회
        List<Post> posts = postRepository.findByStatusAndPostType(BaseStatus.ACTIVE, postType);

        // 3. 현재 날짜 기준
        LocalDateTime now = LocalDateTime.now();

        // 4. 게시글 분류
        // 오늘의 러닝 (오늘 날짜, 모집 중 or 마감 임박, 취소됨 제외)
        List<ReadPostPreviewResponse> todayRuns = posts.stream()
                .filter(post -> isToday(post.getDate(), now))
                .filter(post -> post.getPostStatus() != PostStatus.CANCELED)
                .map(post -> ReadPostPreviewResponse.of(post, countParticipants(post.getId())))
                .collect(Collectors.toList());

        // 예정된 러닝 (오늘 이후, 가장 빠른 날짜순 정렬, 취소됨 포함)
        List<ReadPostPreviewResponse> upcomingRuns = posts.stream()
                .filter(post -> post.getDate().isAfter(now))
                .sorted(Comparator.comparing(Post::getDate))
                .map(post -> ReadPostPreviewResponse.of(post, countParticipants(post.getId())))
                .collect(Collectors.toList());

        // 지난 러닝 (오늘 이전, 마감된 러닝만)
        List<ReadPostPreviewResponse> pastRuns = posts.stream()
                .filter(post -> post.getDate().isBefore(now))
                        .filter(post -> post.getPostStatus() == PostStatus.CLOSED)
                .map(post -> ReadPostPreviewResponse.of(post, countParticipants(post.getId())))
                .collect(Collectors.toList());

        return ReadPostListResponse.of(todayRuns, upcomingRuns, pastRuns);
    }

//    // 게시글 상세 조회
//    public ReadPostDetailResponseDto getPostDetail(Long postId) {
//        // 게시글 조회
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));
//
//        // 댓글 조회
//        List<ReadCommentsResponseDto> comments = commentRepository.findByPost(post).stream()
//                .filter(comment -> comment.getTargetId() == null) // 최상위 댓글만 조회
//                .map(this::mapCommentToDto) // 댓글 -> DTO 변환
//                .toList();
//        return ReadPostDetailResponseDto.of(post, comments);
//    }
//
//    // 댓글 -> DTO 변환
//    private ReadCommentsResponseDto mapCommentToDto(Comment comment) {
//        // 대댓글 리스트 변환
//        List<ReadCommentsResponseDto> replies = commentRepository.findByTargetId(comment.getId()).stream()
//                .map(this::mapCommentToDto)
//                .collect(Collectors.toList());
//
//        return ReadCommentsResponseDto.of(comment, replies);
//    }

//    // 게시글 수정하기
//    @Transactional
//    public void updatePost(AuthMember authMember, String runType, Long postId, UpdatePostRequest request) {
//        // 게시글 조회
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));
//
//        // 게시글 작성자 검증
//        if (!post.getPostCreator().getId().equals(authMember.getId())) {
//            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
//        }
//
//        // 유효한 집합 날짜인지 확인
//        LocalDateTime now = LocalDateTime.now();
//        if (request.getDate().isBefore(now)) {
//            throw new PostException(BaseResponseStatus.INVALID_DATE_AND_TIME);
//        }
//
//        String postImageUrl = post.getPostImageUrl();
//
//        // 이미지 처리
//        if (request.getPostImage() != null) {
//            if (!request.getPostImage().isEmpty()) {
//                // 새로운 이미지 업로드
//                try {
//                    log.debug("Received post image: {}", request.getPostImage().getOriginalFilename());
//                    postImageUrl = s3Uploader.upload(request.getPostImage(), "postImg");
//                    log.debug("post image uploaded: {}", postImageUrl);
//
//                } catch (IOException e) {
//                    log.error("File upload failed: {}", request.getPostImage().getOriginalFilename(), e);
//                    throw new PostException(BaseResponseStatus.POST_IMAGE_UPLOAD_FAILED);
//                }
//            }
//        } else {
//            // 이미지를 없애는 경우
//            postImageUrl = null;
//
//        }
//        post.updatePost(request.getTitle(), request.getLocation(), request.getDate(), request.getContent(), postImageUrl);
//    }


    // 게시글 삭제하기
    @Transactional
    public void cancelPost(AuthMember authMember, String runType, Long postId) {
        // 1. PostType 검증
        PostType postType = validatePostType(runType);

        // 2. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 3. 게시글 작성자 검증
        if (postType == PostType.FLASH) {
            // 번개런이면 생성자 권한으로 취소 가능
            if (!post.getPostCreator().getId().equals(authMember.getId())) {
                throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
            }
        } else {
            // 번개런이 아닌 경우 운영진만 취소 가능
            if (!authMember.isAdmin()) {
                throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
            }
        }

        // 4. 게시글 취소 처리
        post.updateStatus(PostStatus.CANCELED);
    }

    private PostType validatePostType(String runType) {
        try {
            return PostType.valueOf(runType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 runType 입니다: " + runType);
        }
    }

    private boolean isToday(LocalDateTime postDate, LocalDateTime now) {
        return postDate.toLocalDate().isEqual(now.toLocalDate());
    }

    private int countParticipants(Long postId) {
        return participantRepository.countByPostId(postId);
    }
}
