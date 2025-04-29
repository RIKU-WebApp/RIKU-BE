package RIKU.server.Service.Post;

import RIKU.server.Dto.Participant.Response.GroupParticipantResponse;
import RIKU.server.Dto.Participant.Response.ReadParticipantListResponse;
import RIKU.server.Dto.Post.Request.CreatePacerRequest;
import RIKU.server.Dto.Post.Request.CreateTrainingPostRequest;
import RIKU.server.Dto.Post.Response.ReadCommentsResponse;
import RIKU.server.Dto.Post.Response.ReadPacersListResponse;
import RIKU.server.Dto.Post.Response.ReadRegularPostDetailResponse;
import RIKU.server.Dto.Post.Response.ReadTrainingPostDetailResponse;
import RIKU.server.Dto.User.Response.ReadUserInfoResponse;
import RIKU.server.Entity.Board.Attachment;
import RIKU.server.Entity.Board.Comment;
import RIKU.server.Entity.Board.Pacer;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.Post.TrainingPost;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TrainingPostService {

    private final PostRepository postRepository;
    private final TrainingPostRepository trainingPostRepository;
    private final UserRepository userRepository;
    private final PacerRepository pacerRepository;
    private final ParticipantRepository participantRepository;
    private final AttachmentRepository attachmentRepository;
    private final CommentRepository commentRepository;
    private final S3Uploader s3Uploader;

    // 게시글 생성
    @Transactional
    public Long createPost (AuthMember authMember, CreateTrainingPostRequest request) {
        // 1. validate(권한 및 날짜 확인)
        validate(authMember, request);

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

        // 6. 페이서 중복 검증
        List<CreatePacerRequest> pacerRequests = request.getPacers();
        List<Long> pacerIds = pacerRequests.stream()
                .map(CreatePacerRequest::getPacerId)
                .toList();

        Set<Long> uniquePacerIds = new HashSet<>(pacerIds);
        if (uniquePacerIds.size() < pacerIds.size()) {
            throw new PostException(BaseResponseStatus.DUPLICATED_PACER);
        }

        // 7. Pacer 엔티티 생성 및 저장
        List<Pacer> pacers = request.getPacers().stream()
                .map(pacer -> {
                    User pacerUser = userRepository.findById(pacer.getPacerId())
                            .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

                    // 해당 유저가 페이서인지
                    if (!pacerUser.getIsPacer()) {
                        throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
                    }

                    return pacer.toEntity(pacerUser, savedPost);
                })
                .collect(Collectors.toList());
        pacerRepository.saveAll(pacers);

        // 8. S3에 첨부파일 이미지 업로드 및 저장
        List<Attachment> attachments = uploadMultipleImages(savedPost, request.getAttachments(), "attachmentImg");
        attachmentRepository.saveAll(attachments);

        // 9. TrainingPost 엔티티 생성 및 저장
        TrainingPost trainingPost = request.toTrainingPostEntity(savedPost, request.getTrainingType());
        trainingPostRepository.save(trainingPost);

        // 10. 페이서 목록 지정된 그룹에 따라 참여자로 추가
        List<Participant> participants = request.getPacers().stream()
                .map(p -> {
                    User pacerUser = userRepository.findById(p.getPacerId())
                            .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

                    return Participant.createWithGroup(savedPost, pacerUser, p.getGroup());
                })
                .collect(Collectors.toList());
        participantRepository.saveAll(participants);

        return trainingPost.getId();
    }

    // 게시글 상세 조회
    public ReadTrainingPostDetailResponse getPostDetail(Long postId, AuthMember authMember) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        TrainingPost trainingPost = trainingPostRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.TRAINING_POST_NOT_FOUND));

        // 2. 페이서 조회
        List<Pacer> pacerEntities = pacerRepository.findByPost(post);

        List<Long> pacerIds = pacerEntities.stream()
                .map(pacer -> pacer.getUser().getId())
                .toList();

        // 3. 그룹별 참여자 조회
        List<Participant> participants = participantRepository.findByPost(post);

        if (participants.stream().anyMatch(p -> p.getGroup() == null)) {
            throw new ParticipantException(BaseResponseStatus.INVALID_PARTICIPANT_GROUP);
        }

        Map<String, List<ReadParticipantListResponse>> groupedMap = participants.stream()
                .collect(Collectors.groupingBy(
                        Participant::getGroup,
                        Collectors.collectingAndThen(
                                Collectors.mapping(ReadParticipantListResponse::of, Collectors.toList()),
                                list -> list.stream()
                                        .sorted((p1, p2) ->  {
                                            boolean p1IsPacer = pacerIds.contains(p1.getUserId());
                                            boolean p2IsPacer = pacerIds.contains(p2.getUserId());
                                            return Boolean.compare(!p1IsPacer, !p2IsPacer);
                                        })
                                        .collect(Collectors.toList())
                        )
                ));

        List<GroupParticipantResponse> groupedParticipants = groupedMap.entrySet().stream()
                .map(entry -> GroupParticipantResponse.of(entry.getKey(), entry.getValue()))
                .toList();

        // 4. 페이서 응답 변환
        List<ReadPacersListResponse> pacers = pacerEntities.stream()
                .map(ReadPacersListResponse::of)
                .toList();

        // 5. 첨부파일 조회
        List<String> attachmentUrls = attachmentRepository.findByPost(post)
                .stream()
                .map(Attachment::getImageUrl)
                .toList();

        // 6. 댓글 조회
        List<ReadCommentsResponse> comments = commentRepository.findByPost(post)
                .stream()
                .filter(comment -> comment.getTargetId() == null)
                .map(this::mapToDto)
                .toList();

        // 7. 게시글 작성자 정보
        ReadUserInfoResponse postCreator = ReadUserInfoResponse.of(post.getPostCreator());

        // 8. 현재 유저 정보
        User userEntity = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));
        ReadUserInfoResponse user = ReadUserInfoResponse.of(userEntity);

        return ReadTrainingPostDetailResponse.of(post, trainingPost, groupedParticipants, postCreator, pacers, attachmentUrls, user, comments);
    }

    private ReadCommentsResponse mapToDto (Comment comment) {
        List<ReadCommentsResponse> replies = commentRepository.findByTargetId(comment.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ReadCommentsResponse.of(comment, replies);
    }

    private void validate(AuthMember authMember, CreateTrainingPostRequest request) {
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
