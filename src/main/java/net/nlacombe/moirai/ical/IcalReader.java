package net.nlacombe.moirai.ical;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.nlacombe.moirai.domain.Event;
import net.nlacombe.moirai.domain.EventParticipation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public class IcalReader {

    public static List<Event> readFromUrl(String icalUrl, ZoneId timeZone) {
        var icalInputStream = getHttpInputStream(icalUrl);
        var calendar = getCalendar(icalInputStream);

        return calendar.getComponents().stream()
                .filter(component -> component instanceof VEvent)
                .map(component -> (VEvent) component)
                .map(icalEvent -> toEvent(timeZone, icalEvent))
                .collect(Collectors.toList());
    }

    private static Event toEvent(ZoneId timeZone, VEvent icalEvent) {
        var icalPartstatCode = icalEvent.getProperty("PARTSTAT").getValue();

        var event = new Event();
        event.setIcalUid(icalEvent.getUid().getValue());
        event.setName(icalEvent.getSummary().getValue());
        event.setDescription(icalEvent.getDescription().getValue());
        event.setLocation(icalEvent.getLocation() != null ? icalEvent.getLocation().getValue() : null);
        event.setStart(icalEvent.getStartDate().getDate().toInstant().atZone(timeZone));
        event.setEnd(icalEvent.getEndDate().getDate().toInstant().atZone(timeZone));
        event.setParticipation(EventParticipation.fromIcalCode(icalPartstatCode));

        return event;
    }

    private static Calendar getCalendar(InputStream icalInputStream) {
        try {
            return new CalendarBuilder().build(icalInputStream);
        } catch (IOException | ParserException e) {
            throw new RuntimeException("Error reading or parsing calendar from ICal URL (must be an URL to an ICS file)", e);
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
