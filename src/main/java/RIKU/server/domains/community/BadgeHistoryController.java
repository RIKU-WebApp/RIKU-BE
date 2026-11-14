package RIKU.server.domains.community;

import RIKU.server.global.response.BaseResponse;
import RIKU.server.security.AuthMember;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.*;
import java.util.*;

@RestController
class BadgeHistoryController {
    private final JdbcTemplate db;
    private final BadgeStorage storage;
    private final BadgeAwardRepository manual;
    static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    record Dates(LocalDate issuedOn, LocalDate activityOn) {}

    BadgeHistoryController(JdbcTemplate db, BadgeStorage storage, BadgeAwardRepository manual) {
        this.db=db; this.storage=storage; this.manual=manual;
    }

    @GetMapping("/user/activity/badge-dates")
    @Transactional(readOnly=true)
    public BaseResponse<Map<String,Dates>> mine(@AuthenticationPrincipal AuthMember actor) {
        var dates = new LinkedHashMap<String,Dates>();
        if (storage.installed()) db.query("""
                SELECT g.badge_code,FLOOR(UNIX_TIMESTAMP(g.granted_at)),h.achieved_on
                FROM automatic_badge_grant g LEFT JOIN badge_activity_date h USING(user_id,badge_code)
                WHERE g.user_id=?
                """ + (storage.rankingsEnabled() ? "" : " AND g.badge_code NOT LIKE 'monthlyRank%'"),
                rs->{ dates.put(rs.getString(1),new Dates(Instant.ofEpochSecond(rs.getLong(2)).atZone(SEOUL).toLocalDate(),
                        rs.getObject(3, LocalDate.class))); },actor.getId());
        for (var a : manual.findByUserIdOrderByIdAsc(actor.getId())) {
            if (a.getAwardedAt()!=null) dates.put("AWARD_"+a.getId(),new Dates(a.getAwardedAt().atZone(SEOUL).toLocalDate(),null));
        }
        return new BaseResponse<>(dates);
    }
}
