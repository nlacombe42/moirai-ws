package net.nlacombe.moirai;

import net.nlacombe.moirai.domain.Event;
import net.nlacombe.moirai.googlecalendar.GoogleCalendar;
import net.nlacombe.moirai.googlecalendar.GoogleCalendarClient;
import net.nlacombe.moirai.ical.IcalReader;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) {
        var sourceCalendarIcalUrl = getProperty("secrets.properties", "sourceCalendar.ical.url");
        var targetCalendarName = getProperty("config.properties", "targetCalendar.name");
        var targetCalendarDefaultTimezone = ZoneId.of(getProperty("config.properties", "targetCalendar.defaultTimezone"));

        logger.info("Syncing from ICal URL \"" + sourceCalendarIcalUrl + "\" to Google Calendar with name \"" + targetCalendarName + "\".");

        var googleCalendarClient = new GoogleCalendarClient();
        var calendar = getOrCreateCalendar(googleCalendarClient, targetCalendarName, targetCalendarDefaultTimezone);

        logger.info("Reading from source URL...");
        var sourceEvents = IcalReader.readFromUrl(sourceCalendarIcalUrl, calendar.getTimezone());

        logger.info("Found " + sourceEvents.size() + " future source events to sync.");
        logger.info("Primary google calendar email: " + googleCalendarClient.getPrimaryCalendarEmail());

        logger.info("Sycing to Google Calendar...");
        syncGoogleCalendarWithIcalEvents(googleCalendarClient, calendar, sourceEvents);
        logger.info("Done.");
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

    private static String getProperty(String fileName, String propertyName) {
        try {
            Properties properties = new Properties();
            properties.load(Main.class.getResourceAsStream("/" + fileName));

            return properties.getProperty(propertyName);
        } catch (IOException e) {
            throw new RuntimeException("Could not read property \"" + propertyName + "\" from classpath file \"" + fileName + "\".");
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
