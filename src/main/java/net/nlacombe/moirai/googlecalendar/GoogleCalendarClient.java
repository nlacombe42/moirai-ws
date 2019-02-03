package net.nlacombe.moirai.googlecalendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import net.nlacombe.moirai.domain.Event;
import net.nlacombe.moirai.domain.EventParticipation;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class GoogleCalendarClient {

    private static final String APPLICATION_NAME = "morai";
    private static final String CREDENTIALS_CLASSPATH = "/google-client-secret.json";
    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarClient.class);

    private Calendar googleCalendarApiClient;

    public GoogleCalendarClient() {
        InputStream credentialFileInputStream = getClass().getResourceAsStream(CREDENTIALS_CLASSPATH);
        List<String> scopes = Collections.singletonList(CalendarScopes.CALENDAR);

        googleCalendarApiClient = getGoogleCalendarApiClient(APPLICATION_NAME, credentialFileInputStream, scopes);
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

    private Calendar getGoogleCalendarApiClient(String applicationName, InputStream credentialInputStream, List<String> scopes) {
        try {
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = JacksonFactory.getDefaultInstance();

            return new Calendar.Builder(httpTransport, jsonFactory, getGoogleCredentials(jsonFactory, httpTransport, credentialInputStream, scopes))
                    .setApplicationName(applicationName)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Error creating google calendar api client", e);
        }
    }

    private Credential getGoogleCredentials(JsonFactory jsonFactory, NetHttpTransport httpTransport, InputStream credentialInputStream, List<String> scopes) throws IOException {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(credentialInputStream));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets, scopes)
                .setDataStoreFactory(new FileDataStoreFactory(new File("tokens")))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}
