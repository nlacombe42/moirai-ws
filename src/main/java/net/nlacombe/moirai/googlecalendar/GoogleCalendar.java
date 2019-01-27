package net.nlacombe.moirai.googlecalendar;

import java.time.ZoneId;

public class GoogleCalendar {

    private String calendarId;
    private String name;
    private ZoneId timezone;

    public GoogleCalendar(String calendarId, String name, ZoneId timezone) {
        this.calendarId = calendarId;
        this.name = name;
        this.timezone = timezone;
    }

    @Override
    public String toString() {
        return "GoogleCalendar{" +
                "calendarId='" + calendarId + '\'' +
                ", name='" + name + '\'' +
                ", timezone=" + timezone +
                '}';
    }

    public String getCalendarId() {
        return calendarId;
    }

    public String getName() {
        return name;
    }

    public ZoneId getTimezone() {
        return timezone;
    }
}
