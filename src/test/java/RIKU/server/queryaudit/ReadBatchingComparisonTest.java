package RIKU.server.queryaudit;

import RIKU.server.domains.admin.dto.response.ReadUsersResponse;
import RIKU.server.domains.admin.service.AdminService;
import RIKU.server.domains.point.entity.*;
import RIKU.server.domains.point.ranking.RankingChanged;
import RIKU.server.domains.point.repository.UserPointRepository;
import RIKU.server.domains.run.participant.dto.request.ManualAttendParticipantRequest;
import RIKU.server.domains.run.participant.entity.*;
import RIKU.server.domains.run.participant.exception.ParticipantException;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.participant.service.ParticipantService;
import RIKU.server.domains.run.post.entity.*;
import RIKU.server.domains.run.post.repository.PostRepository;
import RIKU.server.domains.season.SeasonProvider;
import RIKU.server.domains.user.entity.*;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.global.entity.BaseStatus;
import RIKU.server.global.response.BaseResponseStatus;
import RIKU.server.queryaudit.baseline.*;
import RIKU.server.security.AuthMember;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Real old/new repository executions; fixture creation and result inspection are outside counters. */
@DataJpaTest(showSql=false) @ActiveProfiles("test") @RecordApplicationEvents
@Import({RIKU.server.domains.run.recruitment.RunWaitlist.class, AdminService.class, ParticipantService.class, LegacyAttendanceOperations.class})
class ReadBatchingComparisonTest {
    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired ParticipantRepository participants;
    @Autowired UserPointRepository points;
    @Autowired AdminService admin;
    @Autowired ParticipantService attendance;
    @Autowired LegacyAttendanceOperations oldAttendance;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired ApplicationEvents events;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @MockBean SeasonProvider season;
    @MockBean PasswordEncoder encoder;
    @MockBean RIKU.server.infra.storage.S3Uploader s3;
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    final LocalDateTime boundary = LocalDateTime.of(2025, 1, 1, 0, 0);
    @BeforeEach void setup() { when(season.seasonStartUtc()).thenReturn(boundary); }
    User user() {
        var u=User.create(UUID.randomUUID().toString(),"hash","회원","college","major",null);
        u.updateUserRole(UserRole.MEMBER); return users.save(u);
    }
    AuthMember auth(User u) { return AuthMember.of(u.getId(),u.getStudentId(),"",AuthorityUtils.createAuthorityList("ROLE_ADMIN"),BaseStatus.ACTIVE); }
    Post post(User u,PostType type,boolean closed) {
        var p=posts.save(Post.create(u,"run","park",LocalDateTime.now().plusDays(1),"body",null,type));
        if(closed) p.updatePostStatus(PostStatus.CLOSED); return p;
    }
    Participant participant(Post p,User u,boolean attended) {
        var a=Participant.createWithGroup(p,u,"A"); if(attended)a.attend(); return participants.save(a);
    }
    ManualAttendParticipantRequest request(User u,boolean attend) {
        var r=new ManualAttendParticipantRequest(); ReflectionTestUtils.setField(r,"userId",u.getId());
        ReflectionTestUtils.setField(r,"isAttend",attend); return r;
    }
    void stamp(String table,String key,Long id,LocalDateTime time) {
        em.flush(); em.createNativeQuery("update "+table+" set created_at=:time, updated_at=:time where "+key+"=:id")
                .setParameter("time",time).setParameter("id",id).executeUpdate(); em.clear();
    }
    Statistics start() {
        em.flush(); em.clear(); events.clear(); var s=emf.unwrap(SessionFactory.class).getStatistics(); s.clear(); return s;
    }
    record Cost(long sql,long inserted,long updated,long deleted,long rows) {}
    Cost cost(Statistics s) {
        em.flush(); long rows=Arrays.stream(s.getQueries()).mapToLong(q->s.getQueryStatistics(q).getExecutionRowCount()).sum();
        return new Cost(s.getPrepareStatementCount(),s.getEntityInsertCount(),s.getEntityUpdateCount(),s.getEntityDeleteCount(),rows);
    }
    void comparison(String name,Cost before,Cost after) {
        assertEquals(before.inserted,after.inserted); assertEquals(before.updated,after.updated); assertEquals(before.deleted,after.deleted);
        System.out.printf("READ_BATCHING %s before_sql=%d after_sql=%d before_result_rows=%d after_result_rows=%d inserts=%d updates=%d deletes=%d%n",
                name,before.sql,after.sql,before.rows,after.rows,after.inserted,after.updated,after.deleted);
    }
    void same(Object expected,Object actual) { assertEquals(json.valueToTree(expected),json.valueToTree(actual)); }

    @ParameterizedTest @ValueSource(ints={0,1,50})
    void adminResultsIncludeZeroInactiveNegativeAndSeasonBoundary(int count) {
        var actor=AuthMember.of(-1L,"admin","",AuthorityUtils.createAuthorityList("ROLE_ADMIN"),BaseStatus.ACTIVE);
        for(int i=0;i<count;i++) {
            var u=user(); if(i%2==0)u.updateStatus(BaseStatus.INACTIVE);
            if(i%3==0) continue;
            var p=post(u,PostType.REGULAR,true);
            var a=participant(p,u,true); stamp("participant","participant_id",a.getId(),boundary);
            var v=points.save(UserPoint.create(u,-7,"negative",PointType.ADD_ATTENDANCE)); stamp("user_point","point_id",v.getId(),boundary);
            var old=points.save(UserPoint.create(u,999,"old",PointType.ADD_ATTENDANCE)); stamp("user_point","point_id",old.getId(),boundary.minusNanos(1000));
            participant(p,u,false); // not attended, despite being in the season
        }
        var s=start(); var expected=users.findAll().stream().map(u->ReadUsersResponse.of(u,
                points.sumPointsByUserSince(u,boundary),participants.countByUserAndParticipantStatusAndCreatedAtGreaterThanEqual(u,ParticipantStatus.ATTENDED,boundary))).toList();
        var before=cost(s); s=start(); var actual=admin.getUsers(actor); var after=cost(s);
        same(expected,actual); comparison("admin_"+count,before,after);
        assertEquals(count*2L+1,before.sql); assertEquals(count==0?1:3,after.sql);
    }

    record AttendanceState(List<String> participants,List<String> points,Set<String> changes) {}
    AttendanceState state(Post p,List<User> people) {
        em.flush(); em.clear();
        var ps=participants.findByPost(p).stream().map(a->a.getUser().getId()+":"+a.getGroup()+":"+a.getParticipantStatus()).sorted().toList();
        var vs=points.findByPostIdAndUserIdIn(p.getId(),people.stream().map(User::getId).toList()).stream()
                .map(v->v.getUser().getId()+":"+v.getPoint()+":"+v.getPointType()+":"+v.getDescription()+":"+v.getCreatedAt().toLocalDate()).sorted().toList();
        var changes=new TreeSet<String>(); events.stream(RankingChanged.class).flatMap(e->e.points().stream())
                .forEach(c->changes.add(c.createdAt().toLocalDate()+":"+c.pointType()));
        return new AttendanceState(ps,vs,changes);
    }
    @ParameterizedTest @ValueSource(strings={"manual","remove","add"})
    void attendanceMatchesOldResultsAndIndividualWrites(String mode) {
        boolean closed=!mode.equals("manual"), removing=mode.equals("remove"); var owner=user();
        var oldPost=post(owner,PostType.REGULAR,closed); var newPost=post(owner,PostType.REGULAR,closed);
        var people=new ArrayList<User>(); var requests=new ArrayList<ManualAttendParticipantRequest>();
        for(int i=0;i<20;i++) {
            var u=user(); people.add(u); requests.add(request(u,!removing));
            for(var p:List.of(oldPost,newPost)) {
                var a=participant(p,u,removing); stamp("participant","participant_id",a.getId(),boundary);
                if(removing) { var v=points.save(UserPoint.createWithPost(u,10,"old",PointType.ADD_REGULAR_JOIN,p)); stamp("user_point","point_id",v.getId(),boundary); }
            }
        }
        var s=start(); if(closed)oldAttendance.fixAttendRun("regular",oldPost.getId(),auth(owner),requests);
        else oldAttendance.manualAttendRun("regular",oldPost.getId(),auth(owner),requests);
        var before=cost(s); var expected=state(oldPost,people);
        s=start(); if(closed)attendance.fixAttendRun("regular",newPost.getId(),auth(owner),requests);
        else attendance.manualAttendRun("regular",newPost.getId(),auth(owner),requests);
        var after=cost(s); var actual=state(newPost,people);
        assertEquals(expected,actual); comparison("attendance_"+mode+"_20",before,after);
        long expectedBefore = removing ? 101 : (closed ? 81 : 41);
        assertEquals(expectedBefore, before.sql);
        assertEquals(closed ? 43 : 22, after.sql);
        participants.findByPost(newPost).forEach(a->{assertTrue(a.getUpdatedAt().isAfter(boundary));assertEquals(boundary,a.getCreatedAt());});
    }

    @ParameterizedTest @EnumSource(PostType.class)
    void duplicateCorrectionsRetainSequentialDeletionReinsertAndBonus(PostType type) {
        var owner=user(); var member=user(); var people=List.of(owner,member);
        var oldPost=post(owner,type,true); var newPost=post(owner,type,true);
        for(var p:List.of(oldPost,newPost))for(var u:people) {
            participant(p,u,true); var v=points.save(UserPoint.createWithPost(u,99,"historic",PointType.ADD_REGULAR_JOIN,p));
            stamp("user_point","point_id",v.getId(),boundary);
        }
        var requests=List.of(request(owner,false),request(owner,true),request(owner,true),request(member,false),request(member,true),request(member,false));
        var s=start(); oldAttendance.fixAttendRun(type.name(),oldPost.getId(),auth(owner),requests);
        var before=cost(s); var expected=state(oldPost,people);
        s=start(); attendance.fixAttendRun(type.name(),newPost.getId(),auth(owner),requests);
        var after=cost(s); assertEquals(expected,state(newPost,people)); comparison("duplicates_"+type,before,after);
        assertEquals(1,points.findByPostIdAndUserIdIn(newPost.getId(),people.stream().map(User::getId).toList()).size());
    }

    @Test void manualDuplicatesKeepCallbacksAndEmptyRequestsDoNotQueryIn() {
        var owner=user(); var a=post(owner,PostType.REGULAR,false); var b=post(owner,PostType.REGULAR,false);
        participant(a,owner,false); participant(b,owner,false);
        var requests=List.of(request(owner,true),request(owner,false));
        var s=start(); oldAttendance.manualAttendRun("regular",a.getId(),auth(owner),requests); var before=cost(s); var expected=state(a,List.of(owner));
        s=start(); attendance.manualAttendRun("regular",b.getId(),auth(owner),requests); var after=cost(s);
        assertEquals(expected,state(b,List.of(owner))); comparison("manual_duplicates",before,after); assertEquals(2,after.updated);
        s=start(); attendance.manualAttendRun("regular",b.getId(),auth(owner),List.of()); assertEquals(1,cost(s).sql);
        b=posts.findById(b.getId()).orElseThrow(); b.updatePostStatus(PostStatus.CLOSED);
        s=start(); attendance.fixAttendRun("regular",b.getId(),auth(owner),List.of()); assertEquals(1,cost(s).sql);
    }

    @Test void missingParticipantKeepsExistingError() {
        var owner=user(); var p=post(owner,PostType.REGULAR,false); start();
        var ex=assertThrows(ParticipantException.class,()->attendance.manualAttendRun("regular",p.getId(),auth(owner),List.of(request(owner,true))));
        assertEquals(BaseResponseStatus.NOT_PARTICIPATED,ex.getStatus());
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void laterInvalidRequestRollsBackAlreadyFlushedUpdatesAndPoints(boolean correction) {
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        record Fixture(User owner,User missing,Post post,Long pointId) {}
        var fixture=tx.execute(status->{
            var owner=user();var missing=user();var p=post(owner,PostType.REGULAR,correction);
            participant(p,owner,correction);
            Long pointId=correction?points.save(UserPoint.createWithPost(owner,17,"unchanged",PointType.ADD_REGULAR_JOIN,p)).getId():null;
            return new Fixture(owner,missing,p,pointId);
        });
        try {
            var requests=List.of(request(fixture.owner,!correction),request(fixture.owner,correction),request(fixture.missing,true));
            assertThrows(ParticipantException.class,()->{
                if(correction)attendance.fixAttendRun("regular",fixture.post.getId(),auth(fixture.owner),requests);
                else attendance.manualAttendRun("regular",fixture.post.getId(),auth(fixture.owner),requests);
            });
            tx.executeWithoutResult(status->{
                var p=participants.findByPostIdAndUserId(fixture.post.getId(),fixture.owner.getId()).orElseThrow();
                assertEquals(correction?ParticipantStatus.ATTENDED:ParticipantStatus.PENDING,p.getParticipantStatus());
                var values=points.findByPostIdAndUserIdIn(fixture.post.getId(),List.of(fixture.owner.getId()));
                assertEquals(correction?1:0,values.size());
                if(correction){assertEquals(fixture.pointId,values.get(0).getId());assertEquals(17,values.get(0).getPoint());}
            });
        } finally {
            tx.executeWithoutResult(status->{
                points.deleteByPost(fixture.post);participants.deleteByPost(fixture.post);em.flush();
                posts.deleteById(fixture.post.getId());em.flush();users.deleteAll(List.of(fixture.owner,fixture.missing));
            });
        }
    }
}
