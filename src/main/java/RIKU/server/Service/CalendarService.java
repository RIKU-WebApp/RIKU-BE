package RIKU.server.Service;

import RIKU.server.Dto.Calendar.Response.ListMonthlyScheduleResponse;
import RIKU.server.Dto.Calendar.Response.ReadDailyScheduleResponse;
import RIKU.server.Dto.Calendar.Response.ReadMonthlyScheduleResponse;
import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.PostRepository;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Security.AuthMember;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    // 해당 일자 일정 조회
    public List<ReadDailyScheduleResponse> getDailySchedule(LocalDate date) {

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // 해당 날짜의 게시글 조회
        List<Post> posts = postRepository.findByStatusAndDateBetween(BaseStatus.ACTIVE, startOfDay, endOfDay)
                .stream()
                .sorted(Comparator.comparing(Post::getDate))
                .toList();

        // 게시글 정보를 DTO로 변환
        return posts.stream()
                .map(ReadDailyScheduleResponse::of)
                .collect(Collectors.toList());
    }

    // 월별 게시글 조회
    public ListMonthlyScheduleResponse getMonthlySchedule(AuthMember authMember, LocalDate date) {
        // 1. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 날짜 계산
        LocalDateTime startOfMonth = date.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = date.withDayOfMonth(date.lengthOfMonth()).atTime(23, 59, 59);

        // 해당 월의 게시글 조회
        List<Post> posts = postRepository.findByStatusAndDateBetween(BaseStatus.ACTIVE, startOfMonth, endOfMonth);

        // 날짜별 게시물 개수 집계
        Map<LocalDate, Long> eventCounts = posts.stream()
                .collect(Collectors.groupingBy(post -> post.getDate().toLocalDate(), Collectors.counting()));

        List<ReadMonthlyScheduleResponse> schedules = eventCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> ReadMonthlyScheduleResponse.of(entry.getKey(), entry.getValue()))
                .toList();

        return ListMonthlyScheduleResponse.of(user, schedules);
    }
}
