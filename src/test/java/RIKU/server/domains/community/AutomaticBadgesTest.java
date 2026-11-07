package RIKU.server.domains.community;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticBadgesTest {
    private BadgeStorage enabledStorage(){
        var storage=org.mockito.Mockito.mock(BadgeStorage.class);
        org.mockito.Mockito.when(storage.enabled()).thenReturn(true);
        org.mockito.Mockito.when(storage.installed()).thenReturn(true);
        org.mockito.Mockito.when(storage.hasRanks()).thenReturn(true);
        org.mockito.Mockito.when(storage.rankingsEnabled()).thenReturn(true);
        return storage;
    }
    @Test void podiumsKeepSeparatePeriodMedalsAndFreezeTheirOriginalPlaces() throws Exception {
        var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:podium;MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        db.execute("CREATE TABLE users(user_id bigint primary key,status varchar(20))");
        db.execute("CREATE TABLE user_point(user_id bigint,point int,created_at timestamp)");
        db.execute("CREATE TABLE rank_honor_period(kind varchar(12),starts_on date,ends_before date,settled boolean,primary key(kind,starts_on))");
        db.execute("CREATE TABLE rank_honor(kind varchar(12),starts_on date,user_id bigint,place_no int,primary key(kind,starts_on,user_id))");
        for(int i=100;i<120;i++) {
            db.update("INSERT INTO users VALUES (?,'ACTIVE')",i);
            db.update("INSERT INTO user_point VALUES (?,?,'2026-02-10 10:00:00')",i,i<102?4:1);
            db.update("INSERT INTO user_point VALUES (?,?,'2026-03-20 10:00:00')",i,i==100?1:2);
        }
        var service=new RankHonors(db,new org.springframework.mock.env.MockEnvironment(),java.time.LocalDate.of(2026,3,2),enabledStorage());
        for(int i=100;i<120;i++)
            db.update("INSERT INTO user_point VALUES (?,100,'2026-09-10 08:00:00')",i);
        var periods=service.closedPeriods(java.time.LocalDate.of(2027,3,10));
        assertTrue(periods.contains(new RankHonors.Period("SEMESTER",java.time.LocalDate.of(2026,3,2),java.time.LocalDate.of(2026,8,31))));
        assertFalse(periods.stream().anyMatch(p->!p.start().isBefore(java.time.LocalDate.of(2026,8,31))));
        service.settle();
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM rank_honor WHERE starts_on>='2026-09-01'",Integer.class));
        var original=service.forMember(100);
        assertEquals(2,original.size());
        assertTrue(original.stream().anyMatch(m->m.kind().equals("MONTH") && m.achievedOn().toString().equals("2026-02-28") && m.place()==1));
        assertTrue(original.stream().anyMatch(m->m.kind().equals("SEMESTER") && m.achievedOn().toString().equals("2026-03-01") && m.place()==1));
        assertEquals(2,service.forMember(101).stream().filter(m->m.place()==1 && m.start().isBefore(java.time.LocalDate.of(2026,3,1))).count());
        db.update("INSERT INTO user_point VALUES (119,999,'2026-02-14 00:00:00')");
        service.settle();
        assertEquals(original,service.forMember(100));
    }
    @Test void monthlyRanksUseClosedKoreanMonthsPositiveScoresAndCompetitionTies() throws Exception {
        var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:monthly_badge;MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        db.execute("CREATE TABLE users(user_id bigint primary key,status varchar(20))");
        db.execute("CREATE TABLE user_point(user_id bigint,point int,created_at timestamp)");
        for(int i=100;i<=120;i++) {
            db.update("INSERT INTO users VALUES (?,'ACTIVE')",i);
            // 20 positive October scores; two at the top, followed by joint third place.
            db.update("INSERT INTO user_point VALUES (?,?,'2026-09-30 15:00:00')",i,i==120?0:i<=101?5:1);
            db.update("INSERT INTO user_point VALUES (?,1,'2026-10-31 15:00:00')",i);
        }
        // A win in a month with only one competitor must not improve October's third place.
        db.update("INSERT INTO user_point VALUES (103,9,'2026-09-03 00:00:00')");
        var sql=new org.springframework.core.io.ClassPathResource("badges/monthly-ranks.sql")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8).replace("__MONTH_KEY__","?");
        var ranks=db.query(sql,(rs,n)->java.util.Map.entry(rs.getInt(1),rs.getInt(2)),202611);
        assertEquals(20,ranks.size());
        assertTrue(ranks.contains(java.util.Map.entry(101,1)));
        assertTrue(ranks.contains(java.util.Map.entry(103,3)));
        assertFalse(ranks.stream().anyMatch(r->r.getKey()==120));
    }
    @Test void repeatedOrOverlappingAwardsKeepFirstTimestampAndSkipInactiveAccounts() throws Exception {
        var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:auto_badge;MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        db.execute("CREATE TABLE users(user_id bigint primary key,status varchar(20))");
        db.execute("CREATE TABLE automatic_badge_grant(user_id bigint,badge_code varchar(64),badge_name varchar(120),badge_rule varchar(500),badge_source varchar(16),granted_at timestamp default current_timestamp, primary key(user_id,badge_code))");
        db.update("INSERT INTO users VALUES (1,'ACTIVE'),(2,'INACTIVE')");
        var engine=new AutomaticBadges(db,new ObjectMapper(),enabledStorage());
        var candidates=List.of(new AutomaticBadges.Grant(1,"visits_1"),new AutomaticBadges.Grant(2,"visits_1"));
        engine.persist(candidates);
        var first=db.queryForObject("SELECT granted_at FROM automatic_badge_grant",java.sql.Timestamp.class);
        engine.persist(candidates);
        engine.persist(List.of(new AutomaticBadges.Grant(1,"visits_5")));
        assertEquals(first,db.queryForObject("SELECT granted_at FROM automatic_badge_grant WHERE badge_code='visits_1'",java.sql.Timestamp.class));
        assertEquals(List.of("visits_1","visits_5"),engine.grants(1).stream().map(CommunityService.Badge::code).toList());
        assertTrue(engine.grants(2).isEmpty());
        db.update("INSERT INTO automatic_badge_grant(user_id,badge_code,badge_name,badge_rule,badge_source) VALUES (1,'retired_1','지난 배지','기존 조건','CLUB')");
        assertTrue(engine.grants(1).contains(new CommunityService.Badge("retired_1","지난 배지","기존 조건","CLUB")));
    }
}
