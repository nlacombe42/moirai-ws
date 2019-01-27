package net.nlacombe.moirai.domain;

import java.time.ZonedDateTime;

public class Event implements Comparable<Event> {

    private String id;
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
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", participation=" + participation +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
