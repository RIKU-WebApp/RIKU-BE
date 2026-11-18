package RIKU.server.domains.user.controller;

import RIKU.server.domains.user.service.LocalRunTest;
import RIKU.server.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
public class LocalRunTestController {
    private final LocalRunTest lab;
    public record Start(String challenge) {}
    public record Launch(String url) {}
    public record Exchange(String code, String verifier, String origin) {}

    @PostMapping("/user/local-run-test/ticket")
    public ResponseEntity<Launch> issue(@AuthenticationPrincipal AuthMember member, @RequestBody Start start) {
        String url = member.isEnabled() ? lab.issue(member.getUsername(), start.challenge()) : null;
        if (url == null) return ResponseEntity.status(403).build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Launch(url));
    }

    @PostMapping("/public/local-run-test/redeem")
    public ResponseEntity<Void> redeem(@RequestBody Exchange exchange) {
        return ResponseEntity.status(lab.redeem(exchange.code(), exchange.verifier(), exchange.origin()) ? 204 : 403)
                .cacheControl(CacheControl.noStore()).build();
    }
}
