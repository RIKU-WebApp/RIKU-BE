package RIKU.server;

import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.user.repository.UserRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:signup-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignupIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @MockBean AmazonS3 s3;

    private ResultActions signup(String studentId, String phone, String password) throws Exception {
        var body = new HashMap<String, String>(Map.of("studentId", studentId, "password", password,
                "name", "테스트", "college", "건축대학", "major", "건축학부"));
        body.put("phone", phone);
        return mvc.perform(post("/user/signup").contentType("application/json")
                .content(mapper.writeValueAsBytes(body)));
    }

    @Test
    void legacyPhoneConflictCanBeCorrectedWithoutChangingOtherFields() throws Exception {
        users.saveAndFlush(User.create("legacy", "encoded", "기존", "건축대학", "건축학부", "010-3456-7890"));
        signup("new", "01034567890", "Signup123!")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.result.errors[0].fieldName").value("phone"));
        signup("new", null, "Signup123!")
                .andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true));
        signup("new", null, "Signup123!")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.result.errors[0].fieldName").value("studentId"));
    }

    @Test
    void invalidFieldsAreActionableWithoutEchoingPrivateValues() throws Exception {
        signup("invalid", "not-a-phone", "badpass")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result.errors[?(@.fieldName == 'phone')]").isNotEmpty())
                .andExpect(jsonPath("$.result.errors[?(@.fieldName == 'password')]").isNotEmpty())
                .andExpect(content().string(not(containsString("badpass"))))
                .andExpect(content().string(not(containsString("not-a-phone"))));
    }
}
