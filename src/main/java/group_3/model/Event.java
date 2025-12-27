package group_3.model;

/**
 * Event composed of one or more sessions occurring at a location and date/time.
 * Stores session ids rather than full session objects to keep the model light.
 *
 * @author Group 3
 */


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import group_3.model.enums.EventStatus;
import group_3.model.enums.EventType;

/** Simple Event class representing an event with multiple sessions. */
public class Event {
   

    private String eventId;
    private String name;
    private EventType type;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;
    private int duration; // duration in days
    private EventStatus status;
    private String eventImage; // URL or path to event image
    private final List<String> sessionIds = new ArrayList<>();

    /**
     * Full constructor with all attributes.
     */
    public Event(String eventId, String name, EventType type, LocalDateTime startDate, LocalDateTime endDate,
                 String location, int duration, EventStatus status, String eventImage) {
        this.eventId = eventId;
        this.name = name;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.duration = duration;
        this.status = status;
        this.eventImage = eventImage;
    }

    /**
     * Simplified constructor with default status and no image.
     */
    public Event(String eventId, String name, EventType type, LocalDateTime startDate, LocalDateTime endDate,
                 String location, int duration, EventStatus eventStatus) {
        this(eventId, name, type, startDate, endDate, location, duration, eventStatus, null);
    }

    // Getters and Setters

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getEventImage() {
        return eventImage;
    }

    public void setEventImage(String eventImage) {
        this.eventImage = eventImage;
    }

    // Session management

    public void addSession(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            sessionIds.add(sessionId);
        }
    }

    /** Remove a session id from the event (no-op if not present). */
    public void removeSession(String sessionId) {
        sessionIds.remove(sessionId);
    }

    /** Snapshot of session ids linked to this event (unmodifiable). */
    public List<String> getSessionIds() {
        return Collections.unmodifiableList(sessionIds);
    }

    @Override
    public String toString() {
        return "Event{" +
                "eventId='" + eventId + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", location='" + location + '\'' +
                ", duration=" + duration +
                ", status=" + status +
                ", eventImage='" + eventImage + '\'' +
                ", sessions=" + sessionIds +
                '}';
    }
}
