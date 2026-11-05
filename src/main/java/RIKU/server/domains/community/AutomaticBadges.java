package RIKU.server.domains.community;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Opt-in: requires the separate automatic-grant table. Never changes manually awarded badges. */
@Service
public class AutomaticBadges {
    // Metrics implemented by eligibility.sql. Reject catalog edits that would silently issue nothing.
    private static final Set<String> METRICS = Set.of("visits","months","kinds","leads","weekends","streak",
            "REGULAR","FLASH","TRAINING","EVENT","hosted","verifiedKm","personalKm","monthlyRank","checkins","fourSeasons","certificateDays");
    private final JdbcTemplate db;
    private final String snapshot, catalog, memberDistance;
    private final BadgeStorage storage;
    private final Map<String, CommunityService.Badge> labels = new LinkedHashMap<>();

    public AutomaticBadges(JdbcTemplate db, ObjectMapper mapper, BadgeStorage storage) throws IOException {
        this.db = db;
        this.storage=storage;
        var monthly = new ClassPathResource("badges/monthly-ranks.sql").getContentAsString(StandardCharsets.UTF_8)
                .replace("__MONTH_KEY__", "EXTRACT(YEAR_MONTH FROM TIMESTAMPADD(HOUR,9,UTC_TIMESTAMP()))");
        String visits=new ClassPathResource("badges/verified-visits.sql").getContentAsString(StandardCharsets.UTF_8);
        memberDistance=visits.replace("__MEMBER_FILTER__","AND a.user_id=?")+" SELECT COALESCE(SUM(km),0) FROM visits";
        snapshot = new ClassPathResource("badges/eligibility.sql").getContentAsString(StandardCharsets.UTF_8)
                .replace("__VISITS__",visits.replace("__MEMBER_FILTER__",""))
                .replace("__MONTHLY_RANKS__", monthly)
                .replace("__RECORD_PEAKS__", new ClassPathResource("badges/record-peaks.sql").getContentAsString(StandardCharsets.UTF_8));
        catalog = new ClassPathResource("badges/series.json").getContentAsString(StandardCharsets.UTF_8);
        var families = new HashSet<String>();
        for (var f : mapper.readTree(catalog)) {
            String id = f.required("id").asText();
            String prefix = f.path("prefix").asText(id);
            if (!METRICS.contains(id) || !families.add(id) || !prefix.matches("[A-Za-z][A-Za-z0-9]{0,31}"))
                throw new IllegalArgumentException("Unknown or duplicate badge metric: "+id);
            var tiers = f.required("tiers");
            var levels=f.required("levels");
            boolean honor = (id.equals("verifiedKm") || id.equals("personalKm")) && tiers.size()==6 &&
                    tiers.get(5).asText().equals("명예") && levels.path(5).asInt()==1000;
            if(tiers.size()<1 || tiers.size()>(honor?6:5) || tiers.size()!=levels.size()) throw new IllegalArgumentException("Invalid tiers: "+id);
            int i=0;
            for(var target : f.required("levels")) {
                if (!target.isIntegralNumber() || !target.canConvertToInt() || target.asInt() <= (i==0 ? 0 : levels.get(i-1).asInt()))
                    throw new IllegalArgumentException("Invalid threshold: "+id);
                String code=prefix+"_"+target.intValue();
                var badge=new CommunityService.Badge(code,f.required("name").asText()+" "+tiers.get(i++).asText(),
                        f.required("rule").asText()+" — "+(f.has("conditions") ? f.required("conditions").required(i-1).asText() : target.asText()+f.required("unit").asText()),prefix.equals("PERSONAL")?"PERSONAL":"CLUB");
                if(labels.put(code,badge)!=null) throw new IllegalArgumentException("Duplicate badge "+code);
            }
        }
    }

    // The periodic reconciliation also catches late attendance corrections and members who never sign in.
    @Transactional
    public void reconcile() {
        if (!storage.enabled()) return;
        db.queryForObject("SELECT job_name FROM badge_job_lock WHERE job_name='catalog' FOR UPDATE",String.class);
        var due = db.query("""
                SELECT s.user_id,s.code FROM (
                """ + snapshot + """
                ) s LEFT JOIN automatic_badge_grant g ON g.user_id=s.user_id AND BINARY g.badge_code=BINARY s.code
                WHERE s.status='ACTIVE' AND s.code IS NOT NULL AND g.user_id IS NULL ORDER BY s.user_id,s.code
                """, (rs,n)->new Grant(rs.getLong(1),rs.getString(2)), catalog);
        persist(due);
        if (!due.isEmpty()) org.slf4j.LoggerFactory.getLogger(AutomaticBadges.class).info("Badge reconciliation: processed {} eligible grants", due.size());
    }

    record Grant(long userId,String code) {}

    void persist(List<Grant> due) {
        if(due.isEmpty()) return;
        db.batchUpdate("""
                INSERT INTO automatic_badge_grant(user_id,badge_code,badge_name,badge_rule,badge_source)
                SELECT u.user_id,?,?,?,? FROM users u WHERE u.user_id=? AND u.status='ACTIVE'
                ON DUPLICATE KEY UPDATE badge_code=automatic_badge_grant.badge_code
                """, due,250,(ps,g)->{
            if(!labels.containsKey(g.code())) throw new IllegalArgumentException("Unknown badge "+g.code());
            var label=labels.get(g.code());
            ps.setString(1,g.code()); ps.setString(2,label.name()); ps.setString(3,label.description());
            ps.setString(4,label.source()); ps.setLong(5,g.userId());
        });
    }

    java.math.BigDecimal verifiedKm(long id){
        return db.queryForObject(memberDistance,java.math.BigDecimal.class,id);
    }

    List<CommunityService.Badge> grants(long id) {
        if (!storage.installed()) return List.of();
        return db.query("SELECT badge_code,badge_name,badge_rule,badge_source FROM automatic_badge_grant WHERE user_id=? ORDER BY badge_code",
                (rs,n)->labels.getOrDefault(rs.getString(1),new CommunityService.Badge(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4))), id);
    }
}

@Configuration(proxyBeanMethods=false)
@EnableScheduling
@ConditionalOnProperty(name="riku.badges.auto-grant.enabled", havingValue="true")
class AutomaticBadgeSchedule {
    private final AutomaticBadges badges;
    AutomaticBadgeSchedule(AutomaticBadges badges){this.badges=badges;}
    @Scheduled(fixedDelayString="${riku.badges.auto-grant.delay-ms:300000}")
    public void reconcile(){badges.reconcile();}
}

@Service
@ConditionalOnProperty(name="riku.badges.auto-grant.enabled", havingValue="true")
class RankHonorSchedule {
    private final RankHonors honors;
    RankHonorSchedule(RankHonors honors){this.honors=honors;}
    @Scheduled(fixedDelayString="${riku.badges.auto-grant.delay-ms:300000}")
    public void reconcile(){honors.settle();}
}
