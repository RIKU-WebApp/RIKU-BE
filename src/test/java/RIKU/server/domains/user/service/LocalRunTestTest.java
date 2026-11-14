package RIKU.server.domains.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class LocalRunTestTest {
    @TempDir Path directory;
    @Test void requiresAllowedMemberAndOriginalBrowserAndConsumesOnce() throws Exception {
        var file = directory.resolve("lab.properties");
        var lab = new LocalRunTest(file.toString());
        String origin = "https://test-run.trycloudflare.com";
        String verifier = "v".repeat(43);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        assertNull(lab.originFor("allowed"));
        Files.writeString(file, "origin="+origin+"\ntesters=allowed\nexpires=2099-01-01T00:00:00Z");
        assertNull(lab.issue("other",challenge));
        String ticket = lab.issue("allowed",challenge).split("#")[1];
        assertFalse(lab.redeem(ticket,"x".repeat(43),origin));
        assertFalse(lab.redeem(ticket,verifier,"https://other.trycloudflare.com"));
        assertTrue(lab.redeem(ticket,verifier,origin));
        assertFalse(lab.redeem(ticket,verifier,origin));
        String pending = lab.issue("allowed",challenge).split("#")[1];
        Files.delete(file);
        assertFalse(lab.redeem(pending,verifier,origin));
    }
}
