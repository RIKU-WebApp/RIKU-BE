package RIKU.server.domains.admin.renewal;

import RIKU.server.domains.user.entity.UserRole;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.global.entity.BaseStatus;
import RIKU.server.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterRenewalService {
    private final SemesterRenewalRepository renewals;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<String> confirmedStudents(AuthMember admin, String term) {
        validate(admin, term);
        return renewals.confirmedStudents(term);
    }

    @Transactional
    public UserRole confirm(AuthMember admin, String term, String studentId) {
        validate(admin, term);
        var member = users.findForRenewal(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        if (member.getStatus() != BaseStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "비활성 회원은 연장을 확정할 수 없습니다.");
        }
        if (!renewals.existsByTermAndMember_Id(term, member.getId())) {
            renewals.save(new SemesterRenewal(term, member, admin.getId()));
            if (member.getUserRole() == UserRole.NEW_MEMBER) {
                member.updateUserRole(UserRole.MEMBER);
            }
        }
        return member.getUserRole();
    }

    private void validate(AuthMember admin, String term) {
        if (!admin.isAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (!term.matches("20[0-9]{2}-[12]")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "연장할 학기를 선택해 주세요.");
        }
    }
}
