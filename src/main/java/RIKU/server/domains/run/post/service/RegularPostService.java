package RIKU.server.domains.run.post.service;

import RIKU.server.domains.run.participant.dto.response.GroupParticipantResponse;
import RIKU.server.domains.run.participant.dto.response.ReadParticipantListResponse;
import RIKU.server.domains.run.comment.dto.response.ReadCommentsResponse;
import RIKU.server.domains.run.pacer.dto.response.ReadPacersListResponse;
import RIKU.server.domains.run.post.dto.response.ReadRegularPostDetailResponse;
import RIKU.server.domains.user.dto.response.ReadUserInfoResponse;
import RIKU.server.domains.run.post.entity.Attachment;
import RIKU.server.domains.run.pacer.entity.Pacer;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.participant.entity.Participant;
import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.run.post.repository.*;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.pacer.repository.PacerRepository;
import RIKU.server.domains.run.comment.service.CommentQueryService;
import RIKU.server.security.AuthMember;
import RIKU.server.global.response.BaseResponseStatus;
import RIKU.server.domains.run.participant.exception.ParticipantException;
import RIKU.server.domains.run.post.exception.PostException;
import RIKU.server.domains.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegularPostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PacerRepository pacerRepository;
    private final ParticipantRepository participantRepository;
    private final AttachmentRepository attachmentRepository;
    private final CommentQueryService commentQueryService;

    // 게시글 상세 조회
    public ReadRegularPostDetailResponse getPostDetail(Long postId, AuthMember authMember) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. 페이서 조회
        List<Pacer> pacerEntities = pacerRepository.findWithUserByPost(post);

        List<Long> pacerIds = pacerEntities.stream()
                .map(pacer -> pacer.getUser().getId())
                .toList();

        // 3. 그룹별 참여자 조회
        List<Participant> participants = participantRepository.findWithUserByPost(post);

        if (participants.stream().anyMatch(p -> p.getGroup() == null)) {
            throw new ParticipantException(BaseResponseStatus.INVALID_PARTICIPANT_GROUP);
        }

        Map<String, List<ReadParticipantListResponse>> groupedMap = participants.stream()
                .collect(Collectors.groupingBy(
                        Participant::getGroup,
                        Collectors.collectingAndThen(
                                Collectors.mapping(p -> ReadParticipantListResponse.of(p, pacerIds.contains(p.getUser().getId())), Collectors.toList()),
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
        List<ReadCommentsResponse> comments = commentQueryService.getComments(postId);

        // 7. 게시글 작성자 정보
        ReadUserInfoResponse postCreator = ReadUserInfoResponse.of(post.getPostCreator());

        // 8. 현재 유저 정보
        User userEntity = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));
        ReadUserInfoResponse user = ReadUserInfoResponse.of(userEntity);

        return ReadRegularPostDetailResponse.of(post, postCreator, user, groupedParticipants, pacers, attachmentUrls, comments);
    }

}
