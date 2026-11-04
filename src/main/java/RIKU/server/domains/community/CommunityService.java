package RIKU.server.domains.community;

import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.participant.entity.ParticipantStatus;
import RIKU.server.domains.run.post.entity.PostStatus;
import RIKU.server.domains.run.recruitment.RunRecruitmentRepository;
import RIKU.server.global.entity.BaseStatus;
import RIKU.server.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class CommunityService {
    private final UserRepository users;
    private final ParticipantRepository participants;
    private final RunRecruitmentRepository recruitment;
    private final PersonalRunRecordRepository records;
    private final BadgeAwardRepository awards;
    public record Badge(String code,String name,String description,String source) {}
    public record PublicProfile(String name,List<Badge> badges) {}
    public record MyActivity(BigDecimal verifiedKm,BigDecimal personalKm,List<Badge> badges,List<PersonalRunRecord> records) {}

    public PublicProfile profile(Long id) {
        var user=users.findById(id).filter(u->u.getStatus()==BaseStatus.ACTIVE)
                .orElseThrow(()->new ResponseStatusException(NOT_FOUND,"회원을 찾을 수 없습니다."));
        return new PublicProfile(user.getName(),activity(id).badges());
    }
    public MyActivity activity(Long id) {
        var personal=records.findByUserIdOrderByDateDescIdDesc(id);
        BigDecimal personalKm=personal.stream().map(PersonalRunRecord::getDistanceKm).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal verified=BigDecimal.ZERO;
        var attended=participants.findAttendedForBadges(id,ParticipantStatus.ATTENDED,PostStatus.CLOSED,BaseStatus.ACTIVE);
        var configs=attended.isEmpty() ? List.<RIKU.server.domains.run.recruitment.RunRecruitment>of()
                : recruitment.findWithGroupsByPostIds(attended.stream().map(p->p.getPost().getId()).distinct().toList());
        var byPost=new HashMap<Long,RIKU.server.domains.run.recruitment.RunRecruitment>();
        configs.forEach(c->byPost.put(c.getPostId(),c));
        for(var p:attended) {
            var c=byPost.get(p.getPost().getId());
            if(c!=null) verified=verified.add(c.getGroups().stream().filter(g->g.getName().equals(p.getGroup()))
                    .map(RIKU.server.domains.run.recruitment.RunRecruitment.GroupPlan::getDistanceKm).findFirst().orElse(BigDecimal.ZERO));
        }
        List<Badge> badges=new ArrayList<>();
        for(int km:new int[]{10,50,100,300,500,1000}) {
            if(verified.compareTo(BigDecimal.valueOf(km))>=0)
                badges.add(new Badge("CLUB_"+km,km+"km 함께 달리기","출석이 확인된 공식 러닝 누적 "+km+"km","CLUB"));
            if(personalKm.compareTo(BigDecimal.valueOf(km))>=0)
                badges.add(new Badge("PERSONAL_"+km,km+"km 나의 발자국","직접 입력한 개인 러닝 누적 "+km+"km","PERSONAL"));
        }
        awards.findByUserIdOrderByIdAsc(id).forEach(a->badges.add(new Badge("AWARD_"+a.getId(),a.getName(),a.getDescription(),"ADMIN")));
        return new MyActivity(verified,personalKm,badges,personal);
    }
    @Transactional public PersonalRunRecord record(AuthMember actor,CommunityController.RecordRequest r) {
        return records.save(new PersonalRunRecord(actor.getId(),r.date(),r.distanceKm(),r.minutes(),r.title().trim()));
    }
    @Transactional public void deleteRecord(AuthMember actor,Long id) {
        var r=records.findById(id).filter(x->x.getUserId().equals(actor.getId()))
                .orElseThrow(()->new ResponseStatusException(NOT_FOUND,"내 기록을 찾을 수 없습니다."));
        records.delete(r);
    }
    @Transactional public void award(AuthMember actor,Long id,CommunityController.AwardRequest r) {
        admin(actor);
        if(!users.existsById(id)) throw new ResponseStatusException(NOT_FOUND,"회원을 찾을 수 없습니다.");
        if(awards.existsByUserIdAndName(id,r.name().trim())) throw new ResponseStatusException(CONFLICT,"이미 수여한 배지입니다.");
        awards.save(new BadgeAward(id,r.name().trim(),r.description(),actor.getId()));
    }
    private void admin(AuthMember actor) { if(!actor.isAdmin()) throw new ResponseStatusException(FORBIDDEN,"관리자 권한이 필요합니다."); }
}
