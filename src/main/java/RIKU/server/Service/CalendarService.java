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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static RIKU.server.Util.DateTimeUtils.*;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    // 해당 일자 일정 조회
    public List<ReadDailyScheduleResponse> getDailySchedule(LocalDate date) {
        log.info("📅 [일간 일정 조회 요청] 입력받은 날짜 (KST): {}", date);

        // 1. 시작/끝 날짜 계산 (UTC 기준)
        LocalDateTime utcStart = startOfDayUtc(date);
        LocalDateTime utcEnd = endOfDayUtc(date);
        log.info("🕒 조회 범위 (UTC): {} ~ {}", utcStart, utcEnd);

        // 2. DB 조회 (UTC 기준)
        List<Post> posts = postRepository.findByStatusAndDateBetween(BaseStatus.ACTIVE, utcStart, utcEnd);
        log.info("📌 조회된 게시글 수: {}", posts.size());

        posts.forEach(post ->
                log.info("🗓 Post ID={} | 저장된 시간(UTC): {} | 변환된 KST 날짜: {}",
                        post.getId(),
                        post.getDate(),
                        toUserLocalDate(post.getDate()))
        );

        // 3.  DTO 변환
        return posts.stream()
                .map(ReadDailyScheduleResponse::of)
                .collect(Collectors.toList());
    }

    // 월별 게시글 조회
    public ListMonthlyScheduleResponse getMonthlySchedule(AuthMember authMember, LocalDate date) {
        log.info("🔍 JVM 기본 시간대: {}", ZoneId.systemDefault());
        log.info("🔍 현재 시간 (LocalDateTime.now()): {}", LocalDateTime.now());
        log.info("🔍 현재 시간 (ZonedDateTime.now()): {}", ZonedDateTime.now());

        // 1. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 2. 해당 월의 시작/끝 날짜 계산 (KST 기준)
        LocalDate firstDay = date.withDayOfMonth(1);
        LocalDate lastDay = date.withDayOfMonth(date.lengthOfMonth());
        log.info("📆 [월간 일정 조회 요청] 기준 월: {} | 시작일: {} | 종료일: {}", date, firstDay, lastDay);

        // 3. 시간 범위 계산 (UTC 기준)
        LocalDateTime utcStart = startOfDayUtc(firstDay);
        LocalDateTime utcEnd = endOfDayUtc(lastDay);
        log.info("🕒 조회 범위 (UTC): {} ~ {}", utcStart, utcEnd);

        // 4. DB 조회 (UTC 기준)
        List<Post> posts = postRepository.findByStatusAndDateBetween(BaseStatus.ACTIVE, utcStart, utcEnd);
        log.info("📌 월간 조회된 게시글 수: {}", posts.size());

        posts.forEach(post ->
                log.info("🗓 Post ID={} | 저장된 시간(UTC): {} | 변환된 KST 날짜: {}",
                        post.getId(),
                        post.getDate(),
                        toUserLocalDate(post.getDate()))
        );

        // 5. 날짜별 그룹핑 (KST 기준)
        Map<LocalDate, Long> eventCounts = posts.stream()
                .collect(Collectors.groupingBy(post -> toUserLocalDate(post.getDate()), Collectors.counting()));

        eventCounts.forEach((groupedDate, count) ->
                log.info("📊 그룹핑된 KST 날짜: {} | 게시글 수: {}", groupedDate, count)
        );

        // 6. DTO 변환 및 정렬
        List<ReadMonthlyScheduleResponse> schedules = eventCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> ReadMonthlyScheduleResponse.of(entry.getKey(), entry.getValue()))
                .toList();

        return ListMonthlyScheduleResponse.of(user, schedules);
    }
}
