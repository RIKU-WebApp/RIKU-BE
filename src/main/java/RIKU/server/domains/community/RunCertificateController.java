package RIKU.server.domains.community;

import RIKU.server.global.response.BaseResponse;
import RIKU.server.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/user/certificates")
@RequiredArgsConstructor
public class RunCertificateController {
    private final RunCertificateRepository certificates;
    private final BadgeStorage storage;

    @GetMapping
    public BaseResponse<List<RunCertificateRepository.Summary>> list(@AuthenticationPrincipal AuthMember member) {
        return new BaseResponse<>(certificates.findByUserIdOrderByIdDesc(member.getId()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<Long> save(@AuthenticationPrincipal AuthMember member, @RequestParam String title,
                                   @RequestParam LocalDate date, @RequestParam MultipartFile image,
                                   @RequestParam(required = false) BigDecimal distanceKm) throws IOException {
        String name = title.strip();
        if (name.isEmpty() || name.length() > 120 || image.getSize() < 4 || image.getSize() > 2 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (distanceKm != null && (distanceKm.signum() < 0 || distanceKm.compareTo(new BigDecimal("99999.99")) > 0
                || distanceKm.stripTrailingZeros().scale() > 2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거리(km)를 확인해 주세요.");
        }
        byte[] bytes = image.getBytes();
        if (bytes[0] != (byte) 0xff || bytes[1] != (byte) 0xd8 || bytes[2] != (byte) 0xff) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return new BaseResponse<>(certificates.save(new RunCertificate(member.getId(), name, date, distanceKm, bytes)).id());
    }

    @GetMapping(value = "/{id}/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal AuthMember member, @PathVariable Long id) {
        return ResponseEntity.of(certificates.findImage(id, member.getId()));
    }

    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthMember member, @PathVariable Long id) {
        if(certificates.deleteOwned(id, member.getId()) != 1) return ResponseEntity.notFound().build();
        storage.rememberCertificate(member.getId());
        return ResponseEntity.noContent().build();
    }
}
