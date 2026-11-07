package RIKU.server.domains.community;

import RIKU.server.security.AuthMember;
import RIKU.server.global.response.BaseResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
public class RankHonors {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final JdbcTemplate db;
    private final String podiumSql;
    private final List<LocalDate> terms;
    private final BadgeStorage storage;
    private final boolean preview;
    public RankHonors(JdbcTemplate db, Environment env, @Value("${season.start-date}") LocalDate currentStart,
                      BadgeStorage storage) throws IOException {
        this.db=db;
        podiumSql=new ClassPathResource("badges/rank-podium.sql").getContentAsString(StandardCharsets.UTF_8);
        this.storage=storage;
        this.preview=env.acceptsProfiles(Profiles.of("preview"));
        var history=new ClassPathResource("badges/semester-starts.txt").getContentAsString(StandardCharsets.UTF_8);
        var starts=new TreeSet<LocalDate>();
        history.lines().filter(s->!s.isBlank()).map(LocalDate::parse).forEach(starts::add);
        starts.add(currentStart);
        terms=List.copyOf(starts);
        if(storage.hasCalendar()) for(var start:terms)db.update("""
            INSERT INTO badge_semester_start(starts_on) VALUES(?)
            ON DUPLICATE KEY UPDATE starts_on=badge_semester_start.starts_on
            """,start);
    }
    record Period(String kind, LocalDate start, LocalDate endExclusive) {}
    record Winner(long member, int place) {}
    public record Medal(String kind, LocalDate start, LocalDate achievedOn, int place) {}

    List<Period> closedPeriods(LocalDate today) {
        var periods=new ArrayList<Period>();
        LocalDateTime first=db.queryForObject("SELECT MIN(created_at) FROM user_point", LocalDateTime.class);
        if(first!=null){
            var month=YearMonth.from(first.plusHours(9));
            while(month.isBefore(YearMonth.from(today))){
                periods.add(new Period("MONTH",month.atDay(1),month.plusMonths(1).atDay(1)));
                month=month.plusMonths(1);
            }
        }
        var calendar=storage.hasCalendar()
                ? db.query("SELECT starts_on FROM badge_semester_start ORDER BY starts_on",(rs,n)->rs.getDate(1).toLocalDate()) : terms;
        for(int i=1;i<calendar.size();i++) {
            if(!calendar.get(i).isAfter(today)) periods.add(new Period("SEMESTER",calendar.get(i-1),calendar.get(i)));
        }
        periods.sort(Comparator.comparing(Period::endExclusive).thenComparing(Period::kind));
        return periods;
    }
    List<Winner> podium(Period period) {
        return db.query(podiumSql,(rs,n)->new Winner(rs.getLong(1),rs.getInt(2)), utcStart(period.start()),utcStart(period.endExclusive()),
                period.kind().equals("MONTH")?20:1);
    }
    private static LocalDateTime utcStart(LocalDate date) {return date.atStartOfDay(SEOUL).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();}

    @Transactional
    public void settle() {
        if(!storage.enabled() || !storage.rankingsEnabled())return;
        var done=new HashSet<>(db.query("SELECT kind,starts_on FROM rank_honor_period WHERE settled=TRUE",
                (rs,n)->rs.getString(1)+rs.getDate(2)));
        for(var p:closedPeriods(LocalDate.now(SEOUL))) {
            if(done.contains(p.kind()+p.start()))continue;
            db.update("""
                    INSERT INTO rank_honor_period(kind,starts_on,ends_before,settled) VALUES(?,?,?,FALSE)
                    ON DUPLICATE KEY UPDATE kind=rank_honor_period.kind
                    """,p.kind(),p.start(),p.endExclusive());
            boolean settled=Boolean.TRUE.equals(db.queryForObject("SELECT settled FROM rank_honor_period WHERE kind=? AND starts_on=? FOR UPDATE",Boolean.class,p.kind(),p.start()));
            if(settled)continue;
            var winners=podium(p);
            if(winners.isEmpty())continue; // A late import can make an empty/undersized period eligible.
            for(var w:winners)db.update("INSERT INTO rank_honor(kind,starts_on,user_id,place_no) VALUES(?,?,?,?)",p.kind(),p.start(),w.member(),w.place());
            db.update("UPDATE rank_honor_period SET settled=TRUE WHERE kind=? AND starts_on=?",p.kind(),p.start());
        }
    }
    @Transactional(readOnly=true)
    public List<Medal> forMember(long id) {
        if(!storage.rankingsEnabled())return List.of();
        if(storage.hasRanks())return issuedForMember(id);
        if(!preview)return List.of();
        var medals=new ArrayList<Medal>();
        for(var p:closedPeriods(LocalDate.now(SEOUL)))for(var w:podium(p))if(w.member()==id)
            medals.add(new Medal(p.kind(),p.start(),p.endExclusive().minusDays(1),w.place()));
        medals.sort(Comparator.comparing(Medal::achievedOn).reversed().thenComparing(Medal::kind));
        return medals;
    }
    public List<Medal> issuedForMember(long id) {
        if (!storage.rankingsEnabled() || !storage.hasRanks()) return List.of();
        return db.query("""
                SELECT h.kind,h.starts_on,p.ends_before,h.place_no FROM rank_honor h
                JOIN rank_honor_period p ON p.kind=h.kind AND p.starts_on=h.starts_on
                WHERE h.user_id=? AND p.settled=TRUE ORDER BY p.ends_before DESC,h.kind
                """,(rs,n)->new Medal(rs.getString(1),rs.getDate(2).toLocalDate(),rs.getDate(3).toLocalDate().minusDays(1),rs.getInt(4)),id);
    }
}

@RestController
class RankHonorController {
    private final RankHonors honors;
    RankHonorController(RankHonors honors){this.honors=honors;}
    @GetMapping("/user/activity/rank-honors")
    public BaseResponse<?> mine(@AuthenticationPrincipal AuthMember actor){return new BaseResponse<>(honors.forMember(actor.getId()));}
}
