package RIKU.server.security;

import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.user.entity.UserRole;
import RIKU.server.domains.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtTokenProvider jwt;
    User admin;
    User member;

    @BeforeEach void seed() {
        admin = users.save(User.create(UUID.randomUUID().toString(), "hash-a", "admin", "college", "major", null));
        admin.updateUserRole(UserRole.ADMIN);
        member = users.save(User.create(UUID.randomUUID().toString(), "hash-b", "member", "college", "major", null));
        member.updateUserRole(UserRole.MEMBER);
    }
    JwtInfo tokens(User user) {
        var principal = AuthMember.builder().id(user.getId()).username(user.getStudentId())
                .authorities(AuthorityUtils.createAuthorityList(user.getUserRole().getRole()))
                .status(user.getStatus()).build();
        return jwt.createToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    @Test void anonymousAndMemberCannotReadAdminButAdminCan() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin").header("Authorization", tokens(member).getAccessToken())).andExpect(status().isForbidden());
        mvc.perform(get("/admin").header("Authorization", tokens(admin).getAccessToken())).andExpect(status().isOk());
    }
    @Test void roleBackdoorIsDisabledForEveryone() throws Exception {
        mvc.perform(patch("/user/" + member.getId() + "/role").param("userRole", "ADMIN"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/user/" + member.getId() + "/role").param("userRole", "ADMIN")
                .header("Authorization", tokens(admin).getAccessToken())).andExpect(status().isForbidden());
        assertEquals(UserRole.MEMBER, member.getUserRole());
    }

    @Test void publicPasswordInquiryRequiresAdminForReviewAndPreservesCredentials() throws Exception {
        String endpoint = "/admin/password-inquiries";
        String adminToken = tokens(admin).getAccessToken();
        String memberToken = tokens(member).getAccessToken();
        mvc.perform(post("/user/password-inquiries").contentType("application/json")
                        .content("""
                                {"studentId":"legacy","name":"member","major":"major","phone":"incorrect"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result.errors[0].fieldName").value("phone"))
                .andExpect(jsonPath("$.result.errors[0].rejectValue").doesNotExist());
        var existing = mvc.perform(post("/user/password-inquiries").contentType("application/json")
                        .content("""
                                {"studentId":"%s","name":"member","major":"major","phone":""}
                                """.formatted(member.getStudentId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result").value(true))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/user/password-inquiries").contentType("application/json")
                        .content("""
                                {"studentId":"unknown","name":"visitor","major":"major","phone":"010-1234-5678"}
                                """))
                .andExpect(status().isOk()).andExpect(content().string(existing));
        mvc.perform(get(endpoint)).andExpect(status().isUnauthorized());
        mvc.perform(get(endpoint).header("Authorization", memberToken)).andExpect(status().isForbidden());
        var page = mvc.perform(get(endpoint).header("Authorization", adminToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].phone").value("01012345678"))
                .andExpect(jsonPath("$.result.content[1].studentId").value(member.getStudentId()))
                .andExpect(jsonPath("$.result.content[1].status").value("RECEIVED"))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(page, "$.result.content[1].id");
        String item = endpoint + "/" + id;
        String resolved = "{\"status\":\"COMPLETED\"}";
        mvc.perform(patch(item).header("Authorization", memberToken).contentType("application/json").content(resolved))
                .andExpect(status().isForbidden());
        mvc.perform(patch(item).header("Authorization", adminToken).contentType("application/json").content(resolved))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("COMPLETED"));
        assertEquals("hash-b", member.getPassword());
        mvc.perform(get("/admin/feedback").header("Authorization", adminToken))
                .andExpect(jsonPath("$.result.totalElements").value(0));
    }
    @Test void refreshAndMalformedTokensCannotAuthenticate() throws Exception {
        mvc.perform(get("/admin").header("Authorization", tokens(admin).getRefreshToken())).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
    }
    @Test void otherEnvironmentIsRejected() throws Exception {
        var original = ReflectionTestUtils.getField(jwt, "issuer");
        String foreign;
        try { ReflectionTestUtils.setField(jwt, "issuer", "riku-dev"); foreign = tokens(admin).getAccessToken(); }
        finally { ReflectionTestUtils.setField(jwt, "issuer", original); }
        mvc.perform(get("/admin").header("Authorization", foreign)).andExpect(status().isUnauthorized());
    }
    @Test void passwordChangeRevokesExistingAccess() throws Exception {
        String token = tokens(admin).getAccessToken();
        admin.updatePassword("different-hash");
        mvc.perform(get("/admin").header("Authorization", token)).andExpect(status().isUnauthorized());
    }
    @Test void roleDemotionTakesEffectImmediately() throws Exception {
        String token = tokens(admin).getAccessToken();
        admin.updateUserRole(UserRole.MEMBER);
        mvc.perform(get("/admin").header("Authorization", token)).andExpect(status().isForbidden());
    }
    @Test void privateProfileRequiresAuthentication() throws Exception {
        mvc.perform(get("/user/profile/detail")).andExpect(status().isUnauthorized());
    }

    @Test void renewalPromotesOnceAndKeepsEachSemesterSeparate() throws Exception {
        member.updateUserRole(UserRole.NEW_MEMBER);
        member.updatePacer(true);
        String url = "/admin/renewals/2027-1/" + member.getStudentId();
        String authorization = tokens(admin).getAccessToken();
        mvc.perform(put(url)).andExpect(status().isUnauthorized());
        mvc.perform(put(url).header("Authorization", tokens(member).getAccessToken())).andExpect(status().isForbidden());
        assertEquals(UserRole.NEW_MEMBER, member.getUserRole());
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(put(url).header("Authorization", authorization)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("MEMBER"));
        }
        assertEquals(UserRole.MEMBER, member.getUserRole());
        assertTrue(member.getIsPacer());
        mvc.perform(get("/admin/renewals/2027-1").header("Authorization", authorization))
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0]").value(member.getStudentId()));
        mvc.perform(get("/admin/renewals/2027-2").header("Authorization", authorization))
                .andExpect(jsonPath("$.isSuccess").value(true)).andExpect(jsonPath("$.result").doesNotExist());
        mvc.perform(put("/admin/renewals/2027-2/" + admin.getStudentId()).header("Authorization", authorization))
                .andExpect(jsonPath("$.result").value("ADMIN"));
        member.updateStatus(RIKU.server.global.entity.BaseStatus.INACTIVE);
        mvc.perform(put("/admin/renewals/2027-2/" + member.getStudentId()).header("Authorization", authorization))
                .andExpect(status().isConflict());
        mvc.perform(put("/admin/renewals/2027-3/" + member.getStudentId()).header("Authorization", authorization))
                .andExpect(status().isBadRequest());
    }
}
