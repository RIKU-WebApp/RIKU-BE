package RIKU.server.domains.point.service;

import RIKU.server.domains.season.SeasonProvider;
import RIKU.server.domains.point.dto.request.ReadUserEventRankingRequest;
import RIKU.server.domains.point.dto.response.ReadUserParticipationsResponse;
import RIKU.server.domains.point.dto.response.ReadUserRankingResponse;
import RIKU.server.domains.point.ranking.RankingCache;
import RIKU.server.domains.point.ranking.RankingQuery;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.domains.user.exception.UserException;
import RIKU.server.security.AuthMember;
import RIKU.server.global.response.BaseResponseStatus;
import RIKU.server.global.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@RequiredArgsConstructor
// Cache misses must not wait for another connection while holding a caller transaction.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class UserPointService {
    private final UserRepository userRepository;
    private final RankingCache rankingCache;
    private final SeasonProvider seasonProvider;
    private final UserActivityService userActivityService;

    public ReadUserRankingResponse getUserPointRanking(AuthMember authMember) {
        validateUser(authMember);
        return rankingCache.get(RankingQuery.season(seasonProvider.seasonStartUtc())).forUser(authMember.getId());
    }

    public ReadUserRankingResponse getUserEventPointRanking(ReadUserEventRankingRequest request, AuthMember authMember) {
        validateUser(authMember);
        var query = new RankingQuery(DateTimeUtils.startOfDayUtc(request.getStartDate()),
                DateTimeUtils.startOfDayUtc(request.getEndDate().plusDays(1)), Set.copyOf(request.getPointTypes()));
        return rankingCache.get(query).forUser(authMember.getId());
    }

    public ReadUserParticipationsResponse getParticipations(AuthMember authMember) {
        var fromUtc = seasonProvider.seasonStartUtc();
        var ranking = rankingCache.get(RankingQuery.season(fromUtc)).activityRank(authMember.getId());
        return userActivityService.getParticipations(authMember.getId(), fromUtc, ranking);
    }

    private void validateUser(AuthMember authMember) {
        if (!userRepository.existsById(authMember.getId())) {
            throw new UserException(BaseResponseStatus.USER_NOT_FOUND);
        }
    }
}
