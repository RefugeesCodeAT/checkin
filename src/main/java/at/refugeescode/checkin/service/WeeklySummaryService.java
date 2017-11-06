package at.refugeescode.checkin.service;

import at.refugeescode.checkin.domain.Checkin;
import at.refugeescode.checkin.domain.CheckinRepository;
import at.refugeescode.checkin.domain.Person;
import at.refugeescode.checkin.domain.PersonRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class WeeklySummaryService {

    @NonNull
    private final PersonRepository personRepository;
    @NonNull
    private final CheckinRepository checkinRepository;
    @NonNull
    private final MailService mailService;

    @Value("${checkin.mail.trainer}")
    private String trainer;
    @Value("${checkin.mail.webmaster}")
    private String webmaster;
    @Value("${checkin.mail.weekly}")
    private String weekly;

    private static final EmailValidator emailValidator = new EmailValidator();

    @Scheduled(cron = "${checkin.mail.weekly}")
    public void sendWeekyMail() {
        log.info("Sending weekly mails");

        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfLastWeek = today.minusDays(7).atStartOfDay();

        StringBuilder overallSummaryMessageBuilder = new StringBuilder();
        overallSummaryMessageBuilder.append("Hello Trainer!<br/><br/>Here is our weekly summary:<br/>");

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy HH:mm");
        String formattedStartOfToday = dateTimeFormatter.format(startOfToday);
        String formattedStartOfLastWeek = dateTimeFormatter.format(startOfLastWeek);

        for (Person person : personRepository.findAll()) {

            Duration total = Duration.ZERO;
            List<Checkin> checkins = checkinRepository.findByPersonAndCheckedInTrueOrderByTime(person);
            for (Checkin checkin : checkins) {
                if (checkin.getTime().isAfter(startOfLastWeek) && !checkin.getTime().isAfter(startOfToday))
                    total = total.plus(checkin.getDuration());
            }

            String personalMessage = String.format("Hello %s!<br/><br/>" +
                            "Another week has passed and we're happy to share with you how much time you were present!<br/>" +
                            "You have been checked in for %s hours, in the week from %s until %s.<br/>" +
                            "Happy Coding and see you next week!<br/><br/>" +
                            "Your refugees{code}-Team",
                    person.getName(),
                    formatDuration(total),
                    formattedStartOfLastWeek,
                    formattedStartOfToday
            );

            String summaryMessage = String.format("%s has been checked in for %s",
                    person.getName(),
                    formatDuration(total)
            );

            overallSummaryMessageBuilder.append(summaryMessage).append("<br/>");

            //send mail to user with summary of hours during the last week
            if (person.getEmail() != null && emailValidator.isValid(person.getEmail(), null)) {
                mailService.sendMail(person.getEmail(), null, webmaster,
                        "Your RefugeesCode Check-in Weekly Summary",
                        personalMessage);
            }
        }

        overallSummaryMessageBuilder.append(String.format("in the week from %s until %s.",
                formattedStartOfLastWeek,
                formattedStartOfToday
        ));

        String overallSummaryMessage = overallSummaryMessageBuilder.toString();

        log.info("{}", overallSummaryMessage);

        //send mail to admin with summary of hours during the last week for all users
        mailService.sendMail(trainer, null, null, "RefugeesCode Check-in Summary", overallSummaryMessage);

    }

    private static long ceilMinutes(Duration duration) {
        if (duration.getSeconds() % 60 != 0 || duration.getNano() != 0)
            return duration.toMinutes() + 1;
        else
            return duration.toMinutes();
    }

    private static String formatDuration(Duration duration) {
        duration = Duration.ofMinutes(ceilMinutes(duration));
        long hoursPart = duration.toHours();
        long minutesPart = duration.minusHours(hoursPart).toMinutes();
        return String.format("%d:%02d", hoursPart, minutesPart);
    }
}
