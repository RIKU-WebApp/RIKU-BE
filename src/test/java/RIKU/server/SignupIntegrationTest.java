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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
    @Autowired JdbcTemplate db;
    @MockBean AmazonS3 s3;

    private ResultActions signup(String studentId, String phone, String password) throws Exception {
        return signup(studentId, phone, password, "테스트");
    }

    private ResultActions signup(String studentId, String phone, String password, String name) throws Exception {
        var body = new HashMap<String, String>(Map.of("studentId", studentId, "password", password,
                "name", name, "college", "건축대학", "major", "건축학부"));
        body.put("phone", phone);
        return mvc.perform(post("/user/signup").contentType("application/json")
                .content(mapper.writeValueAsBytes(body)));
    }

    @Test
    void legacyPhoneConflictCanBeCorrectedWithoutChangingOtherFields() throws Exception {
        users.saveAndFlush(User.create("legacy", "encoded", "기존", "건축대학", "건축학부", "010-3456-7890"));
        signup("202612345", "01034567890", "Signup123!")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.result.errors[0].fieldName").value("phone"));
        signup("202612345", "", "Signup123!")
                .andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true));
        signup("202612345", null, "Signup123!")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.result.errors[0].fieldName").value("studentId"));
    }

    @Test
    void signupEnforcesStudentIdAndNameBoundaries() throws Exception {
        signup("2026123456", null, "Signup123!", "김".repeat(21))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result.errors[?(@.fieldName == 'studentId')]").isNotEmpty())
                .andExpect(jsonPath("$.result.errors[?(@.fieldName == 'name')]").isNotEmpty());
        signup("20261234a", null, "Signup123!").andExpect(status().isBadRequest());
        signup("20261234", null, "Signup123!").andExpect(status().isBadRequest());
        signup("KL1234567", null, "Signup123!").andExpect(status().isBadRequest());
        signup("KL123456", null, "Signup123!").andExpect(status().isOk());
        signup("202612346", null, "Signup123!", "김".repeat(20))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    void concurrentRecoverySubmissionsCollapseButCorrectionsAndLaterInquiriesAreAccepted() throws Exception {
        String studentId = "repeat-inquiry";
        Map<String, String> inquiry = Map.of("studentId", studentId, "name", " 문의자 ", "major", "학과",
                "phone", "010-1234-5678");
        var workers = Executors.newFixedThreadPool(6);
        var ready = new CountDownLatch(6);
        var start = new CountDownLatch(1);
        var results = new ArrayList<Future<?>>();
        try {
            for (int i = 0; i < 6; i++) {
                results.add(workers.submit(() -> {
                    ready.countDown();
                    start.await();
                    submitInquiry(inquiry);
                    return null;
                }));
            }
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            for (var result : results) result.get(20, TimeUnit.SECONDS);
        } finally {
            workers.shutdownNow();
        }
        var normalized = new HashMap<>(inquiry);
        normalized.put("name", "문의자");
        normalized.put("phone", "01012345678");
        submitInquiry(normalized);
        org.junit.jupiter.api.Assertions.assertEquals(1, inquiryCount(studentId));
        normalized.put("major", "다른 학과");
        submitInquiry(normalized);
        org.junit.jupiter.api.Assertions.assertEquals(2, inquiryCount(studentId));
        db.update("UPDATE password_inquiry SET created_at = ? WHERE student_id = ?",
                java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(601)), studentId);
        submitInquiry(inquiry);
        org.junit.jupiter.api.Assertions.assertEquals(3, inquiryCount(studentId));
    }

    private void submitInquiry(Map<String, String> request) throws Exception {
        mvc.perform(post("/user/password-inquiries").contentType("application/json").content(mapper.writeValueAsBytes(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result").value(true));
    }

    private int inquiryCount(String studentId) {
        return db.queryForObject("SELECT COUNT(*) FROM password_inquiry WHERE student_id = ?", Integer.class, studentId);
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
