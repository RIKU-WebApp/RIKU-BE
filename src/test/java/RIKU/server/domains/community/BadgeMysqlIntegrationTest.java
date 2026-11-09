package RIKU.server.domains.community;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

/** Set RIKU_BADGE_TEST_PORT/PASSWORD for a disposable local MySQL 8, with an EMPTY riku_badge_test schema. */
@EnabledIfEnvironmentVariable(named="RIKU_BADGE_TEST_PORT",matches="[0-9]+")
class BadgeMysqlIntegrationTest {
    @Test void backfillAllCatalogTiersResumeAfterPauseAndKeepClosedPodiums() throws Exception {
        var schema=System.getenv().getOrDefault("RIKU_BADGE_TEST_DB","riku_badge_test");
        assertTrue(schema.matches("riku_badge_test(_[0-9]+)?"));
        var ds=new DriverManagerDataSource("jdbc:mysql://127.0.0.1:"+System.getenv("RIKU_BADGE_TEST_PORT")+
                "/"+schema+"?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&allowPublicKeyRetrieval=true&useSSL=false","badge_test",System.getenv("RIKU_BADGE_TEST_PASSWORD"));
        var db=new JdbcTemplate(ds);
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()",Integer.class),
                "Refusing to run fixtures against a nonempty database");
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var mapper=new ObjectMapper();
        assertFalse(new BadgeStorage(db,false,true).installed());
        assertThrows(IllegalStateException.class,()->new BadgeStorage(db,true,true));
        for(String ddl:List.of("users(user_id BIGINT PRIMARY KEY,status VARCHAR(16))",
                "post(post_id BIGINT PRIMARY KEY,user_id BIGINT,post_type VARCHAR(16),date DATETIME,status VARCHAR(16),post_status VARCHAR(16))",
                "participant(post_id BIGINT,user_id BIGINT,participant_group VARCHAR(4),participant_status VARCHAR(16))",
                "pacer(post_id BIGINT,user_id BIGINT,pacer_group VARCHAR(4),distance VARCHAR(30))","run_recruitment_group(post_id BIGINT,group_name VARCHAR(4),distance_km DECIMAL(7,2))",
                "user_point(user_id BIGINT,point INT,point_type VARCHAR(24),created_at DATETIME)",
                "run_certificate(user_id BIGINT,date DATE)","personal_run_record(user_id BIGINT,distance_km DECIMAL(7,2))"))
            db.execute("CREATE TABLE "+ddl);
        var migration=new ResourceDatabasePopulator(new FileSystemResource("deploy/sql/automatic-badge-grant.mysql.sql"));
        migration.execute(ds); migration.execute(ds);
        var off=new BadgeStorage(db,false,true);
        var on=new BadgeStorage(db,true,true);
        var paused=new AutomaticBadges(db,mapper,off);
        var engine=new AutomaticBadges(db,mapper,on);
        var env=new MockEnvironment();
        var ranks=new RankHonors(db,env,LocalDate.of(2026,3,2),on);
        var pausedRanks=new RankHonors(db,env,LocalDate.of(2026,3,2),off);
        tx.executeWithoutResult(ignored->{
            for(int i=100;i<=121;i++)db.update("INSERT INTO users VALUES(?,?)",i,i==121?"INACTIVE":"ACTIVE");
            for(int i=0;i<100;i++){
                var d=LocalDate.of(2024,1,6).plusWeeks(i);
                db.update("INSERT INTO post VALUES(?,100,?,?,'ACTIVE','CLOSED')",i,List.of("REGULAR","FLASH","TRAINING","EVENT").get(i%4),d);
                db.update("INSERT INTO participant VALUES(?,100,'A','ATTENDED'),(?,101,'A','ATTENDED')",i,i);
                db.update("INSERT INTO pacer VALUES(?,100,'A','10')",i);
                db.update("INSERT INTO run_recruitment_group VALUES(?,'A',10)",i);
            }
            // A matching legacy group is evidence only when every pacer agrees on a numeric distance.
            db.update("INSERT INTO post VALUES(105,100,'FLASH','2025-01-01','ACTIVE','CLOSED')");
            db.update("INSERT INTO post VALUES(106,100,'FLASH','2025-01-01','ACTIVE','CLOSED')");
            db.update("INSERT INTO pacer VALUES(105,101,'A','10 km'),(105,102,'A','10.0'),(106,101,'A','30'),(106,102,'A','15'),(106,103,'B','50')");
            db.update("INSERT INTO participant VALUES(105,119,'A','ATTENDED'),(106,119,'A','ATTENDED'),(106,119,NULL,'ATTENDED')");
            // Corrupt historical duplicate must not count the same outing twice, even across groups.
            db.update("INSERT INTO participant VALUES(0,101,'B','ATTENDED')");
            db.update("INSERT INTO run_recruitment_group VALUES(0,'B',9)");
            db.update("INSERT INTO personal_run_record VALUES(100,1000),(121,10)");
            off.lockRecordOwner(100); off.rememberPersonalKm(100);
            db.update("DELETE FROM personal_run_record WHERE user_id=100");
            off.rememberCertificate(100);
            for(int i=0;i<250;i++)
                db.update("INSERT INTO user_point VALUES(100,1,'ADD_ATTENDANCE',?)",LocalDate.of(2024,1,1).plusDays(i));
            for(int i=100;i<120;i++)db.update("INSERT INTO user_point VALUES(?,?,'ADD_ATTENDANCE','2025-10-15')",i,i==100?1000:1);
            paused.reconcile(); pausedRanks.settle();
        });
        var actor = org.mockito.Mockito.mock(RIKU.server.security.AuthMember.class);
        org.mockito.Mockito.when(actor.getId()).thenReturn(100L);
        var progress = new BadgeProgressController(db,on).progress(actor).getResult();
        assertEquals(db.queryForObject("SELECT COUNT(DISTINCT DATE(TIMESTAMPADD(HOUR,9,created_at))) FROM user_point WHERE user_id=100 AND point_type='ADD_ATTENDANCE'", Integer.class), progress.checkins());
        assertEquals(db.queryForObject("SELECT COUNT(DISTINCT date) FROM run_certificate WHERE user_id=100", Integer.class), progress.certificateDays());
        assertEquals(db.queryForObject("SELECT COUNT(*) FROM post p WHERE p.user_id=100 AND p.status='ACTIVE' AND p.post_status='CLOSED' AND EXISTS(SELECT 1 FROM participant a WHERE a.post_id=p.post_id AND a.user_id<>100 AND a.participant_status='ATTENDED')", Integer.class), progress.hosted());
        tx.executeWithoutResult(status -> {
            assertDoesNotThrow(() -> compareMemberMonthAfterCorrections(db));
            status.setRollbackOnly();
        });
        assertTrue(paused.grants(100).isEmpty());
        assertTrue(pausedRanks.forMember(100).isEmpty());
        var activityOnly=new BadgeStorage(db,true,false);
        var firstRelease=new AutomaticBadges(db,mapper,activityOnly);
        var hiddenRanks=new RankHonors(db,env,LocalDate.of(2026,3,2),activityOnly);
        // Member 102 has enough total visits for both platinum tiers, but lacks an event and a winter visit.
        tx.executeWithoutResult(status -> {
            String[] types = {"REGULAR","REGULAR","REGULAR","FLASH","FLASH","FLASH","TRAINING","TRAINING","EVENT","REGULAR"};
            int[] months = {3,3,3,6,6,6,9,9,9,12};
            for (int i=0;i<types.length;i++) {
                db.update("INSERT INTO post VALUES(?,100,?,?,'ACTIVE','CLOSED')",200+i,types[i],LocalDate.of(2025,months[i],1));
                db.update("INSERT INTO participant VALUES(?,102,'A','ATTENDED')",200+i);
            }
            firstRelease.reconcile();
            var before = firstRelease.grants(102).stream().map(CommunityService.Badge::code).toList();
            assertTrue(before.containsAll(List.of("kinds_4","fourSeasons_4")));
            assertFalse(before.contains("kinds_5")); assertFalse(before.contains("fourSeasons_5"));
            db.update("INSERT INTO post VALUES(210,100,'EVENT','2026-01-05','ACTIVE','CLOSED')");
            db.update("INSERT INTO participant VALUES(210,102,'A','ATTENDED')");
            firstRelease.reconcile();
            var after = firstRelease.grants(102).stream().map(CommunityService.Badge::code).toList();
            assertTrue(after.containsAll(List.of("kinds_5","fourSeasons_5")));
            assertFalse(after.contains("kinds_6")); assertFalse(after.contains("fourSeasons_6"));
            status.setRollbackOnly();
        });
        tx.executeWithoutResult(s->{firstRelease.reconcile(); hiddenRanks.settle();});
        assertEquals(78,firstRelease.grants(100).size());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM automatic_badge_grant WHERE badge_code LIKE 'monthlyRank%'",Integer.class));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM rank_honor_period",Integer.class));
        tx.executeWithoutResult(s->{engine.reconcile(); ranks.settle();});
        var expected=new HashSet<String>();
        for(var family:mapper.readTree(new ClassPathResource("badges/series.json").getInputStream())){
            for(var goal:family.path("levels"))expected.add(family.path("prefix").asText(family.path("id").asText())+"_"+goal.asInt());
        }
        assertEquals(81,expected.size());
        assertEquals(expected,new HashSet<>(paused.grants(100).stream().map(CommunityService.Badge::code).toList()));
        assertEquals(10,engine.verifiedKm(119).intValueExact());
        assertTrue(paused.grants(119).stream().anyMatch(b->b.code().equals("CLUB_10")));
        assertTrue(paused.grants(120).isEmpty());
        assertTrue(paused.grants(121).isEmpty());
        assertEquals(78,firstRelease.grants(100).size());
        assertTrue(hiddenRanks.forMember(100).isEmpty());
        assertTrue(hiddenRanks.issuedForMember(100).isEmpty());
        var medals=pausedRanks.forMember(100);
        assertTrue(medals.stream().anyMatch(m->m.kind().equals("MONTH") && m.place()==1));
        assertTrue(medals.stream().anyMatch(m->m.kind().equals("SEMESTER") && m.place()==1));
        var members=org.mockito.Mockito.mock(RIKU.server.domains.user.repository.UserRepository.class);
        var user=RIKU.server.domains.user.entity.User.create("private-id","hash","공개 이름","private school","private major","private phone");
        user.updateStatus(RIKU.server.global.entity.BaseStatus.ACTIVE);
        org.mockito.Mockito.when(members.findById(100L)).thenReturn(java.util.Optional.of(user));
        var community=new CommunityService(members,null,null,null,null,null,pausedRanks,paused,off);
        var preview=community.badgePreview(100L);
        assertEquals(List.of("visits_50","REGULAR_25","FLASH_25"),preview.badges().stream().map(CommunityService.Badge::code).toList());
        assertEquals(medals.size(),preview.medalCount());
        assertEquals(medals.stream().limit(3).toList(),preview.medals());
        var profile=community.profile(100L);
        assertEquals("공개 이름",profile.name()); assertEquals(paused.grants(100),profile.badges()); assertEquals(medals,profile.medals());
        user.updateStatus(RIKU.server.global.entity.BaseStatus.INACTIVE);
        assertEquals(404,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->community.profile(100L)).getStatusCode().value());
        var stamp=db.queryForObject("SELECT MIN(granted_at) FROM automatic_badge_grant WHERE user_id=100",java.sql.Timestamp.class);
        db.update("UPDATE users SET status='ACTIVE' WHERE user_id=121");
        paused.reconcile(); assertTrue(paused.grants(121).isEmpty());
        // Simulate a transaction failure after all inserts; retry from two workers.
        tx.executeWithoutResult(s->{engine.reconcile();s.setRollbackOnly();});
        assertTrue(paused.grants(121).isEmpty());
        Runnable run=()->tx.executeWithoutResult(s->engine.reconcile());
        CompletableFuture.allOf(CompletableFuture.runAsync(run),CompletableFuture.runAsync(run)).join();
        assertEquals(List.of("PERSONAL_10"),paused.grants(121).stream().map(CommunityService.Badge::code).toList());
        assertEquals(stamp,db.queryForObject("SELECT MIN(granted_at) FROM automatic_badge_grant WHERE user_id=100",java.sql.Timestamp.class));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM automatic_badge_grant WHERE user_id=120",Integer.class));
        // A previously undersized month can qualify on a late import; an already-issued podium is immutable.
        for(int i=100;i<120;i++)db.update("INSERT INTO user_point VALUES(?,?,'ADD_ATTENDANCE','2025-09-10')",i,i==100?500:1);
        tx.executeWithoutResult(s->ranks.settle());
        assertTrue(pausedRanks.forMember(100).stream().anyMatch(m->m.kind().equals("MONTH") && m.start().equals(LocalDate.of(2025,9,1))));
        assertTrue(pausedRanks.forMember(100).containsAll(medals));
        // A configured start observed during OFF survives later rollovers even if absent from the static file.
        new RankHonors(db,env,LocalDate.of(2026,4,1),off);
        var nextTerm=new RankHonors(db,env,LocalDate.of(2026,7,1),on);
        assertTrue(nextTerm.closedPeriods(LocalDate.of(2026,7,1)).contains(
                new RankHonors.Period("SEMESTER",LocalDate.of(2026,4,1),LocalDate.of(2026,7,1))));
        // Production retained the obsolete points-season date one day after the official start.
        var autumn = new RankHonors(db,env,LocalDate.of(2026,9,1),on);
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM badge_semester_start WHERE starts_on='2026-09-01'",Integer.class));
        db.update("INSERT INTO badge_semester_start VALUES ('2026-09-01')");
        tx.executeWithoutResult(s -> autumn.settleClosedPeriods(LocalDate.of(2026,11,7)));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM rank_honor_period WHERE kind='SEMESTER' AND starts_on='2026-08-31'",Integer.class));
        var spring = new RankHonors(db,env,LocalDate.of(2027,3,2),on);
        assertTrue(spring.closedPeriods(LocalDate.of(2027,3,2)).contains(
                new RankHonors.Period("SEMESTER",LocalDate.of(2026,8,31),LocalDate.of(2027,3,2))));
    }

    private void compareMemberMonthAfterCorrections(JdbcTemplate db) throws Exception {
        String oldSql = "SELECT MIN(best_rank) FROM (" + new ClassPathResource("badges/monthly-ranks.sql")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8).replace("__MONTH_KEY__", "?")
                + ") ranks WHERE user_id=?";
        String memberSql = new ClassPathResource("badges/member-monthly-rank.sql")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        // January through June 2023 KST. Only June has points for the test member.
        var cutoff = java.time.LocalDateTime.of(2023, 6, 30, 15, 0);
        for (int i = 900; i <= 920; i++) db.update("INSERT INTO users VALUES (?,'ACTIVE')", i);
        for (int month = 1; month <= 6; month++) {
            for (int i = 901; i <= 920; i++) {
                db.update("INSERT INTO user_point VALUES(?,10,'TEST',?)", i, LocalDate.of(2023,month,2));
            }
        }
        db.update("INSERT INTO user_point VALUES(900,10,'TEST','2023-05-31 15:00:00')," +
                "(900,999,'TEST','2023-06-30 15:00:00'),(901,1,'BONUS','2023-06-01')");
        java.util.function.Consumer<Integer> check = expected -> {
            assertEquals(expected, db.queryForObject(oldSql, Integer.class, 202307, 900));
            assertEquals(expected, db.queryForObject(memberSql, Integer.class, cutoff, 900));
        };
        check.accept(2);
        // Use one transaction/connection to measure actual MySQL handler reads, including temp tables.
        for (int member : List.of(900, 901)) {
            for (String sql : List.of(oldSql, memberSql)) {
                boolean legacy = sql.equals(oldSql);
                Object[] params = legacy ? new Object[]{202307,member} : new Object[]{cutoff,member};
                long before = handlerReads(db);
                Integer rank = db.queryForObject(sql, Integer.class, params);
                System.out.printf("MONTH_IO %s member=%d rank=%d handlerReads=%d%n",
                        legacy ? "old" : "new", member, rank, handlerReads(db) - before);
                System.out.println("MONTH_PLAN_" + (legacy ? "OLD" : "NEW") + "_" + member + " "
                        + db.queryForObject("EXPLAIN ANALYZE " + sql, String.class, params));
            }
        }
        db.update("DELETE FROM user_point WHERE user_id=901 AND point_type='BONUS'");
        check.accept(1); // tie for first
        db.update("UPDATE user_point SET point=-1 WHERE user_id=900 AND created_at='2023-05-31 15:00:00'");
        check.accept(null);
        db.update("UPDATE user_point SET point=9 WHERE user_id=900 AND created_at='2023-05-31 15:00:00'");
        check.accept(21);
        db.update("UPDATE users SET status='INACTIVE' WHERE user_id IN (919,920)");
        check.accept(null); // only 19 eligible competitors
        db.update("UPDATE users SET status='ACTIVE' WHERE user_id=919");
        check.accept(20);
        db.update("UPDATE users SET status='INACTIVE' WHERE user_id=900");
        check.accept(null);
    }
    private long handlerReads(JdbcTemplate db) {
        return db.query("SHOW SESSION STATUS LIKE 'Handler_read_%'", rs -> {
            long total = 0;
            while (rs.next()) total += rs.getLong(2);
            return total;
        });
    }
}
