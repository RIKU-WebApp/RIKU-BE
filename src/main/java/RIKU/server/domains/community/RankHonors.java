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
    private final boolean podiumsEnabled;
    private static final LocalDate LAST_RELEASED_TERM = LocalDate.of(2026,8,31);
    // The old points-season setting was September 1. The official term began August 31.
    private static final LocalDate LEGACY_AUTUMN_START = LocalDate.of(2026,9,1);
    // Monthly editions run through February. Semester closing dates come from the calendar.
    private static final LocalDate LAST_RELEASED_MONTH = LocalDate.of(2027,2,1);
    public RankHonors(JdbcTemplate db, Environment env, @Value("${season.start-date}") LocalDate currentStart,
                      BadgeStorage storage) throws IOException {
        this.db=db;
        podiumSql=new ClassPathResource("badges/rank-podium.sql").getContentAsString(StandardCharsets.UTF_8);
        this.storage=storage;
        this.podiumsEnabled=env.getProperty("riku.badges.podiums-enabled",Boolean.class,storage.rankingsEnabled());
        this.preview=env.acceptsProfiles(Profiles.of("preview"));
        var history=new ClassPathResource("badges/semester-starts.txt").getContentAsString(StandardCharsets.UTF_8);
        var starts=new TreeSet<LocalDate>();
        history.lines().filter(s->!s.isBlank()).map(LocalDate::parse).forEach(starts::add);
        if (!currentStart.equals(LEGACY_AUTUMN_START)) starts.add(currentStart);
        if(storage.hasCalendar()) for(var start:starts)db.update("""
            INSERT INTO badge_semester_start(starts_on) VALUES(?)
            ON DUPLICATE KEY UPDATE starts_on=badge_semester_start.starts_on
            """,start);
        terms=storage.hasCalendar()
                ? db.query("SELECT starts_on FROM badge_semester_start WHERE starts_on<>? ORDER BY starts_on",
                        (rs,n)->rs.getDate(1).toLocalDate(),LEGACY_AUTUMN_START)
                : List.copyOf(starts);
    }
    record Period(String kind, LocalDate start, LocalDate endExclusive) {}
    record Winner(long member, int place) {}
    public record Medal(String kind, LocalDate start, LocalDate achievedOn, int place) {}

    // Compare places only within the current and preceding terms. Monthly editions
    // belong to their calendar month (March 2026 is spring, August is still spring).
    private record Window(LocalDate term, LocalDate month) {
        Comparator<Medal> order() {
            return Comparator.comparing((Medal m)->!m.kind().equals("SEMESTER"))
                    .thenComparingInt(m->m.start().isBefore(m.kind().equals("SEMESTER") ? term : month) ? 4 : m.place())
                    .thenComparing(Medal::achievedOn,Comparator.reverseOrder())
                    .thenComparing(Medal::start,Comparator.reverseOrder())
                    .thenComparingInt(Medal::place);
        }
    }
    private Window displayWindow(LocalDate today) {
        var begun=terms.stream().filter(d->!d.isAfter(today)).toList();
        var term=begun.isEmpty() ? terms.get(0) : begun.get(Math.max(0,begun.size()-2));
        var month=term.withDayOfMonth(1);
        if(term.getDayOfMonth()>15)month=month.plusMonths(1);
        return new Window(term,month);
    }

    List<Period> closedPeriods(LocalDate today) {
        var periods=new ArrayList<Period>();
        LocalDateTime first=db.queryForObject("SELECT MIN(created_at) FROM user_point", LocalDateTime.class);
        if(first!=null){
            var month=YearMonth.from(first.plusHours(9));
            while(month.isBefore(YearMonth.from(today)) && !month.isAfter(YearMonth.from(LAST_RELEASED_MONTH))){
                periods.add(new Period("MONTH",month.atDay(1),month.plusMonths(1).atDay(1)));
                month=month.plusMonths(1);
            }
        }
        var calendar=storage.hasCalendar()
                ? db.query("SELECT starts_on FROM badge_semester_start WHERE starts_on<>? ORDER BY starts_on",
                        (rs,n)->rs.getDate(1).toLocalDate(),LEGACY_AUTUMN_START) : terms;
        for(int i=1;i<calendar.size();i++) {
            if(!calendar.get(i).isAfter(today) && !calendar.get(i-1).isAfter(LAST_RELEASED_TERM)) periods.add(new Period("SEMESTER",calendar.get(i-1),calendar.get(i)));
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
        settleClosedPeriods(LocalDate.now(SEOUL));
    }
    void settleClosedPeriods(LocalDate today) {
        if(!storage.enabled() || !podiumsEnabled)return;
        var done=new HashSet<>(db.query("SELECT kind,starts_on FROM rank_honor_period WHERE settled=TRUE",
                (rs,n)->rs.getString(1)+rs.getDate(2)));
        for(var p:closedPeriods(today)) {
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
        if(!podiumsEnabled)return List.of();
        if(storage.hasRanks())return issuedForMember(id);
        if(!preview)return List.of();
        var medals=new ArrayList<Medal>();
        for(var p:closedPeriods(LocalDate.now(SEOUL)))for(var w:podium(p))if(w.member()==id)
            medals.add(new Medal(p.kind(),p.start(),p.endExclusive().minusDays(1),w.place()));
        medals.sort(displayWindow(LocalDate.now(SEOUL)).order());
        return medals;
    }
    private static final String VISIBLE_HONORS = """
                FROM rank_honor h JOIN rank_honor_period p ON p.kind=h.kind AND p.starts_on=h.starts_on
                WHERE h.user_id=? AND p.settled=TRUE
                AND ((h.kind='MONTH' AND h.starts_on<=?) OR (h.kind='SEMESTER' AND h.starts_on<=?))
                """;

    private Medal readMedal(java.sql.ResultSet row) throws java.sql.SQLException {
        return new Medal(row.getString("kind"), row.getDate("starts_on").toLocalDate(),
                row.getDate("ends_before").toLocalDate().minusDays(1), row.getInt("place_no"));
    }
    private static final String DISPLAY_ORDER = """
            ORDER BY CASE WHEN h.kind='SEMESTER' THEN 0 ELSE 1 END,
            CASE WHEN (h.kind='SEMESTER' AND h.starts_on>=?) OR (h.kind='MONTH' AND h.starts_on>=?)
                 THEN h.place_no ELSE 4 END, p.ends_before DESC,h.starts_on DESC,h.place_no
            """;
    public List<Medal> issuedForMember(long id) {
        if (!podiumsEnabled || !storage.hasRanks()) return List.of();
        var w=displayWindow(LocalDate.now(SEOUL));
        return db.query("SELECT h.kind,h.starts_on,p.ends_before,h.place_no " + VISIBLE_HONORS
                + DISPLAY_ORDER, (rs,n)->readMedal(rs),id,LAST_RELEASED_MONTH,LAST_RELEASED_TERM,w.term(),w.month());
    }
    record OwnMedal(String kind, LocalDate start, LocalDate achievedOn, int place, LocalDate issuedOn) {
        OwnMedal(Medal m, LocalDate issued) {this(m.kind(),m.start(),m.achievedOn(),m.place(),issued);}
    }
    List<OwnMedal> own(long id) {
        if(!podiumsEnabled || !storage.hasRanks()) return forMember(id).stream().map(m->new OwnMedal(m,null)).toList();
        var w=displayWindow(LocalDate.now(SEOUL));
        return db.query("SELECT h.kind,h.starts_on,p.ends_before,h.place_no,UNIX_TIMESTAMP(h.issued_at) AS issued " + VISIBLE_HONORS
                + DISPLAY_ORDER, (rs,n)->new OwnMedal(readMedal(rs),java.time.Instant.ofEpochSecond(rs.getLong("issued"))
                .atZone(SEOUL).toLocalDate()),id,LAST_RELEASED_MONTH,LAST_RELEASED_TERM,w.term(),w.month());
    }
    public record Preview(List<Medal> medals, int total) {}
    Preview previewForMember(long id) {
        return previewForMember(id,LocalDate.now(SEOUL));
    }
    Preview previewForMember(long id, LocalDate today) {
        if (!podiumsEnabled) return new Preview(List.of(),0);
        var w=displayWindow(today);
        if (!storage.hasRanks()) {
            var medals = forMember(id);
            var first = medals.stream().sorted(w.order()).limit(3).toList();
            return new Preview(first,medals.size());
        }
        return db.query("SELECT h.kind,h.starts_on,p.ends_before,h.place_no,COUNT(*) OVER() AS total " + VISIBLE_HONORS
                + DISPLAY_ORDER + " LIMIT 3",
                rs -> {
                    var medals = new ArrayList<Medal>(3);
                    int total = 0;
                    while (rs.next()) {
                        total = rs.getInt("total");
                        medals.add(readMedal(rs));
                    }
                    return new Preview(medals,total);
                }, id,LAST_RELEASED_MONTH,LAST_RELEASED_TERM,w.term(),w.month());
    }
}

@RestController
class RankHonorController {
    private final RankHonors honors;
    RankHonorController(RankHonors honors){this.honors=honors;}
    @GetMapping("/user/activity/rank-honors")
    public BaseResponse<?> mine(@AuthenticationPrincipal AuthMember actor){return new BaseResponse<>(honors.own(actor.getId()));}
}
