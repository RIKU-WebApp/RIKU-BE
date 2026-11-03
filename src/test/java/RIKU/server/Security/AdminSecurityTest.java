package RIKU.server.Security;

import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import RIKU.server.Repository.UserRepository;
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
}

