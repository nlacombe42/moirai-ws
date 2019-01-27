package net.nlacombe.moirai.domain;

import java.util.Arrays;

public enum EventParticipation {
    ACCEPTED("ACCEPTED"),
    TENTATIVE("TENTATIVE"),
    DECLINED("DECLINED");

    private String icalCode;

    EventParticipation(String icalCode) {
        this.icalCode = icalCode;
    }

    public static EventParticipation fromIcalCode(String icalCode) {
        return Arrays.stream(values())
                .filter(eventParticipation -> icalCode.equals(eventParticipation.getIcalCode()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No EventParticipation for ICal code: " + icalCode));
    }

    public String getIcalCode() {
        return icalCode;
    }
}
