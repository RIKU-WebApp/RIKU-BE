package RIKU.server.Service.Post;

import RIKU.server.Dto.Participant.Response.GroupParticipantResponse;
import RIKU.server.Dto.Participant.Response.ReadParticipantListResponse;
import RIKU.server.Dto.Post.Request.CreatePacerRequest;
import RIKU.server.Dto.Post.Request.CreateRegularPostRequest;
import RIKU.server.Dto.Post.Response.ReadCommentsResponse;
import RIKU.server.Dto.Post.Response.ReadPacersListResponse;
import RIKU.server.Dto.Post.Response.ReadRegularPostDetailResponse;
import RIKU.server.Dto.User.Response.ReadUserInfoResponse;
import RIKU.server.Entity.Board.Attachment;
import RIKU.server.Entity.Board.Comment;
import RIKU.server.Entity.Board.Pacer;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.Post.RegularPost;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.*;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.S3Uploader;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.ParticipantException;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegularPostService {

    private final PostRepository postRepository;
    private final RegularPostRepository regularPostRepository;
    private final UserRepository userRepository;
    private final PacerRepository pacerRepository;
    private final ParticipantRepository participantRepository;
    private final AttachmentRepository attachmentRepository;
    private final CommentRepository commentRepository;
    private final S3Uploader s3Uploader;

    // 게시글 생성
    @Transactional
    public Long createPost (AuthMember authMember, CreateRegularPostRequest request) {
        // 1. validate(권한 및 날짜 확인)
        validate(authMember, request);

        // 2. 게시글 작성자 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 3. S3에 게시글 이미지 업로드
        String postImageUrl = uploadSingleImage(request.getPostImage(), "postImg");

        // 4. Post 엔티티 생성 및 저장
        Post post = request.toPostEntity(user, postImageUrl);
        Post savedPost = postRepository.save(post);

        // 5. Pacer 엔티티 생성 및 저장
        List<Pacer> pacers = request.getPacers().stream()
                .map(pacer -> {
                    User pacerUser = userRepository.findById(pacer.getPacerId())
                                    .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

                    // 해당 유저가 페이서인지
                    if (!pacerUser.getIsPacer()) {
                        throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
                    }
                    // 해당 게시글에 이미 등록된 페이서인지
                    if (pacerRepository.existsByUserAndPost(pacerUser, savedPost)) {
                        throw new PostException(BaseResponseStatus.DUPLICATED_PACER);
                    }

                    return pacer.toEntity(pacerUser, savedPost);
                })
                .collect(Collectors.toList());
        pacerRepository.saveAll(pacers);

        // 6. S3에 첨부파일 이미지 업로드 및 저장
        List<Attachment> attachments = uploadMultipleImages(savedPost, request.getAttachments(), "attachmentImg");
        attachmentRepository.saveAll(attachments);

        // 7. RegularPost 엔티티 생성 및 저장
        RegularPost regularPost = request.toRegularPostEntity(savedPost);
        regularPostRepository.save(regularPost);

        try {
            // 게시글 작성자가 페이서로 지정된 그룹을 찾아서 참여자로 추가
            String creatorGroup = request.getPacers().stream()
                    .filter(p -> p.getPacerId().equals(authMember.getId()))
                    .findFirst()
                    .map(CreatePacerRequest::getGroup)
                    .orElseThrow(() -> new PostException(BaseResponseStatus.PACER_NOT_FOUND));

            Participant participant = Participant.createWithGroup(savedPost, user, creatorGroup);
            participantRepository.save(participant);

            return regularPost.getId();

        } catch (Exception e) {
            throw new PostException(BaseResponseStatus.POST_CREATION_FAILED);
        }
    }

    // 게시글 상세 조회
    public ReadRegularPostDetailResponse getPostDetail(Long postId, AuthMember authMember) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. 그룹별 참여자 조회
        List<Participant> participants = participantRepository.findByPost(post);

        if (participants.stream().anyMatch(p -> p.getGroup() == null)) {
            throw new ParticipantException(BaseResponseStatus.INVALID_PARTICIPANT_GROUP);
        }

        Map<String, List<ReadParticipantListResponse>> groupedMap = participants.stream()
                .collect(Collectors.groupingBy(
                        Participant::getGroup,
                        Collectors.mapping(ReadParticipantListResponse::of, Collectors.toList())
                ));

        List<GroupParticipantResponse> groupedParticipants = groupedMap.entrySet().stream()
                .map(entry -> GroupParticipantResponse.of(entry.getKey(), entry.getValue()))
                .toList();

        // 3. 페이서 조회
        List<ReadPacersListResponse> pacers = pacerRepository.findByPost(post)
                .stream()
                .map(ReadPacersListResponse::of)
                .toList();

        // 4. 첨부파일 조회
        List<String> attachmentUrls = attachmentRepository.findByPost(post)
                .stream()
                .map(Attachment::getImageUrl)
                .toList();

        // 5. 댓글 조회
        List<ReadCommentsResponse> comments = commentRepository.findByPost(post)
                .stream()
                .filter(comment -> comment.getTargetId() == null)
                .map(this::mapToDto)
                .toList();

        // 6. 게시글 작성자 정보
        ReadUserInfoResponse postCreator = ReadUserInfoResponse.of(post.getPostCreator());

        // 7. 현재 유저 정보
        User userEntity = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));
        ReadUserInfoResponse user = ReadUserInfoResponse.of(userEntity);

        return ReadRegularPostDetailResponse.of(post, postCreator, user, groupedParticipants, pacers, attachmentUrls, comments);
    }

    private ReadCommentsResponse mapToDto (Comment comment) {
        List<ReadCommentsResponse> replies = commentRepository.findByTargetId(comment.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ReadCommentsResponse.of(comment, replies);
    }

    private void validate(AuthMember authMember, CreateRegularPostRequest request) {
        // 1. 작성자가 운영진인지
        if(!authMember.isAdmin()) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        // 2. date가 미래인지
        LocalDateTime now = LocalDateTime.now();
        if (request.getDate().isBefore(now)) {
            throw new PostException(BaseResponseStatus.INVALID_DATE_AND_TIME);
        }

        // 3. 작성자가 페이서 리스트에 포함되어 있는 지 확인
        boolean isCreatorInPacerList = request.getPacers().stream()
                .anyMatch(p -> p.getPacerId().equals(authMember.getId()));

        if (!isCreatorInPacerList) {
            throw new PostException(BaseResponseStatus.CREATOR_NOT_IN_PACER_LIST);
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
