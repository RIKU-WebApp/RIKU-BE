package RIKU.server.domains.run;

import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.user.entity.UserRole;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.security.AuthMember;
import RIKU.server.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RunCoverApiTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtTokenProvider jwt;
    @Autowired ObjectMapper mapper;

    @ParameterizedTest
    @ValueSource(strings = {"flash", "training", "event"})
    void fullImageSurvivesOtherEditsAndCanBeChangedWithoutUploadingAgain(String kind) throws Exception {
        var user = users.save(User.create(UUID.randomUUID().toString(), "hash", "author", "college", "major", null));
        user.updateUserRole(UserRole.ADMIN);
        user.updatePacer(true);
        var principal = AuthMember.builder().id(user.getId()).username(user.getStudentId()).status(user.getStatus())
                .authorities(AuthorityUtils.createAuthorityList(UserRole.ADMIN.getRole())).build();
        var token = jwt.createToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())).getAccessToken();
        String list = "/run/" + kind;
        var create = multipart(list + "/post").param("title", "러닝").param("content", "모집")
                .param("location", "공원").param("date", LocalDateTime.now().plusDays(2).withNano(0).toString())
                .param("showFullImage", "true").header("Authorization", token);
        if (kind.equals("event")) create.param("eventType", "마라톤");
        if (kind.equals("training")) {
            create.param("trainingType", "인터벌").param("pacers[0].pacerId", user.getId().toString())
                    .param("pacers[0].group", "A").param("pacers[0].distance", "5").param("pacers[0].pace", "6:00");
        }
        String response = mvc.perform(create).andExpect(jsonPath("$.isSuccess").value(true)).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(response).get("result").get("postId").asLong();
        String detail = list + "/post/" + id;
        mvc.perform(multipart(HttpMethod.PATCH, detail).header("Authorization", token).param("title", "변경"))
                .andExpect(jsonPath("$.isSuccess").value(true));
        mvc.perform(get(detail).header("Authorization", token)).andExpect(jsonPath("$.result.showFullImage").value(true));
        String setting = "$.result.upcomingRuns[?(@.id == " + id + ")].showFullImage";
        mvc.perform(get(list).header("Authorization", token)).andExpect(jsonPath(setting).value(org.hamcrest.Matchers.contains(true)));
        mvc.perform(multipart(HttpMethod.PATCH, detail).header("Authorization", token).param("showFullImage", "false"))
                .andExpect(jsonPath("$.isSuccess").value(true));
        mvc.perform(get(list).header("Authorization", token)).andExpect(jsonPath(setting).value(org.hamcrest.Matchers.contains(false)));
    }
}
