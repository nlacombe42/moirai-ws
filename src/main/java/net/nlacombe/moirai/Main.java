package net.nlacombe.moirai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) {
        GoogleCalendarService googleCalendarService = new GoogleCalendarService();
        googleCalendarService.listextEvents("primary", 10);
    }

    private static void createEvent() {
        GoogleCalendarService googleCalendarService = new GoogleCalendarService();
        ZonedDateTime start = ZonedDateTime.now().plusDays(1).withHour(10).withMinute(0);
        ZonedDateTime end = start.plusHours(1);
        String eventId = googleCalendarService.createGoogleCalendarEvent("dat eve", start, end);

        logger.info("Event ID: " + eventId);
    }
}
