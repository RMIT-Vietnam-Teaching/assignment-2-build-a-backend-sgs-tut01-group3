package group_3.service.EventAdminService;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import group_3.dao.EventDAO;
import group_3.dao.SessionDAO;
import group_3.dao.TicketDAO;
import group_3.dao.impl.EventDAOImpl;
import group_3.dao.impl.SessionDAOImpl;
import group_3.dao.impl.TicketDAOImpl;
import group_3.model.Event;
import group_3.model.Session;
import group_3.model.Ticket;
import group_3.model.enums.EventStatus;
import group_3.model.enums.TicketStatus;
import group_3.model.enums.TicketType;
import group_3.util.QRCode;

/**
 * Implementation of EventAdminService for managing Events and Sessions.
 * Provides CRUD operations, presenter assignment, ticket management,
 * and report generation capabilities.
 * 
 * @author Group 3
 */
public class EventAdminServiceImpl implements EventAdminService {
    
    private final EventDAO eventDAO;
    private final SessionDAO sessionDAO;
    private final TicketDAO ticketDAO;

    /**
     * Default constructor using DAO implementations.
     */
    public EventAdminServiceImpl() {
        this.eventDAO = new EventDAOImpl();
        this.sessionDAO = new SessionDAOImpl();
        this.ticketDAO = new TicketDAOImpl();
    }

    /**
     * Constructor with dependency injection for testing.
     * @param eventDAO the event DAO
     * @param sessionDAO the session DAO
     */
    public EventAdminServiceImpl(EventDAO eventDAO, SessionDAO sessionDAO) {
        this.eventDAO = eventDAO;
        this.sessionDAO = sessionDAO;
        this.ticketDAO = new TicketDAOImpl();
    }

    /**
     * Constructor with full dependency injection for testing.
     * @param eventDAO the event DAO
     * @param sessionDAO the session DAO
     * @param ticketDAO the ticket DAO
     */
    public EventAdminServiceImpl(EventDAO eventDAO, SessionDAO sessionDAO, TicketDAO ticketDAO) {
        this.eventDAO = eventDAO;
        this.sessionDAO = sessionDAO;
        this.ticketDAO = ticketDAO;
    }

    // ==================== EVENT OPERATIONS ====================

   
    @Override
    public Event createEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getName() == null || event.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Event name cannot be null or empty");
        }
        eventDAO.create(event);
        return event;
    }

    
    @Override
    public Optional<Event> getEventById(int eventId) {
        if (eventId <= 0) {
            return Optional.empty();
        }
        return eventDAO.findById(eventId);
    }

    
    @Override
    public Optional<Event> getEventByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return eventDAO.findByName(name);
    }

   
    @Override
    public List<Event> getAllEvents() {
        return eventDAO.findAll();
    }
    @Override
      public boolean eventExists(int eventId) {
        if (eventId <= 0) {
            return false;
        }
        return eventDAO.exists(eventId);
    }
    @Override
    public boolean sessionExists(int sessionId) {
        if (sessionId <= 0) {
            return false;
        }
        return sessionDAO.exists(sessionId);
    }


    
    @Override
    public void updateEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }
        if (!eventExists(Integer.parseInt(event.getEventId()))) {
            throw new IllegalArgumentException("Event with ID " + event.getEventId() + " does not exist");
        }
        eventDAO.update(event);
    }

    
    @Override
    public void deleteEvent(int eventId) {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Event ID must be positive");
        }
        if (!eventExists(eventId)) {
            throw new IllegalArgumentException("Event with ID " + eventId + " does not exist");
        }
        // Delete all sessions associated with the event first
        List<Session> sessions = sessionDAO.findByEventId(eventId);
        for (Session session : sessions) {
            sessionDAO.delete(Integer.parseInt(session.getSessionId()));
        }
        eventDAO.delete(eventId);
    }

   
    @Override
    public int getEventCount() {
        return eventDAO.count();
    }

    // ==================== SESSION OPERATIONS ====================

    @Override
    public Session createSession(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        if (session.getTitle() == null || session.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Session title cannot be null or empty");
        }
        sessionDAO.create(session);
        return session;
    }

    @Override
    public Optional<Session> getSessionById(int sessionId) {
        if (sessionId <= 0) {
            return Optional.empty();
        }
        return sessionDAO.findById(sessionId);
    }

    @Override
    public Optional<Session> getSessionByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return Optional.empty();
        }
        return sessionDAO.findByTitle(title);
    }

    @Override
    public List<Session> getAllSessions() {
        return sessionDAO.findAll();
    }

    @Override
    public List<Session> getSessionsByEventId(int eventId) {
        if (eventId <= 0) {
            return new ArrayList<>();
        }
        return sessionDAO.findByEventId(eventId);
    }

    @Override
    public void updateSession(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        if (session.getSessionId() == null || session.getSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        if (!sessionExists(Integer.parseInt(session.getSessionId()))) {
            throw new IllegalArgumentException("Session with ID " + session.getSessionId() + " does not exist");
        }
        sessionDAO.update(session);
    }

    @Override
    public void deleteSession(int sessionId) {
        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be positive");
        }
        if (!sessionExists(sessionId)) {
            throw new IllegalArgumentException("Session with ID " + sessionId + " does not exist");
        }
        sessionDAO.delete(sessionId);
    }

    @Override
    public int getSessionCount() {
        return sessionDAO.count();
    }

    @Override
    public int getSessionCountByEvent(int eventId) {
        if (eventId <= 0) {
            return 0;
        }
        List<Session> sessions = sessionDAO.findByEventId(eventId);
        return sessions != null ? sessions.size() : 0;
    }

    // ==================== COMBINED OPERATIONS ====================

    @Override
    public void addSessionToEvent(int eventId, int sessionId) {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Event ID must be positive");
        }
        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be positive");
        }
        if (!eventExists(eventId)) {
            throw new IllegalArgumentException("Event with ID " + eventId + " does not exist");
        }
        if (!sessionExists(sessionId)) {
            throw new IllegalArgumentException("Session with ID " + sessionId + " does not exist");
        }
        
        Optional<Session> sessionOpt = sessionDAO.findById(sessionId);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            session.setEventId(String.valueOf(eventId));
            sessionDAO.update(session);
        }
    }

    @Override
    public void removeSessionFromEvent(int eventId, int sessionId) {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Event ID must be positive");
        }
        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be positive");
        }
        if (!eventExists(eventId)) {
            throw new IllegalArgumentException("Event with ID " + eventId + " does not exist");
        }
        if (!sessionExists(sessionId)) {
            throw new IllegalArgumentException("Session with ID " + sessionId + " does not exist");
        }
        
        Optional<Session> sessionOpt = sessionDAO.findById(sessionId);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            if (session.getEventId() != null && session.getEventId().equals(String.valueOf(eventId))) {
                session.setEventId(null);
                sessionDAO.update(session);
            }
        }
    }

    @Override
    public Optional<Event> getEventWithSessions(int eventId) {
        if (eventId <= 0) {
            return Optional.empty();
        }
        return eventDAO.findById(eventId);
    }

    // ==================== PRESENTER ASSIGNMENT OPERATIONS ====================

    @Override
    public boolean assignPresenterToSession(int sessionId, String presenterId) {
        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be positive");
        }
        if (presenterId == null || presenterId.trim().isEmpty()) {
            throw new IllegalArgumentException("Presenter ID cannot be null or empty");
        }
        if (!sessionExists(sessionId)) {
            return false;
        }
        
        Optional<Session> sessionOpt = sessionDAO.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        
        Session session = sessionOpt.get();
        List<String> presenters = session.getPresenterIds();
        
        if (!presenters.contains(presenterId)) {
            session.addPresenter(presenterId);
            sessionDAO.update(session);
            return true;
        }
        return false;
    }

    @Override
    public boolean unassignPresenterFromSession(int sessionId, String presenterId) {
        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be positive");
        }
        if (presenterId == null || presenterId.trim().isEmpty()) {
            throw new IllegalArgumentException("Presenter ID cannot be null or empty");
        }
        if (!sessionExists(sessionId)) {
            return false;
        }
        
        Optional<Session> sessionOpt = sessionDAO.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        
        Session session = sessionOpt.get();
        if (session.getPresenterIds().contains(presenterId)) {
            session.removePresenter(presenterId);
            sessionDAO.update(session);
            return true;
        }
        return false;
    }

    @Override
    public List<String> getPresentersBySessionId(int sessionId) {
        if (sessionId <= 0) {
            return new ArrayList<>();
        }
        
        Optional<Session> sessionOpt = sessionDAO.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> presenters = sessionOpt.get().getPresenterIds();
        return presenters != null ? new ArrayList<>(presenters) : new ArrayList<>();
    }

    @Override
    public boolean isPresenterAssignedToSession(int sessionId, String presenterId) {
        if (sessionId <= 0 || presenterId == null || presenterId.trim().isEmpty()) {
            return false;
        }
        
        List<String> presenters = getPresentersBySessionId(sessionId);
        return presenters.contains(presenterId);
    }

   

    // ==================== TICKET MANAGEMENT OPERATIONS ====================

  
    @Override
    public Ticket generateTicket(int attendeeId, int eventId, int sessionId, TicketType ticketType, double price) {
        if (attendeeId <= 0) {
            throw new IllegalArgumentException("Attendee ID must be positive");
        }
        if (eventId <= 0) {
            throw new IllegalArgumentException("Event ID must be positive");
        }
        if (!eventExists(eventId)) {
            throw new IllegalArgumentException("Event with ID " + eventId + " does not exist");
        }
        if (sessionId > 0 && !sessionExists(sessionId)) {
            throw new IllegalArgumentException("Session with ID " + sessionId + " does not exist");
        }
        if (ticketType == null) {
            throw new IllegalArgumentException("Ticket type cannot be null");
        }
       

        Ticket ticket = new Ticket();
        ticket.setAttendeeID(attendeeId);
        ticket.setEventID(eventId);
        ticket.setSessionID(sessionId);
        ticket.setType(ticketType);
        ticket.setStatus(TicketStatus.ACTIVE);
        
        // Generate QR code path
        String qrPath = QRCode.generateTicketQRPayload(ticket);
        ticket.setQRpath(qrPath);
        
        ticketDAO.create(ticket);
        return ticket;
    }

    @Override
    public boolean updateTicketStatus(int ticketId, TicketStatus newStatus) {
        if (ticketId <= 0) {
            throw new IllegalArgumentException("Ticket ID must be positive");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("Ticket status cannot be null");
        }

        Ticket ticket = ticketDAO.findById(ticketId);
        if (ticket == null) {
            return false;
        }

        ticket.setStatus(newStatus);
        ticketDAO.update(ticket);
        return true;
    }

  
    @Override
    public Ticket getTicketById(int ticketId) {
        if (ticketId <= 0) {
            return null;
        }
        return ticketDAO.findById(ticketId);
    }

   
    @Override
    public List<Ticket> getTicketsByEventId(int eventId) {
        if (eventId <= 0) {
            return new ArrayList<>();
        }
        
        List<Ticket> allTickets = ticketDAO.findAll();
        if (allTickets == null) {
            return new ArrayList<>();
        }
        
        return allTickets.stream()
                .filter(t -> t.getEventID() == eventId)
                .collect(Collectors.toList());
    }

   
    @Override
    public List<Ticket> getTicketsBySessionId(int sessionId) {
        if (sessionId <= 0) {
            return new ArrayList<>();
        }
        
        List<Ticket> tickets = ticketDAO.findTicketBySessionId(sessionId);
        return tickets != null ? tickets : new ArrayList<>();
    }

  
    @Override
    public boolean deleteTicket(int ticketId) {
        if (ticketId <= 0) {
            return false;
        }
        return ticketDAO.delete(ticketId);
    }

    // ==================== REPORT GENERATION OPERATIONS ====================

   
    @Override
    public Map<String, Object> generateEventAttendanceReport(int eventId) {
        Map<String, Object> report = new HashMap<>();
        
        if (eventId <= 0 || !eventExists(eventId)) {
            report.put("error", "Invalid event ID");
            return report;
        }

        Optional<Event> eventOpt = eventDAO.findById(eventId);
        if (eventOpt.isEmpty()) {
            report.put("error", "Event not found");
            return report;
        }

        Event event = eventOpt.get();
        List<Session> sessions = sessionDAO.findByEventId(eventId);
        List<Ticket> tickets = getTicketsByEventId(eventId);

        // Calculate total unique attendees
        long totalAttendees = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.ACTIVE || t.getStatus() == TicketStatus.USED)
                .map(Ticket::getAttendeeID)
                .distinct()
                .count();

        // Calculate attendance per session
        Map<String, Integer> sessionAttendance = new HashMap<>();
        for (Session session : sessions) {
            int sessionAttendeeCount = (int) tickets.stream()
                    .filter(t -> t.getSessionID() == Integer.parseInt(session.getSessionId()))
                    .filter(t -> t.getStatus() == TicketStatus.ACTIVE || t.getStatus() == TicketStatus.USED)
                    .count();
            sessionAttendance.put(session.getTitle(), sessionAttendeeCount);
        }

        report.put("eventId", eventId);
        report.put("eventName", event.getName());
        report.put("totalAttendees", totalAttendees);
        report.put("sessionAttendance", sessionAttendance);
        report.put("generatedAt", LocalDateTime.now().toString());

        return report;
    }

  
   

  
    @Override
    public Map<String, Object> generateTicketUsageReport(int eventId) {
        Map<String, Object> report = new HashMap<>();
        
        if (eventId <= 0 || !eventExists(eventId)) {
            report.put("error", "Invalid event ID");
            return report;
        }

        Optional<Event> eventOpt = eventDAO.findById(eventId);
        if (eventOpt.isEmpty()) {
            report.put("error", "Event not found");
            return report;
        }

        Event event = eventOpt.get();
        List<Ticket> tickets = getTicketsByEventId(eventId);

        // Count by ticket type
        Map<String, Long> ticketsByType = tickets.stream()
                .collect(Collectors.groupingBy(t -> t.getType().toString(), Collectors.counting()));

        // Count by status
        Map<String, Long> ticketsByStatus = tickets.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().toString(), Collectors.counting()));

       

        report.put("eventId", eventId);
        report.put("eventName", event.getName());
        report.put("totalTickets", tickets.size());
        report.put("ticketsByType", ticketsByType);
        report.put("ticketsByStatus", ticketsByStatus);
        report.put("generatedAt", LocalDateTime.now().toString());

        return report;
    }

   
    @Override
    public String exportEventReport(int eventId, String reportType) {
        if (eventId <= 0 || !eventExists(eventId)) {
            throw new IllegalArgumentException("Invalid event ID");
        }
        if (reportType == null || reportType.trim().isEmpty()) {
            throw new IllegalArgumentException("Report type cannot be null or empty");
        }

        Optional<Event> eventOpt = eventDAO.findById(eventId);
        if (eventOpt.isEmpty()) {
            throw new IllegalArgumentException("Event not found");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("event_%d_%s_%s.csv", eventId, reportType.toLowerCase(), timestamp);
        String filePath = "reports/" + fileName;

        try {
            // Create reports directory if it doesn't exist
            java.io.File reportsDir = new java.io.File("reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                switch (reportType.toUpperCase()) {
                    case "ATTENDANCE":
                        writeAttendanceReport(writer, eventId);
                        break;
                    case "TICKET_USAGE":
                        writeTicketUsageReport(writer, eventId);
                        break;
                    case "FULL":
                        writeFullReport(writer, eventId);
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid report type: " + reportType);
                }
            }

            return filePath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to export report: " + e.getMessage(), e);
        }
    }

    /**
     * Write attendance report to CSV.
     */
    private void writeAttendanceReport(PrintWriter writer, int eventId) {
        Map<String, Object> report = generateEventAttendanceReport(eventId);
        
        writer.println("EVENT ATTENDANCE REPORT");
        writer.println("Generated At," + report.get("generatedAt"));
        writer.println("Event ID," + report.get("eventId"));
        writer.println("Event Name," + report.get("eventName"));
        writer.println("Total Attendees," + report.get("totalAttendees"));
        writer.println();
        writer.println("SESSION BREAKDOWN");
        writer.println("Session Name,Attendee Count");
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> sessionAttendance = (Map<String, Integer>) report.get("sessionAttendance");
        if (sessionAttendance != null) {
            sessionAttendance.forEach((session, count) -> 
                writer.println(session + "," + count));
        }
    }

   
    /**
     * Write ticket usage report to CSV.
     */
    private void writeTicketUsageReport(PrintWriter writer, int eventId) {
        Map<String, Object> report = generateTicketUsageReport(eventId);
        
        writer.println("TICKET USAGE REPORT");
        writer.println("Generated At," + report.get("generatedAt"));
        writer.println("Event ID," + report.get("eventId"));
        writer.println("Event Name," + report.get("eventName"));
        writer.println("Total Tickets," + report.get("totalTickets"));
        writer.println();
        writer.println("TICKETS BY TYPE");
        writer.println("Type,Count");
        
        @SuppressWarnings("unchecked")
        Map<String, Long> byType = (Map<String, Long>) report.get("ticketsByType");
        if (byType != null) {
            byType.forEach((type, count) -> writer.println(type + "," + count));
        }
        
        writer.println();
        writer.println("TICKETS BY STATUS");
        writer.println("Status,Count");
        
        @SuppressWarnings("unchecked")
        Map<String, Long> byStatus = (Map<String, Long>) report.get("ticketsByStatus");
        if (byStatus != null) {
            byStatus.forEach((status, count) -> writer.println(status + "," + count));
        }
    }

    /**
     * Write full report combining all report types to CSV.
     */
    private void writeFullReport(PrintWriter writer, int eventId) {
        writeAttendanceReport(writer, eventId);
        writer.println();
        writer.println("=".repeat(50));
        writer.println();
        writeTicketUsageReport(writer, eventId);
    }

    // ==================== VISITOR VIEW OPERATIONS ====================

   
    @Override
    public List<Event> getAvailableEventsForVisitors() {
        List<Event> allEvents = eventDAO.findAll();
        if (allEvents == null) {
            return new ArrayList<>();
        }
        
        return allEvents.stream()
                .filter(e -> e.getStatus() == EventStatus.SCHEDULED || e.getStatus() == EventStatus.ONGOING)
                .filter(e -> e.getEndDate() == null || e.getEndDate().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
    }

   
    @Override
    public List<Event> filterEventsByType(String eventType) {
        if (eventType == null || eventType.trim().isEmpty()) {
            return getAvailableEventsForVisitors();
        }

        return getAvailableEventsForVisitors().stream()
                .filter(e -> e.getType() != null && e.getType().toString().equalsIgnoreCase(eventType))
                .collect(Collectors.toList());
    }

  
    @Override
    public List<Event> filterEventsByDateRange(String startDate, String endDate) {
        if (startDate == null || endDate == null) {
            return getAvailableEventsForVisitors();
        }

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            return getAvailableEventsForVisitors().stream()
                    .filter(e -> {
                        LocalDate eventStart = e.getStartDate().toLocalDate();
                        LocalDate eventEnd = e.getEndDate().toLocalDate();
                        return !eventStart.isBefore(start) && !eventEnd.isAfter(end);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return getAvailableEventsForVisitors();
        }
    }

  
    @Override
    public List<Event> filterEventsByLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return getAvailableEventsForVisitors();
        }

        return getAvailableEventsForVisitors().stream()
                .filter(e -> e.getLocation() != null && 
                        e.getLocation().toLowerCase().contains(location.toLowerCase()))
                .collect(Collectors.toList());
    }

  
    @Override
    public boolean hasAvailableCapacity(int eventId) {
        if (eventId <= 0 || !eventExists(eventId)) {
            return false;
        }

        List<Session> sessions = sessionDAO.findByEventId(eventId);
        List<Ticket> tickets = getTicketsByEventId(eventId);

        int totalCapacity = sessions.stream()
                .mapToInt(Session::getCapacity)
                .sum();

        long registeredCount = tickets.stream()
                .filter(t -> t.getStatus() != TicketStatus.CANCELLED)
                .count();

        return registeredCount < totalCapacity;
    }
}
