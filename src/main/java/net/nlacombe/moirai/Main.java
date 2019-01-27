package net.nlacombe.moirai;

import net.nlacombe.moirai.googlecalendar.GoogleCalendar;
import net.nlacombe.moirai.googlecalendar.GoogleCalendarClient;
import net.nlacombe.moirai.ical.IcalReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Properties;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) {
        String facebookCalendarName = getProperty("config.properties", "googleCalendar.targetCalendar.name");
        ZoneId facebookCalendarTimezone = ZoneId.of(getProperty("config.properties", "googleCalendar.targetCalendar.timezone"));
        String facebookIcalUrl = getProperty("secrets.properties", "facebook.ical.url");

        GoogleCalendarClient googleCalendarClient = new GoogleCalendarClient();
        var calendar = getOrCreateCalendar(googleCalendarClient, facebookCalendarName, facebookCalendarTimezone);

        var eventStream = IcalReader.readFromUrl(facebookIcalUrl, calendar.getTimezone());
        eventStream
                .filter(event -> event.getStart().isAfter(ZonedDateTime.now()))
                .sorted()
                .forEach(event -> logger.info(event.toString()));
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
            calendar = googleCalendarClient.createCalendar(calendarName, timezone);
        }

        return calendar;
    }

    private static void createEvent(GoogleCalendarClient googleCalendarClient) {
        ZonedDateTime start = ZonedDateTime.now().plusDays(1).withHour(10).withMinute(0);
        ZonedDateTime end = start.plusHours(1);
        String eventId = googleCalendarClient.createGoogleCalendarEvent("dat eve", start, end);

        logger.info("Event ID: " + eventId);
    }
}
