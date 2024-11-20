package RIKU.server.Service;

import RIKU.server.Dto.Calendar.Response.DailyScheduleResponseDto;
import RIKU.server.Dto.Calendar.Response.MonthlyScheduleResponseDto;
import RIKU.server.Entity.Board.Post;
import RIKU.server.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final PostRepository postRepository;

    // 해당 날짜 게시글 조회
    public List<DailyScheduleResponseDto> getDailySchedule(LocalDate date) {

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // 해당 날짜의 게시글 조회
        List<Post> posts = postRepository.findByDateBetween(startOfDay, endOfDay)
                .stream()
                .sorted((post1, post2) -> post1.getDate().compareTo(post2.getDate()))
                .toList();

        // 게시글 정보를 DTO로 변환
        return posts.stream()
                .map(post -> new DailyScheduleResponseDto(post.getTitle(), post.getDate(), post.getLocation()))
                .collect(Collectors.toList());
    }

    // 월별 게시글 조회
    public List<MonthlyScheduleResponseDto> getMonthlySchedule(LocalDate date) {

        LocalDateTime startOfMonth = date.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = date.withDayOfMonth(date.lengthOfMonth()).atTime(23, 59, 59);

        // 해당 월의 게시글 조회
        List<Post> posts = postRepository.findByDateBetween(startOfMonth, endOfMonth);

        // 날짜별 게시물 개수 집계
        Map<LocalDate, Long> eventCounts = posts.stream()
                .collect(Collectors.groupingBy(post -> post.getDate().toLocalDate(), Collectors.counting()));

        return eventCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> MonthlyScheduleResponseDto.builder()
                        .date(entry.getKey())
                        .eventCount(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}
