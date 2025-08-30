package RIKU.server.Config;

import RIKU.server.Util.DateTimeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class SeasonProvider {

    @Value("${season.start-date}")
    private LocalDate seasonStartDate;

    public LocalDateTime seasonStartUtc() {
        return DateTimeUtils.startOfDayUtc(seasonStartDate);
    }

}
