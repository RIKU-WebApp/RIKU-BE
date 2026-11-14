package RIKU.server.domains.auth.recovery;

import RIKU.server.domains.community.Feedback;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
class PasswordInquiryService {
    private final PasswordInquiryRepository inquiries;

    @Transactional
    public void submit(PasswordInquiryController.SubmitRequest request) {
        inquiries.save(new PasswordInquiry(request.studentId(), request.name(), request.major(), request.phone()));
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
