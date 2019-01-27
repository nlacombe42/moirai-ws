package net.nlacombe.moirai;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

public class IcalReader {

    private static final Logger logger = LoggerFactory.getLogger(IcalReader.class);

    public static void readFromUrl(String icalUrl) {
        var icalInputStream = getHttpInputStream(icalUrl);

        try {
            Calendar calendar = new CalendarBuilder().build(icalInputStream);

            calendar.getComponents().stream()
                    .filter(component -> component instanceof VEvent)
                    .map(component -> (VEvent) component)
                    .filter(event -> event.getStartDate().getDate().toInstant().isAfter(Instant.now()))
                    .sorted((eventl, eventr) -> {
                        var eventLStartDate = eventl.getStartDate().getDate().toInstant();
                        var eventRStartDate = eventr.getStartDate().getDate().toInstant();

                        return eventLStartDate.compareTo(eventRStartDate);
                    })
                    .forEach(event -> {
                        logger.debug("Event summary: " + event.getSummary());
                        logger.debug("Event start: " + event.getStartDate().getDate());
                        logger.debug("Event end: " + event.getEndDate().getDate());
                        logger.debug("Location: " + event.getLocation());
                    });
        } catch (IOException | ParserException e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream getHttpInputStream(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

            return client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
