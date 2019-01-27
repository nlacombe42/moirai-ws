package net.nlacombe.moirai;

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
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "morai";
    private static final String CREDENTIALS_CLASSPATH = "/google-client-secret.json";
    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);

    private Calendar googleCalendarApiClient;

    public GoogleCalendarService() {
        InputStream credentialFileInputStream = getClass().getResourceAsStream(CREDENTIALS_CLASSPATH);
        List<String> scopes = Collections.singletonList(CalendarScopes.CALENDAR);

        googleCalendarApiClient = getGoogleCalendarApiClient(APPLICATION_NAME, credentialFileInputStream, scopes);
    }

    public String createGoogleCalendarEvent(String summary, ZonedDateTime start, ZonedDateTime end) {
        try {
            Event event = new Event();
            event.setSummary(summary);
            event.setDescription("description");
            event.setStart(toEventDateTime(start));
            event.setEnd(toEventDateTime(end));
            event.setStatus("tentative");

            event = googleCalendarApiClient.events().insert("primary", event).execute();

            return event.getId();
        } catch (IOException e) {
            throw new RuntimeException("Error calling google calendar api.", e);
        }
    }

    public void listextEvents(String calendarId, int numberOfEventsToList) {
        try {
            DateTime now = new DateTime(Instant.now().toEpochMilli());
            var events = googleCalendarApiClient.events().list(calendarId)
                    .setMaxResults(numberOfEventsToList)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()
                    .getItems();

            events.forEach(event -> {
                logger.info("Event summary: " + event.getSummary());
                logger.info("Event start: " + event.getStart().getDateTime());
                logger.info("Event end: " + event.getEnd().getDateTime());
            });
        } catch (IOException e) {
            throw new RuntimeException("Error calling google calendar api.", e);
        }
    }

    private static EventDateTime toEventDateTime(ZonedDateTime zonedDateTime) {
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
