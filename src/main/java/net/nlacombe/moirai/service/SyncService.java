package net.nlacombe.moirai.service;

import net.nlacombe.moirai.domain.Event;
import net.nlacombe.moirai.googlecalendar.GoogleCalendar;
import net.nlacombe.moirai.googlecalendar.GoogleCalendarClient;
import net.nlacombe.moirai.ical.IcalReader;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SyncService {

    private static final Logger logger = LoggerFactory.getLogger(SyncService.class);

    private EmailSenderService emailSenderService;
    private String targetCalendarName;
    private String targetCalendarDefaultTimezoneText;

    @Inject
    public SyncService(EmailSenderService emailSenderService,
                       @Value("${targetCalendar.name}") String targetCalendarName,
                       @Value("${targetCalendar.defaultTimezone}") String targetCalendarDefaultTimezoneText) {

        this.emailSenderService = emailSenderService;
        this.targetCalendarName = targetCalendarName;
        this.targetCalendarDefaultTimezoneText = targetCalendarDefaultTimezoneText;
    }

    public void sync(String sourceCalendarIcalUrl, String googleUserAccessToken, String googleUserRefreshToken) {
        try {
            var targetCalendarDefaultTimezone = ZoneId.of(targetCalendarDefaultTimezoneText);

            logger.info("Syncing from ICal URL \"" + sourceCalendarIcalUrl + "\" to Google Calendar with name \"" + targetCalendarName + "\".");

            var googleCalendarClient = new GoogleCalendarClient(googleUserAccessToken, googleUserRefreshToken);
            var calendar = getOrCreateCalendar(googleCalendarClient, targetCalendarName, targetCalendarDefaultTimezone);

            logger.info("Reading from source URL...");
            var sourceEvents = IcalReader.readFromUrl(sourceCalendarIcalUrl, calendar.getTimezone());

            logger.info("Found " + sourceEvents.size() + " future source events to sync.");
            logger.info("Primary google calendar email: " + googleCalendarClient.getPrimaryCalendarEmail());

            logger.info("Sycing to Google Calendar...");
            syncGoogleCalendarWithIcalEvents(googleCalendarClient, calendar, sourceEvents);
            logger.info("Done.");
        } catch (Exception e) {
            emailSenderService.sendErrorEmail(e);
            throw e;
        }
    }

    private static void syncGoogleCalendarWithIcalEvents(GoogleCalendarClient googleCalendarClient, GoogleCalendar calendar, List<Event> sourceEvents) {
        var sourceEventsByIcalUid = sourceEvents.stream().collect(Collectors.toMap(Event::getIcalUid, Function.identity()));

        sourceEvents.stream()
                .filter(event -> event.getStart().isAfter(ZonedDateTime.now()))
                .sorted()
                .forEach(event -> updateOrCreateEventInGoogleCalendar(googleCalendarClient, calendar, event));

        googleCalendarClient.getAllEvents(calendar.getCalendarId())
                .filter(event -> !sourceEventsByIcalUid.containsKey(event.getIcalUid()))
                .forEach(event -> googleCalendarClient.removeEvent(calendar.getCalendarId(), event.getGoogleEventId()));
    }

    private static void updateOrCreateEventInGoogleCalendar(GoogleCalendarClient googleCalendarClient, GoogleCalendar calendar, Event icalEvent) {
        var existingGoogleEvent = googleCalendarClient.getEventFromIcalUid(calendar.getCalendarId(), icalEvent.getIcalUid());

        if (existingGoogleEvent == null) {
            googleCalendarClient.createEvent(calendar.getCalendarId(), icalEvent);
        } else {
            if (existingGoogleEvent.equals(icalEvent))
                return;

            copyBeanProperties(icalEvent, existingGoogleEvent);
            googleCalendarClient.updateEvent(calendar.getCalendarId(), existingGoogleEvent);
        }
    }

    private static <SourceType, TargetType> void copyBeanProperties(SourceType sourceBean, TargetType targetBean) {
        try {
            BeanUtils.copyProperties(targetBean, sourceBean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static GoogleCalendar getOrCreateCalendar(GoogleCalendarClient googleCalendarClient, String calendarName, ZoneId timezone) {
        var calendar = googleCalendarClient.getCalendarByName(calendarName);

        if (calendar == null) {
            logger.info("Creating google calendar with name \"" + calendarName + "\" and timezone \"" + timezone + "\"");

            calendar = googleCalendarClient.createCalendar(calendarName, timezone);
        }

        return calendar;
    }
}
