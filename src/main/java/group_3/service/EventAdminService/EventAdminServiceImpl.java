package group_3.service.EventAdminService;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

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
import group_3.security.AuthContext;
import group_3.service.SystemHistoryService.SystemHistoryService;
import group_3.service.SystemHistoryService.SystemHistoryServiceImpl;
import group_3.util.NotificationUtil;
import group_3.util.QRCode;

/**
 * @author Group 3
 *
 * Implementation of EventAdminService for managing Events and Sessions.
 * Provides CRUD operations, presenter assignment, ticket management,
 * and report generation capabilities.
 */
public class EventAdminServiceImpl implements EventAdminService {
    
    private final EventDAO eventDAO;
    private final SessionDAO sessionDAO;
    private final TicketDAO ticketDAO;
    private final SystemHistoryService historyService;

    /**
     * Default constructor using DAO implementations.
     */
    public EventAdminServiceImpl() {
        this.eventDAO = new EventDAOImpl();
        this.sessionDAO = new SessionDAOImpl();
        this.ticketDAO = new TicketDAOImpl();
        this.historyService = new SystemHistoryServiceImpl();
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
        this.historyService = new SystemHistoryServiceImpl();
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
        this.historyService = new SystemHistoryServiceImpl();
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

        String detail = String.format("""
            {
                "entity": "Event",
                "eventId": %d,
                "name": "%s",
                "type": "%s",
                "location": "%s",
                "startDate": "%s",
                "endDate": "%s",
                "status": "%s"
            }
            """,
            event.getEventId(),
            event.getName(),
            event.getType().name(),
            event.getLocation(),
            event.getStartDate(),
            event.getEndDate(),
            event.getStatus().name()
        );

        historyService.logAction(
            AuthContext.getCurrentUserId(),
            "CREATE EVENT",
            detail
        );

        NotificationUtil.notify(
                AuthContext.getCurrentUser(),
                "Event Created",
                "The event \"" + event.getName() + "\" has been successfully created"
        );

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
        if (event.getEventId() <= 0) {
            throw new IllegalArgumentException("Event ID must be positive");
        }
        if (!eventExists(event.getEventId())) {
            throw new IllegalArgumentException("Event with ID " + event.getEventId() + " does not exist");
        }
        eventDAO.update(event);

        String jsonDetail = String.format("{\"action\": \"UPDATE_EVENT\", \"eventId\": %d, \"name\": \"%s\"}",
                event.getEventId(), event.getName());

        historyService.logAction(
                AuthContext.getCurrentUserId(),
                "UPDATE EVENT",
                jsonDetail
        );

        NotificationUtil.notify(
                AuthContext.getCurrentUser(),
                "Update Successfully",
                "The event \"" + event.getName() + "\" has been successfully updated"
        );
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
            sessionDAO.delete(session.getSessionId());
        }
        Optional<Event> event = eventDAO.findById(eventId);

        if(event.isEmpty()){
            return;
        }
        eventDAO.delete(eventId);


        String jsonDetail = String.format("{\"action\": \"DELETE_EVENT\", \"eventId\": %d}",
                eventId);

        historyService.logAction(
                AuthContext.getCurrentUserId(),
                "DELETE EVENT",
                jsonDetail
        );

        NotificationUtil.notify(
                AuthContext.getCurrentUser(),
                "Delete successfully",
                "The event \"" + event.get().getName() + "\" has been deleted"
        );
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
        if (session.getStartTime() == null || session.getEndTime() == null) {
            throw new IllegalArgumentException("Session start time and end time cannot be null");
        }
        if (session.getStartTime().isAfter(session.getEndTime())) {
            throw new IllegalArgumentException("Session start time cannot be after end time");
        }
        
        // Validate session times comply with event times
        Event event = eventDAO.findById(session.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + session.getEventId()));
        
        if (session.getStartTime().isBefore(event.getStartDate())) {
            throw new IllegalArgumentException("Session start time cannot be before event start date");
        }
        if (session.getEndTime().isAfter(event.getEndDate())) {
            throw new IllegalArgumentException("Session end time cannot be after event end date");
        }
        
        sessionDAO.create(session);

        String detail = String.format("""
            {
                "entity": "Session",
                "sessionId": %d,
                "eventId": "%d",
                "title": "%s",
                "description": "%s",
                "startTime": "%s",
                "endTime": "%s",
                "venue": "%s",
                "capacity": "%d"
            }
            """,
                session.getSessionId(),
                session.getEventId(),
                session.getTitle(),
                session.getDescription(),
                session.getStartTime(),
                session.getEndTime(),
                session.getVenue(),
                session.getCapacity()
        );

        historyService.logAction(
                AuthContext.getCurrentUserId(),
                "CREATE SESSION",
                detail
        );

        NotificationUtil.notify(
                AuthContext.getCurrentUser(),
                "Session Created",
                "The session \"" + session.getTitle() + "\" has been successfully created"
        );

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
        if (session.getSessionId() <= 0) {
            throw new IllegalArgumentException("Session ID must be positive");
        }
        if (!sessionExists(session.getSessionId())) {
            throw new IllegalArgumentException("Session with ID " + session.getSessionId() + " does not exist");
        }
        sessionDAO.update(session);

        String jsonDetail = String.format("{\"action\": \"UPDATE_SESSION\", \"sessionId\": %d, \"title\": \"%s\"}",
                session.getSessionId(), session.getTitle());

        historyService.logAction(
                AuthContext.getCurrentUserId(),
                "UPDATE SESSION",
                jsonDetail
        );

        NotificationUtil.notify(
                AuthContext.getCurrentUser(),
                "Session Updated",
                "The session \"" + session.getTitle() + "\" has been successfully updated"
        );
    }

    @Override
    public void deleteSession(int sessionId) {
        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be positive");
        }
        if (!sessionExists(sessionId)) {
            throw new IllegalArgumentException("Session with ID " + sessionId + " does not exist");
        }
        Optional<Session> session = sessionDAO.findById(sessionId);
        if(session.isEmpty()){
            return;
        }
        sessionDAO.delete(sessionId);

        String jsonDetails = String.format("{\"action\": \"DELETE_SESSION\", \"sessionId\": %d}", sessionId);

        historyService.logAction(
                AuthContext.getCurrentUserId(),
                "DELETE SESSION",
                jsonDetails
        );

        NotificationUtil.notify(
                AuthContext.getCurrentUser(),
                "Session Deleted",
                "The session \"" + session.get().getTitle() + "\" has been deleted"
        );
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
            session.setEventId(eventId);
            sessionDAO.update(session);

            String jsonDetail = String.format("{\"action\": \"LINK_SESSION_EVENT\", \"sessionId\": %d, \"eventId\": %d}",
                    sessionId, eventId);

            historyService.logAction(
                    AuthContext.getCurrentUserId(),
                    "LINK SESSION EVENT",
                    jsonDetail
            );
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
            if (session.getEventId() == eventId) {
                session.setEventId(0);
                sessionDAO.update(session);

                String jsonDetail = String.format("{\"action\": \"UNLINK_SESSION_EVENT\", \"sessionId\": %d, \"eventId\": %d}",
                        sessionId, eventId);

                historyService.logAction(
                        AuthContext.getCurrentUserId(),
                        "UNLINK SESSION EVENT",
                        jsonDetail
                );
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
    public boolean assignPresenterToSession(int sessionId, int presenterId) {
        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be positive");
        }
        if (presenterId <= 0) {
            throw new IllegalArgumentException("Presenter ID must be positive");
        }
        if (!sessionExists(sessionId)) {
            return false;
        }
        
        Optional<Session> sessionOpt = sessionDAO.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        
        Session session = sessionOpt.get();
        List<Integer> presenters = session.getPresenterIds();
        
        if (!presenters.contains(presenterId)) {
            session.addPresenter(presenterId);
            sessionDAO.update(session);

            String jsonDetail = String.format("{\"action\": \"ASSIGN_PRESENTER\", \"sessionId\": %d, \"presenterId\": %d}",
                    sessionId, presenterId);

            historyService.logAction(
                    AuthContext.getCurrentUserId(),
                    "ASSIGN PRESENTER",
                    jsonDetail
            );

            NotificationUtil.notify(
                    AuthContext.getCurrentUser(),
                    "Assign Presenter Successfully",
                    "Assign presenter to session \"" + session.getTitle() + "\" successfully"
            );
            return true;
        }
        return false;
    }

    @Override
    public boolean unassignPresenterFromSession(int sessionId, int presenterId) {
        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be positive");
        }
        if (presenterId <= 0) {
            throw new IllegalArgumentException("Presenter ID must be positive");
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

            String jsonDetail = String.format("{\"action\": \"UNASSIGN_PRESENTER\", \"sessionId\": %d, \"presenterId\": %d}",
                    sessionId, presenterId);

            historyService.logAction(
                    AuthContext.getCurrentUserId(),
                    "UNASSIGN PRESENTER",
                    jsonDetail
            );

            NotificationUtil.notify(
                    AuthContext.getCurrentUser(),
                    "Unassign Presenter Successfully",
                    "Unassign presenter to session \"" + session.getTitle() + "\" successfully"
            );

            return true;
        }
        return false;
    }

    @Override
    public List<Integer> getPresentersBySessionId(int sessionId) {
        if (sessionId <= 0) {
            return new ArrayList<>();
        }
        
        Optional<Session> sessionOpt = sessionDAO.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Integer> presenters = sessionOpt.get().getPresenterIds();
        return presenters != null ? new ArrayList<>(presenters) : new ArrayList<>();
    }

    @Override
    public boolean isPresenterAssignedToSession(int sessionId, int presenterId) {
        if (sessionId <= 0 || presenterId <= 0) {
            return false;
        }
        
        List<Integer> presenters = getPresentersBySessionId(sessionId);
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

        String detail = String.format("""
            {
                "entity": "Ticket",
                "ticketId": %d",
                "sessionId": "%d",
                "type": "%s",
                "price": "%f",
                "status": "%s",
                "QRPath": "%s",
            }
            """,
                ticket.getTicketID(),
                ticket.getSessionID(),
                ticket.getType().name(),
                ticket.getPrice(),
                ticket.getStatus().name(),
                ticket.getQRpath()
        );

        historyService.logAction(
                AuthContext.getCurrentUserId(),
                "CREATE TICKET",
                detail
        );

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

        String jsonDetail = String.format("{\"action\": \"UPDATE_TICKET_STATUS\", \"ticketId\": %d, \"newStatus\": \"%s\"}",
                ticketId, newStatus.name());

        historyService.logAction(
                AuthContext.getCurrentUserId(),
                "UPDATE TICKET STATUS",
                jsonDetail
        );
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
        
        // Use optimized query to fetch only tickets for this event
        List<Ticket> eventTickets = ticketDAO.findByEventId(eventId);
        if (eventTickets == null) {
            return new ArrayList<>();
        }
        
        return eventTickets;
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

        String jsonDetail = String.format("{\"action\": \"DELETE_TICKET\", \"ticketId\": %d}", ticketId);

        historyService.logAction(
                AuthContext.getCurrentUserId(),
                "DELETE TICKET",
                jsonDetail
        );

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
                    .filter(t -> t.getSessionID() == session.getSessionId())
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
                    case "ATTENDANCE" -> writeAttendanceReport(writer, eventId);
                    case "TICKET_USAGE" -> writeTicketUsageReport(writer, eventId);
                    case "FULL" -> writeFullReport(writer, eventId);
                    default -> throw new IllegalArgumentException("Invalid report type: " + reportType);
                }
            }

            String detail =  String.format(
                    "{\"eventId\": %d, \"reportType\": \"%s\", \"filePath\": \"%s\"}",
                    eventId,
                    reportType.toUpperCase(),
                    filePath.replace("\\", "/")
            );

            historyService.logAction(
                    AuthContext.getCurrentUserId(),
                    "EXPORT EVENT REPORT",
                    detail
            );

            NotificationUtil.notifyReportExported(
                    AuthContext.getCurrentUser(),
                    reportType
            );

            return filePath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to export report: " + e.getMessage(), e);
        }
    }

    @Override
    public String exportEventReportPdf(int eventId) {
        if (eventId <= 0 || !eventExists(eventId)) {
            throw new IllegalArgumentException("Invalid event ID");
        }

        Event event = eventDAO.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        Map<String, Object> attendanceReport = generateEventAttendanceReport(eventId);
        Map<String, Object> ticketUsageReport = generateTicketUsageReport(eventId);
        List<Session> sessions = sessionDAO.findByEventId(eventId);
        List<Ticket> tickets = getTicketsByEventId(eventId);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path reportsDir = Paths.get("reports");
        Path pdfPath = reportsDir.resolve(String.format("event_%d_report_%s.pdf", eventId, timestamp));

        try {
            Files.createDirectories(reportsDir);

            try (PdfWriter writer = new PdfWriter(pdfPath.toString());
                 PdfDocument pdfDocument = new PdfDocument(writer);
                 Document document = new Document(pdfDocument)) {

                document.add(new Paragraph("Event Report").setBold().setFontSize(18));
                document.add(new Paragraph(event.getName() + " (ID: " + eventId + ")"));
                document.add(new Paragraph("Location: " + (event.getLocation() != null ? event.getLocation() : "N/A")));
                if (event.getStartDate() != null && event.getEndDate() != null) {
                    document.add(new Paragraph("Schedule: " + event.getStartDate() + " to " + event.getEndDate()));
                }
                document.add(new Paragraph("Generated At: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
                document.add(new Paragraph(" "));

                document.add(new Paragraph("Attendance Overview").setBold());
                Table overviewTable = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();
                overviewTable.addHeaderCell(headerCell("Metric"));
                overviewTable.addHeaderCell(headerCell("Value"));
                overviewTable.addCell(bodyCell("Total Attendees"));
                overviewTable.addCell(bodyCell(String.valueOf(attendanceReport.getOrDefault("totalAttendees", 0))));
                overviewTable.addCell(bodyCell("Sessions"));
                overviewTable.addCell(bodyCell(String.valueOf(sessions.size())));
                overviewTable.addCell(bodyCell("Tickets Issued"));
                overviewTable.addCell(bodyCell(String.valueOf(tickets.size())));
                document.add(overviewTable);
                document.add(new Paragraph(" "));

                document.add(new Paragraph("Session Occupancy").setBold());
                Table sessionTable = new Table(UnitValue.createPercentArray(new float[]{4, 2, 2, 2, 2})).useAllAvailableWidth();
                sessionTable.addHeaderCell(headerCell("Session"));
                sessionTable.addHeaderCell(headerCell("Capacity"));
                sessionTable.addHeaderCell(headerCell("Tickets Sold"));
                sessionTable.addHeaderCell(headerCell("Checked In"));
                sessionTable.addHeaderCell(headerCell("Occupancy"));

                if (sessions.isEmpty()) {
                    sessionTable.addCell(new Cell(1, 5)
                            .add(new Paragraph("No sessions available"))
                            .setTextAlignment(TextAlignment.CENTER));
                } else {
                    for (Session session : sessions) {
                        long sold = tickets.stream()
                                .filter(t -> t.getSessionID() == session.getSessionId())
                                .filter(t -> t.getStatus() == TicketStatus.ACTIVE || t.getStatus() == TicketStatus.USED)
                                .count();
                        long checkedIn = tickets.stream()
                                .filter(t -> t.getSessionID() == session.getSessionId())
                                .filter(t -> t.getStatus() == TicketStatus.USED)
                                .count();
                        double occupancy = session.getCapacity() > 0
                                ? (sold * 100.0) / session.getCapacity()
                                : 0.0;

                        sessionTable.addCell(bodyCell(session.getTitle() != null ? session.getTitle() : "Session " + session.getSessionId()));
                        sessionTable.addCell(bodyCell(String.valueOf(session.getCapacity())));
                        sessionTable.addCell(bodyCell(String.valueOf(sold)));
                        sessionTable.addCell(bodyCell(String.valueOf(checkedIn)));
                        sessionTable.addCell(bodyCell(String.format("%.1f%%", occupancy)));
                    }
                }
                document.add(sessionTable);
                document.add(new Paragraph(" "));

                document.add(new Paragraph("Attendance by Session").setBold());
                @SuppressWarnings("unchecked")
                Map<String, Integer> sessionAttendance = (Map<String, Integer>) attendanceReport.get("sessionAttendance");
                Table attendanceTable = new Table(UnitValue.createPercentArray(new float[]{4, 2})).useAllAvailableWidth();
                attendanceTable.addHeaderCell(headerCell("Session"));
                attendanceTable.addHeaderCell(headerCell("Attendees"));
                if (sessionAttendance != null && !sessionAttendance.isEmpty()) {
                    sessionAttendance.forEach((sessionName, count) -> {
                        attendanceTable.addCell(bodyCell(sessionName));
                        attendanceTable.addCell(bodyCell(String.valueOf(count)));
                    });
                } else {
                    attendanceTable.addCell(new Cell(1, 2)
                            .add(new Paragraph("No attendance data"))
                            .setTextAlignment(TextAlignment.CENTER));
                }
                document.add(attendanceTable);
                document.add(new Paragraph(" "));

                document.add(new Paragraph("Ticket Usage by Type").setBold());
                @SuppressWarnings("unchecked")
                Map<String, Long> ticketsByType = (Map<String, Long>) ticketUsageReport.get("ticketsByType");
                addKeyValueTable(document, ticketsByType, "Type", "Count");

                document.add(new Paragraph("Ticket Usage by Status").setBold());
                @SuppressWarnings("unchecked")
                Map<String, Long> ticketsByStatus = (Map<String, Long>) ticketUsageReport.get("ticketsByStatus");
                addKeyValueTable(document, ticketsByStatus, "Status", "Count");
            }

            String detail = String.format(
                    "{\"eventId\": %d, \"reportType\": \"PDF\", \"filePath\": \"%s\"}",
                    eventId,
                    pdfPath.toString().replace("\\", "/")
            );

            historyService.logAction(
                    AuthContext.getCurrentUserId(),
                    "EXPORT EVENT REPORT PDF",
                    detail
            );

            return pdfPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export PDF report: " + e.getMessage(), e);
        }
    }

    private void addKeyValueTable(Document document, Map<String, ? extends Number> data, String keyHeader, String valueHeader) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();
        table.addHeaderCell(headerCell(keyHeader));
        table.addHeaderCell(headerCell(valueHeader));

        if (data != null && !data.isEmpty()) {
            data.forEach((key, value) -> {
                table.addCell(bodyCell(key));
                table.addCell(bodyCell(String.valueOf(value)));
            });
        } else {
            table.addCell(new Cell(1, 2)
                    .add(new Paragraph("No data available"))
                    .setTextAlignment(TextAlignment.CENTER));
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private Cell headerCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setTextAlignment(TextAlignment.LEFT);
    }

    private Cell bodyCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setTextAlignment(TextAlignment.LEFT);
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
