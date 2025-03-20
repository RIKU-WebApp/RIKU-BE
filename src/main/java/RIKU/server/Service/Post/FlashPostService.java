package RIKU.server.Service.Post;

import RIKU.server.Dto.Post.Request.CreateFlashPostRequest;
import RIKU.server.Entity.Board.Attachment;
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
    private final S3Uploader s3Uploader;

    // 게시글 생성
    @Transactional
    public Long createPost (AuthMember authMember, CreateFlashPostRequest request) {
        // 1. validate(날짜 확인)
        validate(request);

        // 2. 게시글 작성자 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 3. S3에 게시글 이미지 업로드
        String postImageUrl = uploadSingleImage(request.getPostImage(), "postImg");

        // 4. Post 엔티티 생성 및 저장
        Post post = request.toPostEntity(user, postImageUrl);
        Post savedPost = postRepository.save(post);

        // 5. S3에 첨부파일 이미지 업로드 및 저장
        List<Attachment> attachments = uploadMultipleImages(savedPost, request.getAttachments(), "attachmentImg");
        attachmentRepository.saveAll(attachments);

        // 6. RegularPost 엔티티 생성 및 저장
        FlashPost flashPost = request.toFlashPostEntity(savedPost);
        flashPostRepository.save(flashPost);

        try {
            // 게시글 작성자를 참여자로 추가 및 출석으로 변경
            Participant participant = Participant.create(savedPost, user);
            participant.attend();
            participantRepository.save(participant);

            return flashPost.getId();

        } catch (Exception e) {
            throw new PostException(BaseResponseStatus.POST_CREATION_FAILED);
        }
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
