package group_3.service.EventAdminService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import group_3.model.Event;
import group_3.model.Session;
import group_3.model.Ticket;
import group_3.model.enums.TicketStatus;
import group_3.model.enums.TicketType;

/**
 * Service interface for Event Administration.
 * Provides business logic operations for managing events and sessions.
 * 
 * Author: Group 3
 */
public interface EventAdminService {

    // ==================== EVENT OPERATIONS ====================

    /**
     * Create a new event.
     * @param event the event to create
     * @return the created event with assigned ID
     */
    Event createEvent(Event event);

    /**
     * Retrieve an event by ID.
     * @param eventId the event identifier
     * @return an Optional containing the event if found
     */
    Optional<Event> getEventById(int eventId);

    /**
     * Retrieve an event by name.
     * @param name the event name
     * @return an Optional containing the event if found
     */
    Optional<Event> getEventByName(String name);

    /**
     * Retrieve all events.
     * @return a list of all events
     */
    List<Event> getAllEvents();

    /**
     * Update an existing event.
     * @param event the event with updated information
     */
    void updateEvent(Event event);

    /**
     * Delete an event by ID.
     * @param eventId the event identifier
     */
    void deleteEvent(int eventId);

    /**
     * Get the total number of events.
     * @return the count of events
     */
    int getEventCount();

    // ==================== SESSION OPERATIONS ====================

    /**
     * Create a new session for an event.
     * @param session the session to create
     * @return the created session with assigned ID
     */
    Session createSession(Session session);

    /**
     * Retrieve a session by ID.
     * @param sessionId the session identifier
     * @return an Optional containing the session if found
     */
    Optional<Session> getSessionById(int sessionId);

    /**
     * Retrieve a session by title.
     * @param title the session title
     * @return an Optional containing the session if found
     */
    Optional<Session> getSessionByTitle(String title);

    /**
     * Retrieve all sessions.
     * @return a list of all sessions
     */
    List<Session> getAllSessions();

    /**
     * Retrieve all sessions for a specific event.
     * @param eventId the event identifier
     * @return a list of sessions in the event
     */
    List<Session> getSessionsByEventId(int eventId);

    /**
     * Update an existing session.
     * @param session the session with updated information
     */
    void updateSession(Session session);

    /**
     * Delete a session by ID.
     * @param sessionId the session identifier
     */
    void deleteSession(int sessionId);

    /**
     * Get the total number of sessions.
     * @return the count of sessions
     */
    int getSessionCount();

    /**
     * Get the number of sessions for a specific event.
     * @param eventId the event identifier
     * @return the count of sessions in the event
     */
    int getSessionCountByEvent(int eventId);

    // ==================== COMBINED OPERATIONS ====================

    /**
     * Add a session to an event.
     * @param eventId the event identifier
     * @param sessionId the session identifier
     */
    void addSessionToEvent(int eventId, int sessionId);

    /**
     * Remove a session from an event.
     * @param eventId the event identifier
     * @param sessionId the session identifier
     */
    void removeSessionFromEvent(int eventId, int sessionId);

    /**
     * Get all details of an event including its sessions.
     * @param eventId the event identifier
     * @return an Optional containing a map with event and its sessions
     */
    Optional<Event> getEventWithSessions(int eventId);

    /**
     * Check if an event exists.
     * @param eventId the event identifier
     * @return true if the event exists, false otherwise
     */
    boolean eventExists(int eventId);

    /**
     * Check if a session exists.
     * @param sessionId the session identifier
     * @return true if the session exists, false otherwise
     */
    boolean sessionExists(int sessionId);

    // ==================== PRESENTER ASSIGNMENT OPERATIONS ====================

    /**
     * Assign a presenter to a session.
     * @param sessionId the session identifier
     * @param presenterId the presenter identifier
     * @return true if assignment was successful, false otherwise
     */
    boolean assignPresenterToSession(int sessionId, String presenterId);

    /**
     * Unassign a presenter from a session.
     * @param sessionId the session identifier
     * @param presenterId the presenter identifier
     * @return true if unassignment was successful, false otherwise
     */
    boolean unassignPresenterFromSession(int sessionId, String presenterId);

    /**
     * Get all presenter IDs assigned to a session.
     * @param sessionId the session identifier
     * @return a list of presenter IDs assigned to the session
     */
    List<String> getPresentersBySessionId(int sessionId);

    /**
     * Check if a presenter is assigned to a session.
     * @param sessionId the session identifier
     * @param presenterId the presenter identifier
     * @return true if the presenter is assigned to the session, false otherwise
     */
    boolean isPresenterAssignedToSession(int sessionId, String presenterId);

    // ==================== TICKET MANAGEMENT OPERATIONS ====================

    /**
     * Generate a ticket for an attendee for a specific event and session.
     * @param attendeeId the attendee identifier
     * @param eventId the event identifier
     * @param sessionId the session identifier (0 if event-level ticket)
     * @param ticketType the type of ticket
     * @param price the ticket price
     * @return the generated ticket
     */
    Ticket generateTicket(int attendeeId, int eventId, int sessionId, TicketType ticketType, double price);

    /**
     * Update the status of a ticket.
     * @param ticketId the ticket identifier
     * @param newStatus the new ticket status
     * @return true if the update was successful, false otherwise
     */
    boolean updateTicketStatus(int ticketId, TicketStatus newStatus);

    /**
     * Get a ticket by its ID.
     * @param ticketId the ticket identifier
     * @return the ticket if found, null otherwise
     */
    Ticket getTicketById(int ticketId);

    /**
     * Get all tickets for a specific event.
     * @param eventId the event identifier
     * @return a list of tickets for the event
     */
    List<Ticket> getTicketsByEventId(int eventId);

    /**
     * Get all tickets for a specific session.
     * @param sessionId the session identifier
     * @return a list of tickets for the session
     */
    List<Ticket> getTicketsBySessionId(int sessionId);

    /**
     * Delete a ticket by its ID.
     * @param ticketId the ticket identifier
     * @return true if deletion was successful, false otherwise
     */
    boolean deleteTicket(int ticketId);

    // ==================== REPORT GENERATION OPERATIONS ====================

    /**
     * Generate an attendance report for a specific event.
     * @param eventId the event identifier
     * @return a map containing attendance statistics (total attendees, per session breakdown)
     */
    Map<String, Object> generateEventAttendanceReport(int eventId);

    /**
     * Generate a session occupancy report for a specific event.
     * @param eventId the event identifier
     * @return a map containing session occupancy data (capacity, registered, percentage)
     */
    Map<String, Object> generateSessionOccupancyReport(int eventId);

    /**
     * Generate a ticket usage report for a specific event.
     * @param eventId the event identifier
     * @return a map containing ticket statistics (by type, by status, revenue)
     */
    Map<String, Object> generateTicketUsageReport(int eventId);

    /**
     * Export an event report to a downloadable format.
     * @param eventId the event identifier
     * @param reportType the type of report (ATTENDANCE, OCCUPANCY, TICKET_USAGE, FULL)
     * @return the file path of the generated report
     */
    String exportEventReport(int eventId, String reportType);

    // ==================== VISITOR VIEW OPERATIONS ====================

    /**
     * Get all available events for visitors (public view).
     * @return a list of all available events
     */
    List<Event> getAvailableEventsForVisitors();

    /**
     * Filter events by type for visitors.
     * @param eventType the event type to filter by
     * @return a list of events matching the type
     */
    List<Event> filterEventsByType(String eventType);

    /**
     * Filter events by date range for visitors.
     * @param startDate the start date (ISO format)
     * @param endDate the end date (ISO format)
     * @return a list of events within the date range
     */
    List<Event> filterEventsByDateRange(String startDate, String endDate);

    /**
     * Filter events by location for visitors.
     * @param location the location to filter by
     * @return a list of events at the specified location
     */
    List<Event> filterEventsByLocation(String location);

    /**
     * Check if an event has available capacity.
     * @param eventId the event identifier
     * @return true if the event has available spots, false otherwise
     */
    boolean hasAvailableCapacity(int eventId);
}
