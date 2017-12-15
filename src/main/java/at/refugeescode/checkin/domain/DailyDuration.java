package at.refugeescode.checkin.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDate;

@AllArgsConstructor
@Getter
public class DailyDuration implements OverviewDuration {

    protected LocalDate date;
    protected Duration duration;
}
