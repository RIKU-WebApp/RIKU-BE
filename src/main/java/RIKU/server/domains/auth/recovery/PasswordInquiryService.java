package RIKU.server.domains.auth.recovery;

import RIKU.server.domains.community.Feedback;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
class PasswordInquiryService {
    private static final Duration DUPLICATE_WINDOW = Duration.ofMinutes(10);
    private static final int SUBMISSION_LOCK_SLOTS = 64;
    private final PasswordInquiryRepository inquiries;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void submit(PasswordInquiryController.SubmitRequest request) {
        String studentId = request.studentId().trim();
        int slot = Math.floorMod(studentId.toLowerCase(Locale.ROOT).hashCode(), SUBMISSION_LOCK_SLOTS);
        inquiries.lockSubmissionSlot(slot);
        var inquiry = new PasswordInquiry(studentId, request.name(), request.major(), request.phone());
        if (!inquiries.existsByStudentIdAndNameAndMajorAndPhoneAndCreatedAtAfter(studentId,
                inquiry.getName(), inquiry.getMajor(), inquiry.getPhone(), inquiry.getCreatedAt().minus(DUPLICATE_WINDOW))) {
            inquiries.save(inquiry);
        }
    }

    @Transactional(readOnly = true)
    public Page<PasswordInquiry> list(int page) {
        return inquiries.findAll(PageRequest.of(Math.max(0, page), 20, Sort.by(Sort.Direction.DESC, "id")));
    }

    @Transactional
    public PasswordInquiry update(long id, Feedback.Status status) {
        PasswordInquiry inquiry = inquiries.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "비밀번호 문의를 찾을 수 없습니다."));
        inquiry.updateStatus(status);
        return inquiry;
    }
}
