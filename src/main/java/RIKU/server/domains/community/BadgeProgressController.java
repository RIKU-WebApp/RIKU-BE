package RIKU.server.domains.community;

import RIKU.server.global.response.BaseResponse;
import RIKU.server.security.AuthMember;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BadgeProgressController {
    private final JdbcTemplate db;
    private final String monthly;
    public BadgeProgressController(JdbcTemplate db) throws IOException {
        this.db=db;
        monthly="SELECT MIN(best_rank) FROM ("+new ClassPathResource("badges/monthly-ranks.sql")
                .getContentAsString(StandardCharsets.UTF_8).replace("__MONTH_KEY__", "?")+") r WHERE user_id=?";
    }
    public record Progress(int visits, int months, int kinds, int leads, int weekends, int streak,
                           Map<String,Integer> byType, int hosted, int checkins, int certificateDays, int fourSeasons, Integer bestMonthlyRank) {}

    @GetMapping("/user/activity/badge-progress")
    public BaseResponse<Progress> progress(@AuthenticationPrincipal AuthMember actor) {
        YearMonth now=YearMonth.now(ZoneId.of("Asia/Seoul"));
        Integer place=db.queryForObject(monthly,Integer.class,now.getYear()*100+now.getMonthValue(),actor.getId());
        int checkins = db.queryForObject("SELECT COUNT(DISTINCT CAST(TIMESTAMPADD(HOUR,9,created_at) AS DATE)) FROM user_point WHERE user_id=? AND point_type='ADD_ATTENDANCE'",Integer.class,actor.getId());
        int certificates = db.queryForObject("SELECT COUNT(DISTINCT date) FROM run_certificate WHERE user_id=?",Integer.class,actor.getId());
        int hosted = db.queryForObject("""
                SELECT COUNT(*) FROM post p WHERE p.user_id=? AND p.status='ACTIVE' AND p.post_status='CLOSED'
                AND EXISTS(SELECT 1 FROM participant a WHERE a.post_id=p.post_id AND a.user_id<>p.user_id AND a.participant_status='ATTENDED')
                """, Integer.class, actor.getId());
        var progress = db.query("""
            SELECT DISTINCT p.post_id, p.post_type, p.date,
                   EXISTS (SELECT 1 FROM pacer x WHERE x.post_id=p.post_id AND x.user_id=a.user_id) AS led
            FROM participant a JOIN post p ON p.post_id=a.post_id
            WHERE a.user_id=? AND a.participant_status='ATTENDED' AND p.status='ACTIVE' AND p.post_status='CLOSED'
            """, (ResultSetExtractor<Progress>) rows -> {
                int visits = 0, leads = 0, weekends = 0;
                Set<YearMonth> months = new HashSet<>();
                Set<Integer> seasons = new HashSet<>();
                Map<String,Integer> types = new HashMap<>();
                Set<LocalDate> weeks = new TreeSet<>();
                while (rows.next()) {
                    visits++;
                    if (rows.getBoolean("led")) leads++;
                    types.merge(rows.getString("post_type"), 1, Integer::sum);
                    var day = rows.getObject("date", java.time.LocalDateTime.class).plusHours(9).toLocalDate();
                    months.add(YearMonth.from(day));
                    seasons.add(day.getMonthValue()%12/3);
                    if (day.getDayOfWeek().getValue() >= 6) weekends++;
                    weeks.add(day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
                }
                int longest = 0, chain = 0;
                LocalDate last = null;
                for (LocalDate week : weeks) {
                    chain = last != null && last.plusWeeks(1).equals(week) ? chain + 1 : 1;
                    longest = Math.max(longest, chain);
                    last = week;
                }
                return new Progress(visits, months.size(), types.size(), leads, weekends, longest, types, hosted, checkins, certificates, seasons.size(), place);
            }, actor.getId());
        return new BaseResponse<>(progress);
    }
}
