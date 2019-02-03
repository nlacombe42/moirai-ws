package net.nlacombe.moirai.domain;

import java.util.Arrays;

public enum EventParticipation {
    NEEDS_ACTION("NEEDS-ACTION", "needsAction"),
    ACCEPTED("ACCEPTED", "accepted"),
    TENTATIVE("TENTATIVE", "tentative"),
    DECLINED("DECLINED", "declined");

    private String icalCode;
    private String googleCalendarCode;

    EventParticipation(String icalCode, String googleCalendarCode) {
        this.icalCode = icalCode;
        this.googleCalendarCode = googleCalendarCode;
    }

    public static EventParticipation fromIcalCode(String icalCode) {
        return Arrays.stream(values())
                .filter(eventParticipation -> icalCode.equals(eventParticipation.getIcalCode()))
                .findAny()
                .orElse(NEEDS_ACTION);
    }

    public static EventParticipation fromGoogleCalendarCode(String googleCalendarCode) {
        return Arrays.stream(values())
                .filter(eventParticipation -> googleCalendarCode.equals(eventParticipation.getGoogleCalendarCode()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No EventParticipation for Google Calendar code: " + googleCalendarCode));
    }

    public String getIcalCode() {
        return icalCode;
    }

    public String getGoogleCalendarCode() {
        return googleCalendarCode;
    }
}
