package RIKU.server.domains.user.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalRunTest {
    private final Path settings;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Ticket> tickets = new HashMap<>();
    private record Ticket(String student, String challenge, String origin, Instant expires) {}
    private record Lab(String origin, Set<String> testers, Instant expires) {}
    private static final Lab CLOSED = new Lab("", Set.of(), Instant.EPOCH);

    public LocalRunTest(@Value("${riku.local-run-test.file:/etc/RIKU/local-run-test.properties}") String file) {
        settings = Path.of(file);
    }

    // A missing, expired or incomplete operator configuration closes the test.
    private Lab readLab() {
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(settings)) {
            props.load(reader);
            String origin = props.getProperty("origin", "");
            Instant expires = Instant.parse(props.getProperty("expires", ""));
            if (!origin.matches("https://[a-z0-9-]+[.]trycloudflare[.]com") || !expires.isAfter(Instant.now())) return CLOSED;
            return new Lab(origin, Set.of(props.getProperty("testers", "").strip().split("\\s*,\\s*")), expires);
        } catch (IOException | RuntimeException invalid) {
            return CLOSED;
        }
    }

    public String originFor(String student) {
        var lab = readLab();
        return lab.testers.contains(student) ? lab.origin : null;
    }

    public synchronized String issue(String student, String challenge) {
        String origin = originFor(student);
        if (origin == null || challenge == null || !challenge.matches("[A-Za-z0-9_-]{43}")) return null;
        Instant now = Instant.now();
        tickets.values().removeIf(t -> t.student.equals(student) || !t.expires.isAfter(now));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(code, new Ticket(student, challenge, origin, now.plusSeconds(60)));
        return origin + "/enter#" + code;
    }

    public synchronized boolean redeem(String code, String verifier, String origin) {
        if (code == null || verifier == null || !verifier.matches("[A-Za-z0-9_-]{43}")) return false;
        var ticket = tickets.get(code);
        if (ticket == null || !ticket.expires.isAfter(Instant.now()) || !ticket.origin.equals(origin)
                || !ticket.origin.equals(originFor(ticket.student))) return false;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            if (!ticket.challenge.equals(Base64.getUrlEncoder().withoutPadding().encodeToString(digest))) return false;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        tickets.remove(code);
        return true;
    }
}
