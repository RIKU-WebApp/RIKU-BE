package RIKU.server.domains.run;

import RIKU.server.domains.run.recruitment.*;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.participant.service.ParticipantService;
import RIKU.server.domains.run.post.repository.PostRepository;
import RIKU.server.domains.run.post.entity.PostStatus;
import RIKU.server.domains.user.entity.*;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.security.AuthMember;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static RIKU.server.domains.run.recruitment.RunEnrollment.Role.*;

@SpringBootTest @ActiveProfiles("test")
class RecruitmentIntegrationTest {
    @Autowired RecruitmentService service;
    @Autowired UserRepository users;
    @Autowired ParticipantRepository participants;
    @Autowired ParticipantService legacy;
    @Autowired PostRepository posts;
    @Autowired PlatformTransactionManager manager;
    @Autowired RunWaitlist waitlist;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = RIKU.server.domains.run.post.entity.PostType.class, names = {"FLASH", "TRAINING", "EVENT"})
    void cancellationAndReentryPreserveWaitlistOrder(RIKU.server.domains.run.post.entity.PostType type) {
        var post = posts.save(RIKU.server.domains.run.post.entity.Post.create(users.findById(admin.getId()).orElseThrow(),
                "모집", "공원", LocalDateTime.now(ZoneOffset.UTC).plusDays(1), "안내", null, type));
        var tx = new TransactionTemplate(manager);
        tx.executeWithoutResult(status -> waitlist.configure(post, 1));
        var first = user(UserRole.MEMBER);
        var second = user(UserRole.MEMBER);
        var third = user(UserRole.MEMBER);
        var id = post.getId();
        var path = type.name().toLowerCase(Locale.ROOT);
        String group = type == RIKU.server.domains.run.post.entity.PostType.TRAINING ? "A" : null;
        legacy.joinRun(path, id, group, first);
        legacy.joinRun(path, id, group, second);
        legacy.joinRun(path, id, group, third);
        assertEquals(1, participants.countByPostId(id));
        assertTrue(waitlist.read(path, id, second.getId()).mine());
        waitlist.leave(path, id, second.getId());
        legacy.joinRun(path, id, group, second);
        legacy.joinRun(path, id, null, first);
        assertTrue(participants.findByPostIdAndUserId(id, third.getId()).isPresent());
        assertTrue(waitlist.read(path, id, second.getId()).mine());
        tx.executeWithoutResult(status -> waitlist.configure(post, null));
        assertEquals(2, participants.countByPostId(id));
        assertTrue(waitlist.read(path, id, second.getId()).waiting().isEmpty());
    }
    AuthMember admin;
    @BeforeEach void setup(){admin=user(UserRole.ADMIN);}
    AuthMember user(UserRole role) {
        var u=User.create(UUID.randomUUID().toString(),"hash","테스트 러너","college","major",null);u.updateUserRole(role);users.save(u);
        return AuthMember.builder().id(u.getId()).username(u.getStudentId()).status(u.getStatus()).authorities(AuthorityUtils.createAuthorityList(role.getRole())).build();
    }
    Long run(int capacity) {
        return service.create(admin,new RecruitmentRequest("선착순 테스트","공원",LocalDateTime.now(ZoneOffset.UTC).plusDays(1),"안내",capacity,
                List.of(new RecruitmentRequest.Group("A","6:30",BigDecimal.valueOf(5)),new RecruitmentRequest.Group("B","7:00",BigDecimal.valueOf(3)))));
    }
    @Test void cancellationPromotesAndRejoinGoesToBackOfQueue() {
        Long id=run(1);var first=user(UserRole.MEMBER);var second=user(UserRole.MEMBER);var third=user(UserRole.MEMBER);
        assertEquals("CONFIRMED",service.join(id,first,RUNNER,"A").mine().state());
        assertEquals(1,service.join(id,second,RUNNER,"A").mine().waitingNumber());
        assertEquals(2,service.join(id,third,RUNNER,"A").mine().waitingNumber());
        service.cancel(id,first);
        assertEquals("CONFIRMED",service.read(id,second).mine().state());
        assertEquals(2,service.join(id,first,RUNNER,"A").mine().waitingNumber());
        assertEquals(1,participants.countByPostId(id));
    }
    @Test void sharedCapacityAndFifoAcrossRoles() {
        Long id=run(1);
        var runner=user(UserRole.MEMBER);
        var pacer=user(UserRole.MEMBER);
        var nextRunner=user(UserRole.MEMBER);
        new TransactionTemplate(manager).executeWithoutResult(tx -> users.findById(pacer.getId()).orElseThrow().updatePacer(true));
        service.join(id,runner,RUNNER,"A");
        assertEquals(1,service.join(id,pacer,PACER,"A").mine().waitingNumber());
        assertEquals(2,service.join(id,nextRunner,RUNNER,"B").mine().waitingNumber());
        service.cancel(id,runner);
        assertEquals("PACER",service.read(id,runner).confirmed().get(0).role());
        assertEquals("CONFIRMED",service.read(id,pacer).mine().state());
        service.cancel(id,pacer);
        assertEquals("CONFIRMED",service.read(id,nextRunner).mine().state());
    }

    @Test void pacerPermissionRequiredEvenForAdministrators() {
        Long id=run(2);
        for (var actor : List.of(admin,user(UserRole.MEMBER))) {
            var denied=assertThrows(ResponseStatusException.class, () -> service.join(id,actor,PACER,"A"));
            assertEquals(403,denied.getStatusCode().value());
            assertNull(service.read(id,actor).mine());
        }
    }
    @Test void duplicateRequestsAndGroupChangesKeepPlaceAndOldApiCannotBypassCapacity() {
        Long id=run(1);var a=user(UserRole.MEMBER);var b=user(UserRole.MEMBER);
        service.join(id,a,RUNNER,"A");service.join(id,b,RUNNER,"A");
        assertThrows(ResponseStatusException.class,()->service.join(id,b,RUNNER,"B"));
        var result=service.join(id,b,RUNNER,"A");assertEquals(1,result.waiting().size());assertEquals(1,result.mine().waitingNumber());
        assertThrows(ResponseStatusException.class,()->legacy.joinRun("regular",id,"A",b));
        assertEquals(1,participants.countByPostId(id));
        assertThrows(ResponseStatusException.class,()->service.join(id,a,RUNNER,"UNKNOWN"));
    }
    @Test void closedRunAndAttendedCancellationAreRejected() {
        Long id=run(1);var a=user(UserRole.MEMBER);service.join(id,a,RUNNER,"A");
        new TransactionTemplate(manager).executeWithoutResult(tx->participants.findByPostIdAndUserId(id,a.getId()).orElseThrow().attend());
        assertThrows(ResponseStatusException.class,()->service.cancel(id,a));
        new TransactionTemplate(manager).executeWithoutResult(tx->posts.findById(id).orElseThrow().updatePostStatus(PostStatus.CLOSED));
        assertThrows(ResponseStatusException.class,()->service.join(id,user(UserRole.MEMBER),RUNNER,"A"));
    }
    @Test void creatorAndLatePacerCanCheckInAfterRecruitment() {
        Long id = service.create(admin, new RecruitmentRequest("출석", "공원",
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5), "안내", 2,
                List.of(new RecruitmentRequest.Group("A", "6:30", BigDecimal.ONE))));
        service.join(id, admin, RUNNER, "A");
        String code = legacy.createAttendanceCode("regular", id, admin);
        assertEquals("ATTENDED", service.read(id, admin).mine().attendance());

        var pacer = user(UserRole.MEMBER);
        new TransactionTemplate(manager).executeWithoutResult(tx -> users.findById(pacer.getId()).orElseThrow().updatePacer(true));
        service.join(id, pacer, PACER, "A");
        legacy.attendRun("regular", id, pacer, code);
        assertEquals("ATTENDED", service.read(id, pacer).mine().attendance());
    }

    @Test void changingCapacityPromotesInOrderAndProtectsExistingParticipants() {
        Long id = run(1);
        var first = user(UserRole.MEMBER);
        var second = user(UserRole.MEMBER);
        var third = user(UserRole.MEMBER);
        for (var member : List.of(first, second, third)) service.join(id, member, RUNNER, "A");
        var tx = new TransactionTemplate(manager);
        tx.executeWithoutResult(t -> service.changeCapacity(posts.findForUpdate(id).orElseThrow(), 2));
        assertEquals("CONFIRMED", service.read(id, second).mine().state());
        assertEquals("WAITLISTED", service.read(id, third).mine().state());
        assertThrows(ResponseStatusException.class, () ->
                tx.executeWithoutResult(t -> service.changeCapacity(posts.findForUpdate(id).orElseThrow(), 1)));
        tx.executeWithoutResult(t -> service.changeCapacity(posts.findForUpdate(id).orElseThrow(), null));
        assertTrue(service.read(id, third).waiting().isEmpty());
        assertNull(service.read(id, third).capacity());
        assertEquals("CONFIRMED", service.join(id, user(UserRole.MEMBER), RUNNER, "A").mine().state());
    }

    static RunRecruitment.GroupPlan plan(String group,int seats) {
        return new RunRecruitment.GroupPlan(group,"6:00",BigDecimal.TEN,seats);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(RIKU.server.domains.run.post.entity.PostType.class)
    void promoteEarliestEligibleGroupAndProtectExistingSeats(RIKU.server.domains.run.post.entity.PostType type) {
        boolean regular = type == RIKU.server.domains.run.post.entity.PostType.REGULAR;
        var id = regular ? run(2) : posts.save(RIKU.server.domains.run.post.entity.Post.create(users.findById(admin.getId()).orElseThrow(),
                "그룹 모집","공원",LocalDateTime.now(ZoneOffset.UTC).plusDays(1),"안내",null,type)).getId();
        var path=type.name().toLowerCase(Locale.ROOT);
        java.util.function.Consumer<List<RunRecruitment.GroupPlan>> configure = plans ->
            new TransactionTemplate(manager).executeWithoutResult(t -> {
                var post=posts.findForUpdate(id).orElseThrow();
                if (regular) service.edit(post,true,2,plans);
                else waitlist.configure(post,true,2,plans);
            });
        configure.accept(List.of(plan("A",1),plan("B",2)));
        java.util.function.BiConsumer<AuthMember,String> join=(u,g)->{
            if (regular) service.join(id,u,RUNNER,g); else legacy.joinRun(path,id,g,u);
        };
        var a=user(UserRole.MEMBER);var aw=user(UserRole.MEMBER);var b=user(UserRole.MEMBER);var bw=user(UserRole.MEMBER);
        join.accept(a,"A"); join.accept(aw,"A");
        assertThrows(ResponseStatusException.class,()->join.accept(aw,"B"));
        join.accept(b,"B"); join.accept(bw,"B");
        assertThrows(ResponseStatusException.class,()->join.accept(b,"A"));
        assertThrows(ResponseStatusException.class,()->configure.accept(List.of(plan("B",2))));
        assertEquals("B",participants.findByPostIdAndUserId(id,b.getId()).orElseThrow().getGroup());
        if (regular) service.cancel(id,b); else legacy.joinRun(path,id,null,b);
        assertTrue(participants.findByPostIdAndUserId(id,bw.getId()).isPresent());
        assertTrue(participants.findByPostIdAndUserId(id,aw.getId()).isEmpty());
        if (regular) service.cancel(id,a); else legacy.joinRun(path,id,null,a);
        assertTrue(participants.findByPostIdAndUserId(id,aw.getId()).isPresent());
    }
    @Test void parallelRequestsDoNotOversell() throws Exception {
        Long id=run(3);List<AuthMember> actors=new ArrayList<>();for(int i=0;i<12;i++)actors.add(user(UserRole.MEMBER));
        var pool=Executors.newFixedThreadPool(6);
        try {
            List<Callable<RecruitmentService.Summary>> jobs=actors.stream().<Callable<RecruitmentService.Summary>>map(a->()->service.join(id,a,RUNNER,"A")).toList();
            for(var future:pool.invokeAll(jobs))future.get(20,TimeUnit.SECONDS);
        } finally {pool.shutdownNow();}
        var result=service.read(id,admin);assertEquals(3,result.confirmed().size());assertEquals(9,result.waiting().size());assertEquals(3,participants.countByPostId(id));
    }
}
