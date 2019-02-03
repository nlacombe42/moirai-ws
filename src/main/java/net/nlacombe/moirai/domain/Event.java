package net.nlacombe.moirai.domain;

import java.time.ZonedDateTime;
import java.util.Objects;

public class Event implements Comparable<Event> {

    private String icalUid;
    private String name;
    private String description;
    private String location;
    private ZonedDateTime start;
    private ZonedDateTime end;
    private EventParticipation participation;

    @Override
    public int compareTo(Event event) {
        return start.compareTo(event.getStart());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event)) return false;
        Event event = (Event) o;
        return Objects.equals(icalUid, event.icalUid) &&
                Objects.equals(name, event.name) &&
                Objects.equals(description, event.description) &&
                Objects.equals(location, event.location) &&
                Objects.equals(start, event.start) &&
                Objects.equals(end, event.end) &&
                participation == event.participation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(icalUid, name, description, location, start, end, participation);
    }

    @Override
    public String toString() {
        return "Event{" +
                "icalUid='" + icalUid + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", participation=" + participation +
                '}';
    }

    public String getIcalUid() {
        return icalUid;
    }

    public void setIcalUid(String icalUid) {
        this.icalUid = icalUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public ZonedDateTime getStart() {
        return start;
    }

    public void setStart(ZonedDateTime start) {
        this.start = start;
    }

    public ZonedDateTime getEnd() {
        return end;
    }

    public void setEnd(ZonedDateTime end) {
        this.end = end;
    }

    public EventParticipation getParticipation() {
        return participation;
    }

    public void setParticipation(EventParticipation participation) {
        this.participation = participation;
    }
}
