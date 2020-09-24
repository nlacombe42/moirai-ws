package net.nlacombe.moirai.googlecalendar;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import net.nlacombe.commonlib.stream.PageIterator;
import net.nlacombe.commonlib.stream.StreamUtil;
import net.nlacombe.moirai.domain.Event;
import net.nlacombe.moirai.domain.EventParticipation;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class GoogleCalendarClient {

    private static final String APPLICATION_NAME = "morai";
    private static final String CREDENTIALS_CLASSPATH = "/google-client-secret.json";
    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarClient.class);

    private Calendar googleCalendarApiClient;

    public GoogleCalendarClient(String googleUserAccessToken, String googleUserRefreshToken) {
        InputStream clientSecretCredentialInputStream = getClass().getResourceAsStream(CREDENTIALS_CLASSPATH);

        googleCalendarApiClient = getGoogleCalendarApiClient(APPLICATION_NAME, clientSecretCredentialInputStream,
                googleUserAccessToken, googleUserRefreshToken);
    }

    public String getPrimaryCalendarEmail() {
        try {
            var calendar = googleCalendarApiClient.calendarList().get("primary").execute();

            if (!calendar.getId().endsWith("@gmail.com"))
                throw new RuntimeException("Primary Google Calendar email not found.");

            return calendar.getId();

        } catch (IOException e) {
            throw new RuntimeException("Error calling google calendar api.", e);
        }
    }

    public GoogleCalendar getCalendarByName(String calendarName) {
        try {
            var calendars = googleCalendarApiClient.calendarList().list().execute().getItems();

            return calendars.stream()
                    .map(this::toGoogleCalendar)
                    .filter(calendar -> calendarName.equals(calendar.getName()))
                    .findAny()
                    .orElse(null);

        } catch (IOException e) {
            throw new RuntimeException("Error calling google calendar api.", e);
        }
    }

    public GoogleCalendar createCalendar(String calendarName, ZoneId timezone) {
        try {
            com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
            calendar.setSummary(calendarName);
            calendar.setTimeZone(timezone.getId());

            calendar = googleCalendarApiClient.calendars().insert(calendar).execute();

            return toGoogleCalendar(calendar);

        } catch (IOException e) {
            throw new RuntimeException("Error calling google calendar api.", e);
        }
    }

    public GoogleEvent getEventFromIcalUid(String calendarId, String icalUid) {
        try {
            return googleCalendarApiClient.events().list(calendarId)
                    .setICalUID(icalUid)
                    .setMaxResults(1)
                    .setShowDeleted(true)
                    .setSingleEvents(true)
                    .execute()
                    .getItems()
                    .stream()
                    .map(this::toEvent)
                    .findAny()
                    .orElse(null);
        } catch (IOException e) {
            throw new RuntimeException("Error calling google calendar api.", e);
        }
    }

    public Stream<GoogleEvent> getAllEvents(String calendarId) {
        var pageIterator = new PageIterator<com.google.api.services.calendar.model.Event>() {

            private String pageToken = null;
            private boolean firstPage = true;

            @Override
            public boolean hasNext() {
                return firstPage || pageToken != null;
            }

            @Override
            public List<com.google.api.services.calendar.model.Event> next() {
                try {
                    var response = googleCalendarApiClient.events().list(calendarId)
                            .setPageToken(pageToken)
                            .setMaxResults(250)
                            .setShowDeleted(false)
                            .setSingleEvents(true)
                            .execute();

                    pageToken = response.getNextPageToken();
                    firstPage = false;

                    return response.getItems();
                } catch (IOException e) {
                    throw new RuntimeException("Error calling google calendar api.", e);
                }
            }
        };

        return StreamUtil.createStream(pageIterator)
                .map(this::toEvent);
    }

    public String createEvent(String calendarId, Event event) {
        try {
            var googleEvent = getGoogleEvent(getPrimaryCalendarEmail(), event);

            googleEvent = googleCalendarApiClient.events().insert(calendarId, googleEvent).execute();

            return googleEvent.getId();
        } catch (IOException e) {
            throw new RuntimeException("Error calling google calendar api.", e);
        }
    }

    public void updateEvent(String calendarId, GoogleEvent event) {
        try {
            var googleEvent = getGoogleEvent(getPrimaryCalendarEmail(), event);
            googleEvent.setId(event.getGoogleEventId());

            googleCalendarApiClient.events().update(calendarId, googleEvent.getId(), googleEvent).execute();
        } catch (IOException e) {
            throw new RuntimeException("Error calling google calendar api.", e);
        }
    }

    public void removeEvent(String calendarId, String googleEventId) {
        try {
            googleCalendarApiClient.events().delete(calendarId, googleEventId).execute();
        } catch (IOException e) {
            throw new RuntimeException("Error calling google calendar api.", e);
        }
    }

    private com.google.api.services.calendar.model.Event getGoogleEvent(String userEmailAddress, Event event) {
        var googleEventAttendee = new EventAttendee();
        googleEventAttendee.setSelf(true);
        googleEventAttendee.setResponseStatus(event.getParticipation().getGoogleCalendarCode());
        googleEventAttendee.setEmail(userEmailAddress);

        com.google.api.services.calendar.model.Event googleEvent = new com.google.api.services.calendar.model.Event();
        googleEvent.setICalUID(event.getIcalUid());
        googleEvent.setSummary(event.getName());
        googleEvent.setDescription(event.getDescription());
        googleEvent.setStart(toEventDateTime(event.getStart()));
        googleEvent.setEnd(toEventDateTime(event.getEnd()));
        googleEvent.setLocation(event.getLocation());
        googleEvent.setAttendees(Collections.singletonList(googleEventAttendee));
        googleEvent.setStatus("confirmed");

        return googleEvent;
    }

    private GoogleEvent toEvent(com.google.api.services.calendar.model.Event googleEvent) {
        var event = new GoogleEvent();
        event.setGoogleEventId(googleEvent.getId());
        event.setIcalUid(googleEvent.getICalUID());
        event.setName(googleEvent.getSummary());
        event.setStart(toZonedDateTime(googleEvent.getStart()));
        event.setEnd(toZonedDateTime(googleEvent.getEnd()));
        event.setLocation(googleEvent.getLocation());
        event.setDescription(googleEvent.getDescription());
        event.setParticipation(toEventParticipation(googleEvent.getAttendees()));

        return event;
    }

    private EventParticipation toEventParticipation(List<EventAttendee> attendees) {
        if (CollectionUtils.isEmpty(attendees))
            return EventParticipation.NEEDS_ACTION;

        return attendees.stream()
                .filter(EventAttendee::isSelf)
                .findAny()
                .map(attendee -> EventParticipation.fromGoogleCalendarCode(attendee.getResponseStatus()))
                .orElse(EventParticipation.NEEDS_ACTION);
    }

    private ZonedDateTime toZonedDateTime(EventDateTime eventDateTime) {
        return ZonedDateTime.parse(eventDateTime.getDateTime().toStringRfc3339());
    }

    private GoogleCalendar toGoogleCalendar(com.google.api.services.calendar.model.Calendar calendar) {
        return new GoogleCalendar(calendar.getId(), calendar.getSummary(), ZoneId.of(calendar.getTimeZone()));
    }

    private GoogleCalendar toGoogleCalendar(CalendarListEntry calendarListEntry) {
        return new GoogleCalendar(calendarListEntry.getId(), calendarListEntry.getSummary(), ZoneId.of(calendarListEntry.getTimeZone()));
    }

    private EventDateTime toEventDateTime(ZonedDateTime zonedDateTime) {
        DateTimeFormatter rfc3339DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

        DateTime googleDatetime = DateTime.parseRfc3339(zonedDateTime.format(rfc3339DateTimeFormatter));
        EventDateTime eventDateTime = new EventDateTime();
        eventDateTime.setDateTime(googleDatetime);

        return eventDateTime;
    }

    private Calendar getGoogleCalendarApiClient(String applicationName, InputStream credentialInputStream,
                                                String googleUserAccessToken, String googleUserRefreshToken) {
        try {
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = JacksonFactory.getDefaultInstance();
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(credentialInputStream));
            var googleCredentials = new GoogleCredential.Builder()
                    .setJsonFactory(jsonFactory)
                    .setTransport(httpTransport)
                    .setClientSecrets(clientSecrets)
                    .build()
                    .setAccessToken(googleUserAccessToken)
                    .setRefreshToken(googleUserRefreshToken);

            return new Calendar.Builder(httpTransport, jsonFactory, googleCredentials)
                    .setApplicationName(applicationName)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Error creating google calendar api client", e);
        }
    }
}
