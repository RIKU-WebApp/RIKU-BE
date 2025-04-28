package RIKU.server.Service.Post;

import RIKU.server.Dto.Participant.Response.ReadParticipantListResponse;
import RIKU.server.Dto.Post.Request.CreateFlashPostRequest;
import RIKU.server.Dto.Post.Response.ReadCommentsResponse;
import RIKU.server.Dto.Post.Response.ReadFlashPostDetailResponse;
import RIKU.server.Dto.User.Response.ReadUserInfoResponse;
import RIKU.server.Entity.Board.Attachment;
import RIKU.server.Entity.Board.Comment;
import RIKU.server.Entity.Board.Post.FlashPost;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.*;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.S3Uploader;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.PostException;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FlashPostService {

    private final PostRepository postRepository;
    private final FlashPostRepository flashPostRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final AttachmentRepository attachmentRepository;
    private final CommentRepository commentRepository;
    private final S3Uploader s3Uploader;

    // 게시글 생성
    @Transactional
    public Long createPost (AuthMember authMember, CreateFlashPostRequest request) {
        // 1. validate(날짜 확인)
        validate(request);

        // 2. 게시글 작성자 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 3. 중복 글 검사
        if (postRepository.findByPostCreatorIdAndTitleAndDate(user.getId(), request.getTitle(), request.getDate()).isPresent()) {
            throw new PostException(BaseResponseStatus.DUPLICATE_POST);
        }

        // 4. S3에 게시글 이미지 업로드
        String postImageUrl = uploadSingleImage(request.getPostImage(), "postImg");

        // 5. Post 엔티티 생성 및 저장
        Post post = request.toPostEntity(user, postImageUrl);
        Post savedPost = postRepository.save(post);

        // 6. S3에 첨부파일 이미지 업로드 및 저장
        List<Attachment> attachments = uploadMultipleImages(savedPost, request.getAttachments(), "attachmentImg");
        attachmentRepository.saveAll(attachments);

        // 7. FlashPost 엔티티 생성 및 저장
        FlashPost flashPost = request.toFlashPostEntity(savedPost);
        flashPostRepository.save(flashPost);

        try {
            // 게시글 작성자를 참여자로 추가
            Participant participant = Participant.create(savedPost, user);
            participantRepository.save(participant);

            return flashPost.getId();

        } catch (Exception e) {
            throw new PostException(BaseResponseStatus.POST_CREATION_FAILED);
        }
    }

    // 게시글 상세 조회
    public ReadFlashPostDetailResponse getPostDetail(Long postId, AuthMember authMember) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. 참여자 조회
        List<ReadParticipantListResponse> participants = participantRepository.findByPost(post)
                .stream()
                .map(ReadParticipantListResponse::of)
                .toList();

        // 3. 첨부파일 조회
        List<String> attachmentUrls = attachmentRepository.findByPost(post)
                .stream()
                .map(Attachment::getImageUrl)
                .toList();

        // 4. 댓글 조회
        List<ReadCommentsResponse> comments = commentRepository.findByPost(post)
                .stream()
                .filter(comment -> comment.getTargetId() == null)
                .map(this::mapToDto)
                .toList();

        // 5. 게시글 작성자 정보
        ReadUserInfoResponse postCreator = ReadUserInfoResponse.of(post.getPostCreator());

        // 6. 현재 유저 정보
        User userEntity = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));
        ReadUserInfoResponse user = ReadUserInfoResponse.of(userEntity);

        return ReadFlashPostDetailResponse.of(post, participants, postCreator, attachmentUrls, user, comments);
    }

    private ReadCommentsResponse mapToDto (Comment comment) {
        List<ReadCommentsResponse> replies = commentRepository.findByTargetId(comment.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ReadCommentsResponse.of(comment, replies);
    }

    private void validate(CreateFlashPostRequest request) {
        // 1. date가 미래인지
        LocalDateTime now = LocalDateTime.now();
        if (request.getDate().isBefore(now)) {
            throw new PostException(BaseResponseStatus.INVALID_DATE_AND_TIME);
        }
    }

    private String uploadSingleImage(MultipartFile image, String dirName) {
        if(image != null && !image.isEmpty()) {
            try {
                return s3Uploader.upload(image, dirName); // S3에 이미지 업로드
            } catch (IOException e) {
                log.error("File upload failed: {}", image.getOriginalFilename(), e);
                throw new PostException(BaseResponseStatus.POST_IMAGE_UPLOAD_FAILED);
            }
        }
        return null;
    }

    private List<Attachment> uploadMultipleImages(Post post, List<MultipartFile> images, String dirName) {
        List<Attachment> attachments = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                String attachmentUrl = null;
                try {
                    attachmentUrl = s3Uploader.upload(file, dirName);
                } catch (IOException e) {
                    throw new PostException(BaseResponseStatus.ATTACHMENT_UPLOAD_FAILED);
                }
                attachments.add(Attachment.create(post, attachmentUrl));
            }
        }
        return attachments;
    }
}
