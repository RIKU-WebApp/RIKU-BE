package RIKU.server.domains.community;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Manual schema installation is independent of the switch that allows issuing awards. */
@Component
@org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
public class BadgeStorage {
    private final JdbcTemplate db;
    private final boolean enabled, rankingsEnabled;
    private final Set<String> tables;

    public BadgeStorage(JdbcTemplate db, @Value("${riku.badges.auto-grant.enabled:false}") boolean enabled,
                        @Value("${riku.badges.rankings-enabled:false}") boolean rankingsEnabled) {
        this.db=db;
        this.enabled=enabled;
        this.rankingsEnabled=rankingsEnabled;
        tables=db.execute((ConnectionCallback<Set<String>>) c->{
            var names=new HashSet<String>();
            var metadata=c.getMetaData();
            var schema="H2".equals(metadata.getDatabaseProductName()) ? c.getSchema() : null;
            try(var rows=metadata.getTables(c.getCatalog(),schema,"%",new String[]{"TABLE","BASE TABLE"})){
                while(rows.next()) names.add(rows.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
            return Set.copyOf(names);
        });
        if(enabled){
            if(!installed() || !hasRanks() || !hasPeaks() || !hasCalendar() || !tables.contains("badge_job_lock"))
                throw new IllegalStateException("Install deploy/sql/automatic-badge-grant.mysql.sql before enabling automatic badges.");
            db.queryForObject("SELECT job_name FROM badge_job_lock WHERE job_name='catalog'",String.class);
            for(String projection: new String[]{"user_id,badge_code,badge_name,badge_rule,badge_source,granted_at FROM automatic_badge_grant",
                    "kind,starts_on,ends_before,settled FROM rank_honor_period",
                    "kind,starts_on,user_id,place_no FROM rank_honor","user_id,personal_km,had_certificate FROM badge_record_peak"})
                db.queryForList("SELECT "+projection+" WHERE 1=0");
        }
    }
    boolean rankingsEnabled(){return rankingsEnabled;}
    boolean enabled(){return enabled;}
    boolean installed(){return tables.contains("automatic_badge_grant");}
    boolean hasRanks(){return tables.containsAll(Set.of("rank_honor","rank_honor_period"));}
    boolean hasCalendar(){return tables.contains("badge_semester_start");}
    boolean hasPeaks(){return tables.contains("badge_record_peak");}

    void lockRecordOwner(long id){
        if(hasPeaks()) db.queryForObject("SELECT user_id FROM users WHERE user_id=? FOR UPDATE",Long.class,id);
    }
    // Called before a personal record is deleted, after serializing this member's creates/deletes.
    void rememberPersonalKm(long id){
        if(!hasPeaks())return;
        var km=db.queryForObject("SELECT COALESCE(SUM(distance_km),0) FROM personal_run_record WHERE user_id=?",java.math.BigDecimal.class,id);
        db.update("""
            INSERT INTO badge_record_peak(user_id,personal_km) VALUES(?,?)
            ON DUPLICATE KEY UPDATE personal_km=GREATEST(personal_km,?)
            """,id,km,km);
    }
    // Called only after deleting a real certificate owned by the member, in the same transaction.
    void rememberCertificate(long id){
        if(hasPeaks())db.update("""
            INSERT INTO badge_record_peak(user_id,had_certificate) VALUES(?,TRUE)
            ON DUPLICATE KEY UPDATE had_certificate=TRUE
            """,id);
    }
}
