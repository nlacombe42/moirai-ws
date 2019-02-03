package net.nlacombe.moirai.googlecalendar;

import net.nlacombe.moirai.domain.Event;

public class GoogleEvent extends Event {

    private String googleEventId;

    @Override
    public String toString() {
        return "GoogleEvent{" +
                " " + super.toString() +
                "googleEventId='" + googleEventId + '\'' +
                '}';
    }

    public String getGoogleEventId() {
        return googleEventId;
    }

    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }
}
