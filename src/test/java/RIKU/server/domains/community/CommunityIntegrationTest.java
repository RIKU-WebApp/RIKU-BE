package RIKU.server.domains.community;
import RIKU.server.domains.user.entity.*;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.security.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test") @AutoConfigureMockMvc @Transactional
class CommunityIntegrationTest {
    @Autowired UserRepository users; @Autowired CommunityService service; @Autowired JwtTokenProvider jwt; @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
    @Autowired RIKU.server.domains.run.recruitment.RecruitmentService runs;
    @Autowired RIKU.server.domains.run.post.repository.PostRepository posts;
    @Autowired RIKU.server.domains.run.participant.repository.ParticipantRepository participants;
    @org.springframework.boot.test.mock.mockito.MockBean RIKU.server.infra.storage.S3Uploader storage;
    AuthMember member,other,admin;
    @BeforeEach void seed(){member=user(UserRole.MEMBER);other=user(UserRole.MEMBER);admin=user(UserRole.ADMIN);}
    AuthMember user(UserRole role) {var u=User.create(UUID.randomUUID().toString(),"hash","이름","private-college","private-major",UUID.randomUUID().toString());u.updateUserRole(role);users.save(u);return AuthMember.builder().id(u.getId()).username(u.getStudentId()).status(u.getStatus()).authorities(AuthorityUtils.createAuthorityList(role.getRole())).build();}
    String token(AuthMember a){return jwt.createToken(new UsernamePasswordAuthenticationToken(a,null,a.getAuthorities())).getAccessToken();}
    @Test void feedbackIsPrivateAndOnlyAdminCanRespond() throws Exception {
        var f=service.submit(member,new CommunityController.FeedbackRequest(Feedback.Category.BUG,"문제","나만의 비공개 내용"));
        assertEquals(1,service.myFeedback(member,0).getTotalElements());assertEquals(0,service.myFeedback(other,0).getTotalElements());
        mvc.perform(get("/admin/feedback").header("Authorization",token(member))).andExpect(status().isForbidden());
        assertThrows(ResponseStatusException.class,()->service.respond(other,f.getId(),new CommunityController.AnswerRequest(Feedback.Status.COMPLETED,"답변")));
        service.respond(admin,f.getId(),new CommunityController.AnswerRequest(Feedback.Status.COMPLETED,"수정했습니다."));
        assertEquals("수정했습니다.",service.myFeedback(member,0).getContent().get(0).getAnswer());
    }
    @Test void certificatesCanOnlyBeListedReadAndDeletedByOwner() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"jpeg",bytes);
        var upload=new org.springframework.mock.web.MockMultipartFile("image","run.jpg","image/jpeg",bytes.toByteArray());
        var response=mvc.perform(multipart("/user/certificates").file(upload).param("title","완주").param("date","2026-01-01")
            .header("Authorization",token(member))).andExpect(status().isOk()).andReturn().getResponse();
        long id=mapper.readTree(response.getContentAsString()).path("result").asLong(); assertTrue(id>0);
        String path="/user/certificates/"+id;
        mvc.perform(get("/user/certificates").header("Authorization",token(member))).andExpect(jsonPath("$.result[0].id").value(id));
        mvc.perform(get("/user/certificates").header("Authorization",token(other))).andExpect(status().isOk()).andExpect(jsonPath("$.result").doesNotExist());
        mvc.perform(get(path+"/image").header("Authorization",token(other))).andExpect(status().isNotFound());
        mvc.perform(delete(path).header("Authorization",token(other))).andExpect(status().isNotFound());
        mvc.perform(get(path+"/image").header("Authorization",token(member))).andExpect(status().isOk()).andExpect(content().bytes(bytes.toByteArray()));
        mvc.perform(delete(path).header("Authorization",token(member))).andExpect(status().isNoContent());
        mvc.perform(get(path+"/image").header("Authorization",token(member))).andExpect(status().isNotFound());
    }
    @Test void profileOnlyContainsNameAndBadges() throws Exception {
        service.record(member,new CommunityController.RecordRequest("private run",LocalDate.now(),BigDecimal.valueOf(100),45));
        service.award(admin,member.getId(),new CommunityController.AwardRequest("운영진 수여","private note"));
        mvc.perform(get("/user/"+member.getId()+"/public-profile")).andExpect(status().isUnauthorized());
        var response=mvc.perform(get("/user/"+member.getId()+"/public-profile").header("Authorization",token(other))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var result=mapper.readTree(response).path("result");assertEquals(Set.of("name","badges","medals"),mapper.convertValue(result,Map.class).keySet());
        assertTrue(result.path("badges").isEmpty());assertTrue(result.path("medals").isEmpty());
        assertFalse(response.contains("private"));assertFalse(response.contains("studentId"));assertFalse(response.contains("phone"));
    }
    @Test void personalDistanceCreatesDistinctBadgeAndDeletionRecalculates() {
        var r=service.record(member,new CommunityController.RecordRequest("개인 기록",LocalDate.now(),BigDecimal.valueOf(100),600));
        var a=service.activity(member.getId());assertEquals(0,BigDecimal.valueOf(100).compareTo(a.personalKm()));assertEquals(0,a.verifiedKm().signum());
        assertTrue(a.badges().stream().anyMatch(b->b.code().equals("PERSONAL_100")));
        assertFalse(a.badges().stream().anyMatch(b->b.source().equals("CLUB")));
        assertThrows(ResponseStatusException.class,()->service.deleteRecord(other,r.getId()));
        service.deleteRecord(member,r.getId());assertTrue(service.activity(member.getId()).badges().isEmpty());
    }
    @Test void previewIsPrivateAndOnlyReturnsTheFirstThreeManualAwards() throws Exception {
        for (String name : List.of("첫째","둘째","셋째","넷째"))
            service.award(admin,member.getId(),new CommunityController.AwardRequest(name,"수여"));
        var path="/user/activity/badge-preview";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization",token(member))).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.badges.length()").value(3))
                .andExpect(jsonPath("$.result.badges[2].name").value("셋째"))
                .andExpect(jsonPath("$.result.medalCount").value(0))
                .andExpect(jsonPath("$.result.records").doesNotExist());
        assertTrue(service.badgePreview(other.getId()).badges().isEmpty());
    }
    @Test void badgeAdministrationRequiresAuthenticatedAdmin() throws Exception {
        String path = "/admin/users/" + other.getId() + "/badges";
        String body = mapper.writeValueAsString(Map.of("name", "완주", "description", "정규런 완주"));
        mvc.perform(post(path).contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post(path).header("Authorization", token(member)).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).header("Authorization", token(admin)).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.awarded").value(true));
    }

    @Test void badgeAwardRequiresAdminAndCannotBeRepeated() {
        var r=new CommunityController.AwardRequest("꾸준한 러너","꾸준한 참여를 응원합니다.");
        assertThrows(ResponseStatusException.class,()->service.award(member,other.getId(),r));
        service.award(admin,other.getId(),r);
        assertThrows(ResponseStatusException.class,()->service.award(admin,other.getId(),r));
        assertEquals("ADMIN",service.privateActivity(other.getId()).badges().get(0).source());
    }
    @Test void officialBadgesRequireConfirmedAttendanceAndClosedRun() {
        var id=runs.create(admin,new RIKU.server.domains.run.recruitment.RecruitmentRequest("공식 거리", "공원",
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(1),"안내",1,
                List.of(new RIKU.server.domains.run.recruitment.RecruitmentRequest.Group("A","6:30",BigDecimal.valueOf(100)))));
        runs.join(id,member,RIKU.server.domains.run.recruitment.RunEnrollment.Role.RUNNER,"A");
        runs.join(id,other,RIKU.server.domains.run.recruitment.RunEnrollment.Role.RUNNER,"A");
        var participant=participants.findByPostIdAndUserId(id,member.getId()).orElseThrow();
        participant.attend();
        assertEquals(0,service.activity(member.getId()).verifiedKm().signum());
        posts.findById(id).orElseThrow().updatePostStatus(RIKU.server.domains.run.post.entity.PostStatus.CLOSED);
        assertTrue(service.activity(member.getId()).badges().stream().anyMatch(b->b.code().equals("CLUB_100")));
        assertEquals(0,service.activity(other.getId()).verifiedKm().signum());
        participant.absent();
        assertFalse(service.activity(member.getId()).badges().stream().anyMatch(b->b.source().equals("CLUB")));
    }
    @Test void capacityIsOptionalButMustBePositiveWhenSet() throws Exception {
        var payload=new LinkedHashMap<String,Object>();
        payload.put("title","모집 검증");payload.put("location","공원");payload.put("content","안내");
        payload.put("date",java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(1).toString());
        payload.put("groups",List.of(Map.of("name","A","pace","6:30","distanceKm",5)));
        mvc.perform(post("/run/regular/recruitment").header("Authorization",token(admin)).contentType("application/json")
                .content(mapper.writeValueAsString(payload))).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true));
        payload.put("title","정원이 있는 정규런");
        payload.put("capacity",0);
        mvc.perform(post("/run/regular/recruitment").header("Authorization",token(admin)).contentType("application/json")
                .content(mapper.writeValueAsString(payload))).andExpect(status().isBadRequest());
        payload.put("capacity",2);
        mvc.perform(post("/run/regular/recruitment").header("Authorization",token(admin)).contentType("application/json")
                .content(mapper.writeValueAsString(payload))).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true));
    }
    @Test void originalCreationFormCanUploadCoverAndCourseImagesWithCapacities() throws Exception {
        org.mockito.Mockito.when(storage.upload(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq("postImg")))
                .thenReturn("https://example.test/cover.png");
        org.mockito.Mockito.when(storage.upload(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq("attachmentImg")))
                .thenReturn("https://example.test/course.png");
        var payload=Map.of("title","사진 첨부 검증","location","공원","content","안내",
                "date",java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(1).toString(),
                "capacity",2,"groups",List.of(Map.of("name","A","pace","6:30","distanceKm",5)));
        var result=mvc.perform(multipart("/run/regular/recruitment")
                .file(new org.springframework.mock.web.MockMultipartFile("request","request.json","application/json",mapper.writeValueAsBytes(payload)))
                .file(new org.springframework.mock.web.MockMultipartFile("postImage","cover.png","image/png",new byte[]{1}))
                .file(new org.springframework.mock.web.MockMultipartFile("attachments","course.png","image/png",new byte[]{2}))
                .header("Authorization",token(admin))).andExpect(status().isOk()).andReturn();
        Long id=mapper.readTree(result.getResponse().getContentAsString()).path("result").path("postId").asLong();
        assertEquals("https://example.test/cover.png",posts.findById(id).orElseThrow().getPostImageUrl());
        mvc.perform(get("/run/regular/post/"+id).header("Authorization",token(admin)))
                .andExpect(jsonPath("$.result.attachmentUrls[0]").value("https://example.test/course.png"));
        assertEquals(2,runs.read(id,admin).capacity());
    }
    @Test void regularRosterEditingRequiresAdminEvenWhenMemberCreatedRun() throws Exception {
        users.findById(member.getId()).orElseThrow().updatePacer(true);
        Long id=runs.create(member,new RIKU.server.domains.run.recruitment.RecruitmentRequest("명단 권한","공원",
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(1),"안내",2,
                List.of(new RIKU.server.domains.run.recruitment.RecruitmentRequest.Group("A","6:30",BigDecimal.valueOf(5)))));
        runs.join(id,other,RIKU.server.domains.run.recruitment.RunEnrollment.Role.RUNNER,"A");
        String body=mapper.writeValueAsString(List.of(Map.of("userId",other.getId(),"isAttend",true)));
        mvc.perform(patch("/run/regular/post/"+id+"/manual-attendance").header("Authorization",token(member))
                .contentType("application/json").content(body)).andExpect(status().isForbidden());
        assertEquals("PENDING",participants.findByPostIdAndUserId(id,other.getId()).orElseThrow().getParticipantStatus().name());
        mvc.perform(patch("/run/regular/post/"+id+"/manual-attendance").header("Authorization",token(admin))
                .contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true));
        assertEquals("ATTENDED",participants.findByPostIdAndUserId(id,other.getId()).orElseThrow().getParticipantStatus().name());
    }
}
