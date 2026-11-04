package RIKU.server.domains.run.post.service;

import RIKU.server.domains.run.recruitment.RunWaitlist;
import RIKU.server.domains.run.pacer.dto.request.UpdatePacerRequest;
import RIKU.server.domains.run.post.dto.request.UpdatePostRequest;
import RIKU.server.domains.run.pacer.dto.response.ReadPaceGroupResponse;
import RIKU.server.domains.run.post.dto.response.ReadPostListResponse;
import RIKU.server.domains.run.post.dto.response.ReadPostPreviewResponse;
import RIKU.server.global.entity.BaseStatus;
import RIKU.server.domains.run.post.entity.Attachment;
import RIKU.server.domains.run.pacer.entity.Pacer;
import RIKU.server.domains.run.post.entity.EventPost;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.PostType;
import RIKU.server.domains.run.post.entity.TrainingPost;
import RIKU.server.domains.run.post.entity.PostStatus;
import RIKU.server.domains.run.participant.entity.Participant;
import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.run.post.repository.*;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.domains.point.repository.UserPointRepository;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.pacer.repository.PacerRepository;
import RIKU.server.domains.run.comment.repository.CommentRepository;
import RIKU.server.security.AuthMember;
import RIKU.server.infra.storage.S3Uploader;
import RIKU.server.global.response.BaseResponseStatus;
import RIKU.server.domains.point.ranking.RankingChanged;
import RIKU.server.domains.run.participant.repository.PostParticipantCount;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import RIKU.server.domains.run.post.exception.PostException;
import RIKU.server.domains.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.stream.Stream;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static RIKU.server.global.util.DateTimeUtils.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final RunWaitlist waitlist;

    private final ApplicationEventPublisher events;
    private final PostRepository postRepository;
    private final RIKU.server.domains.run.recruitment.RecruitmentService recruitmentService;
    private final RIKU.server.domains.run.recruitment.RunEnrollmentRepository enrollments;
    private final RIKU.server.domains.run.recruitment.RunRecruitmentRepository recruitment;
    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;
    private final ParticipantRepository participantRepository;
    private final AttachmentRepository attachmentRepository;
    private final CommentRepository commentRepository;
    private final PacerRepository pacerRepository;
    private final RegularPostRepository regularPostRepository;
    private final FlashPostRepository flashPostRepository;
    private final TrainingPostRepository trainingPostRepository;
    private final EventPostRepository eventPostRepository;
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

        LocalDate today = LocalDate.now(getDefaultZone());
        LocalDateTime start = startOfDayUtc(today);
        LocalDateTime tomorrow = startOfDayUtc(today.plusDays(1));
        var todayRows = postRepository.findTodayPreviews(BaseStatus.ACTIVE, postType, start, tomorrow, PostStatus.NOW);
        var upcomingRows = postRepository.findUpcomingPreviews(BaseStatus.ACTIVE, postType, tomorrow);
        var pastRows = postRepository.findPastPreviews(BaseStatus.ACTIVE, postType, PostStatus.CLOSED, start, PageRequest.of(0, 6));
        var ids = Stream.of(todayRows, upcomingRows, pastRows).flatMap(List::stream).map(PostPreviewRow::id).toList();
        Map<Long, Long> counts = ids.isEmpty() ? Map.of() : participantRepository.countByPostIds(ids).stream()
                .collect(Collectors.toMap(PostParticipantCount::postId, PostParticipantCount::count));
        return ReadPostListResponse.of(previews(todayRows, counts), previews(upcomingRows, counts), previews(pastRows, counts));
    }

    private List<ReadPostPreviewResponse> previews(List<PostPreviewRow> rows, Map<Long, Long> counts) {
        return rows.stream().map(row -> ReadPostPreviewResponse.of(row, Math.toIntExact(counts.getOrDefault(row.id(), 0L)))).toList();
    }

    // 게시글 수정하기
    @Transactional
    public void updatePost(AuthMember authMember, String runType, Long postId, UpdatePostRequest request) {
        // 1. 게시글 조회
        Post post = postRepository.findForUpdate(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. 게시글 작성자 검증
        validatePostCreator(authMember, post);

        // 3. PostType 검증
        PostType postType = validatePostType(runType, post.getPostType(), post.getId());

        // 4. PostStatus 검증
        validatePostIsOpen(post);
        if (request.getCapacity() != null || request.isClearCapacity()) {
            if (request.getCapacity() != null && request.isClearCapacity()) {
                throw new PostException(BaseResponseStatus.INVALID_FIELD);
            }

        }

        boolean changeTotal = request.getCapacity() != null || request.isClearCapacity();
        if (postType == PostType.REGULAR && (changeTotal || request.isReplaceGroups()))
            recruitmentService.edit(post, changeTotal, request.getCapacity(), request.isReplaceGroups() ? request.plans() : null);
        // 5. 날짜 유효성 검증
        if (request.getDate() != null) {
            validateDate(request); // ← 날짜 유효성 검사
        }

        // Validate the complete replacement list before changing files in S3.
        if ((request.getPacers() != null || request.isReplacePacers()) &&
                (postType == PostType.REGULAR || postType == PostType.TRAINING)) {
            if (postType == PostType.REGULAR && recruitment.existsById(postId)) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                        "페이서는 정규런 신청 명단에서 관리합니다.");
            }
            boolean groupBased = postType == PostType.TRAINING && (request.isReplaceGroups()
                    ? !request.getGroupPlans().isEmpty() : waitlist.hasGroups(post));
            updatePacers(post, request.getPacers() == null ? List.of() : request.getPacers(), request.isReplacePacers(), groupBased);
        }
        if (postType != PostType.REGULAR) {
            if (changeTotal || request.isReplaceGroups())
                waitlist.configure(post, changeTotal, request.getCapacity(), request.isReplaceGroups() ? request.plans() : null);
            else if (postType == PostType.TRAINING && (request.isReplacePacers() || request.getPacers() != null))
                waitlist.fillVacancies(post);
        }


        // 6. 공통 필드 처리
        String title = request.getTitle() != null ? request.getTitle() : post.getTitle();
        String location = request.getLocation() != null ? request.getLocation() : post.getLocation();
        LocalDateTime date = request.getDate() != null ? request.getDate() : post.getDate();
        String content = request.getContent() != null ? request.getContent() : post.getContent();
        String postImageUrl = updateSingleImage(request.getPostImage(), post.getPostImageUrl(), "postImg");

        post.updatePost(title, location, date, content, postImageUrl);

        // 7. 첨부파일 처리
        updateMultipleImages(post, request.getAttachments(), "attachmentImg");

        // 8. 러닝 유형별 분기 처리
        switch (postType) {
            case REGULAR -> {}
            case TRAINING -> updateTrainingPost(post, request);
            case EVENT -> updateEventPost(post, request);
            case FLASH -> {}
            default -> throw new PostException(BaseResponseStatus.INVALID_RUN_TYPE);
        }
    }

    // 게시글 삭제하기
    @Transactional
    public void deletePost(AuthMember authMember, String runType, Long postId) {
        // 1. 게시글 조회
        Post post = postRepository.findForUpdate(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. 운영진 권한 검증
        if(!authMember.isAdmin()) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        // 3. PostType 검증
        PostType postType = validatePostType(runType, post.getPostType(), post.getId());

        // 4. 관련 엔티티 삭제
        if (recruitment.existsById(postId)) {
            enrollments.deleteByPostId(postId);
            recruitment.deleteById(postId);
        }
        var deletedPoints = userPointRepository.deleteByPost(post);
        participantRepository.deleteByPost(post);
        commentRepository.deleteByPost(post);
        pacerRepository.deleteByPost(post);

        attachmentRepository.findByPost(post).forEach(a -> s3Uploader.deleteFileByUrl(a.getImageUrl()));
        attachmentRepository.deleteByPost(post);

        switch(postType) {
            case REGULAR -> regularPostRepository.deleteByPost(post);
            case TRAINING -> trainingPostRepository.deleteByPost(post);
            case EVENT -> eventPostRepository.deleteByPost(post);
            case FLASH -> flashPostRepository.deleteByPost(post);
        }
        postRepository.delete(post);
        if (!deletedPoints.isEmpty()) events.publishEvent(RankingChanged.points(deletedPoints));
    }

    private void updateTrainingPost(Post post, UpdatePostRequest request) {
        // trainingType 수정
        if (request.getTrainingType() != null) {
            TrainingPost trainingPost = trainingPostRepository.findByPost(post)
                    .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));
            trainingPost.updateTrainingType(request.getTrainingType());
        }
    }

    private void updateEventPost(Post post, UpdatePostRequest request) {
        if (request.getEventType() != null) {
            EventPost eventPost = eventPostRepository.findByPost(post)
                    .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));
            eventPost.updateEventType(request.getEventType());
        }
    }

    // 게시글 러닝 취소
    @Transactional
    public void cancelPost(AuthMember authMember, String runType, Long postId) {
        // 1. 게시글 조회
        Post post = postRepository.findForUpdate(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. PostType 검증
        validatePostType(runType, post.getPostType(), post.getId());

        // 3. 게시글 작성자 검증
        validatePostCreator(authMember, post);

        // 4. 게시글 취소 처리
        post.updatePostStatus(PostStatus.CANCELED);
    }

    // 페이스 그룹 조회
    public List<ReadPaceGroupResponse> getPaceGroups(String runType, Long postId) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. PostType 검증
        PostType postType = validatePostType(runType, post.getPostType(), post.getId());
        if (postType != PostType.REGULAR && postType != PostType.TRAINING) {
            throw new PostException(BaseResponseStatus.UNAUTHORIZED_POST_TYPE);
        }

        return pacerRepository.findByPost(post)
                .stream()
                .map(ReadPaceGroupResponse::of)
                .toList();
    }

    private PostType validatePostType(String runType, PostType postType, Long postId) {
        log.info("[validatePostType] 원본 runType: '{}'", runType);

        try {
            PostType requestType = PostType.valueOf(runType.toUpperCase().trim());
            log.info("[validatePostType] requestType(enum): {}", requestType);
            log.info("[validatePostType] actual postType(enum from DB): {}", postType);

            if (!postType.equals(requestType)) {
                log.warn("[validatePostType] 게시글 타입 불일치 - postId: {}, 요청 runType: {}, 실제 postType: {}", postId, runType, postType);
                throw new PostException(BaseResponseStatus.INVALID_POST_TYPE);
            }
            return postType;
        } catch (IllegalArgumentException e) {
            log.error("[validatePostType] 잘못된 runType 요청: {}, postId: {}", runType, postId);
            throw new PostException(BaseResponseStatus.INVALID_RUN_TYPE);
        }
    }

    private void validatePostCreator(AuthMember authMember, Post post) {
        // 게시글 작성자 검증
        if (!post.getPostCreator().getId().equals(authMember.getId())) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }
    }

    private void validateDate(UpdatePostRequest request) {
        // 유효한 집합 날짜인지 확인
        LocalDateTime now = LocalDateTime.now();
        if (request.getDate().isBefore(now)) {
            throw new PostException(BaseResponseStatus.INVALID_DATE_AND_TIME);
        }
    }

    private void validatePostIsOpen(Post post) {
        if (!post.getPostStatus().equals(PostStatus.NOW)) {
            throw new PostException(BaseResponseStatus.INVALID_POST_STATUS);
        }
    }

    private String updateSingleImage(MultipartFile image, String originUrl, String dirName) {
        try {
            if (image != null && !image.isEmpty()) {
                if (originUrl != null) {
                    s3Uploader.deleteFileByUrl(originUrl);
                }
                return s3Uploader.upload(image, dirName);
            } else if (image != null && image.isEmpty()) {
                if (originUrl != null) {
                    s3Uploader.deleteFileByUrl(originUrl);
                }
                return null;
            }
            return originUrl;
        } catch (IOException e) {
            throw new PostException(BaseResponseStatus.POST_IMAGE_UPLOAD_FAILED);
        }
    }

    private void updateMultipleImages(Post post, List<MultipartFile> images, String dirName) {
        if (images == null) { return; }

        // 기존 첨부파일 삭제
        List<Attachment> existingAttachments  = attachmentRepository.findByPost(post);
        for (Attachment attachment : existingAttachments ) {
            s3Uploader.deleteFileByUrl(attachment.getImageUrl());
        }
        attachmentRepository.deleteAll(existingAttachments );

        // 요청된 파일이 비어있으면 첨부파일 제거만 하고 끝
        if (images.isEmpty()) return;

        // 새 첨부파일 업로드 및 저장
        List<Attachment> newAttachments = new ArrayList<>();
        for (MultipartFile file : images) {
            try {
                String uploadedUrl = s3Uploader.upload(file, dirName);
                newAttachments.add(Attachment.create(post, uploadedUrl));
            } catch (IOException e) {
                throw new PostException(BaseResponseStatus.ATTACHMENT_UPLOAD_FAILED);
            }
        }
        attachmentRepository.saveAll(newAttachments);
    }

    private void updatePacers(Post post, List<UpdatePacerRequest> pacerRequests, boolean replace, boolean groupBased) {
        List<Pacer> existingPacers = pacerRepository.findByPost(post);
        if (!replace && pacerRequests.size() != existingPacers.size()) {
            throw new PostException(BaseResponseStatus.INVALID_PACER_COUNT);
        }
        Set<Long> requestedIds = new HashSet<>();
        Map<Long, User> users = new HashMap<>();
        for (UpdatePacerRequest request : pacerRequests) {
            if (request == null || request.getPacerId() == null || request.getPacerId() <= 0 ||
                    request.getGroup() == null || request.getGroup().isBlank() ||
                    request.getPace() == null || request.getPace().isBlank() ||
                    request.getDistance() == null || request.getDistance().isBlank()) {
                throw new PostException(BaseResponseStatus.INVALID_FIELD);
            }
            if (!requestedIds.add(request.getPacerId())) {
                throw new PostException(BaseResponseStatus.DUPLICATED_PACER);
            }
            User user = userRepository.findById(request.getPacerId())
                    .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));
            if (user.getStatus() != BaseStatus.ACTIVE) {
                throw new UserException(BaseResponseStatus.INACTIVE_USER);
            }
            if (!Boolean.TRUE.equals(user.getIsPacer())) {
                throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
            }
            users.put(user.getId(), user);
        }
        if (!groupBased && !requestedIds.contains(post.getPostCreator().getId())) {
            throw new PostException(BaseResponseStatus.CREATOR_NOT_IN_PACER_LIST);
        }
        Map<Long, Pacer> pacersByUser = existingPacers.stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));
        Map<Long, Participant> participantsByUser = participantRepository.findByPost(post).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

        // Removing the pacer role does not cancel participation or erase attendance.
        pacerRepository.deleteAll(existingPacers.stream()
                .filter(p -> !requestedIds.contains(p.getUser().getId())).toList());
        for (UpdatePacerRequest request : pacerRequests) {
            User user = users.get(request.getPacerId());
            Pacer pacer = pacersByUser.get(user.getId());
            if (pacer == null) {
                pacerRepository.save(request.toEntity(user, post));
            } else {
                pacer.updateDetails(request.getGroup(), request.getPace(), request.getDistance());
            }
            Participant participant = participantsByUser.get(user.getId());
            if (participant == null) {
                if (post.getPostType() == PostType.TRAINING) waitlist.checkPacer(post, user.getId());
                participantRepository.save(Participant.createWithGroup(post, user, request.getGroup()));
            } else {
                // Keep identity, createdAt and attendance, including promoted participants.
                participant.updateGroup(request.getGroup());
            }
        }
    }
}
