package group_3.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import group_3.model.Admin;
import group_3.model.Event;
import group_3.model.EventStatistics;
import group_3.model.Person;
import group_3.model.Presenter;
import group_3.model.ScheduleEntry;
import group_3.model.Session;
import group_3.model.SystemHistory;
import group_3.model.Ticket;
import group_3.model.enums.EventStatus;
import group_3.model.enums.EventType;
import group_3.model.enums.Role;
import group_3.model.enums.TicketStatus;
import group_3.model.enums.TicketType;
/**
 * @author Group 3
 */
/**
 * Utility class to load all dashboard data using a SINGLE database
 * connection. This dramatically reduces load time by eliminating multiple
 * connection overheads.
 */
public class BulkDataLoader {

    /**
     * Container for all System Admin data
     */
    public static class SystemAdminData {

        public List<SystemHistory> history = new ArrayList<>();
        public List<Person> users = new ArrayList<>();
        public List<Event> events = new ArrayList<>();
        public List<Session> sessions = new ArrayList<>();
        public List<Ticket> tickets = new ArrayList<>();
        public List<ScheduleEntry> schedules = new ArrayList<>();
        public List<EventStatistics> statistics = new ArrayList<>();
    }

    /**
     * Container for Attendee Dashboard data
     */
    public static class AttendeeData {

        public List<Event> events = new ArrayList<>();
        public List<Ticket> tickets = new ArrayList<>();
        public List<ScheduleEntry> schedules = new ArrayList<>();
        public List<Session> sessions = new ArrayList<>();  // Cached sessions for all events
        public java.util.Map<Integer, List<Session>> sessionsByEventId = new java.util.HashMap<>();  // Sessions grouped by event ID
    }

    /**
     * Container for Presenter Dashboard data
     */
    public static class PresenterData {
        public List<Session> sessions = new ArrayList<>();
        public List<Ticket> allTickets = new ArrayList<>();
        public List<Event> events = new ArrayList<>();  // Events for presenter's sessions (for charts)
        public java.util.Map<String, Integer> eventTypeStats = new java.util.HashMap<>();  // Pre-calculated event type distribution
        public java.util.Map<Integer, Integer> sessionAudienceMap = new java.util.HashMap<>();  // Session ID -> audience count
        public Presenter presenter = null;
        public int sessionsPresented = 0;
        public int totalAttendees = 0;
        public double avgAttendance = 0.0;
    }

    /**
     * Container for Guest/Public Event Browser data
     */
    public static class GuestData {
        public List<Event> events = new ArrayList<>();
    }

    /**
     * Container for Event Admin data
     */
    public static class EventAdminData {
        public List<Event> events = new ArrayList<>();
        public List<Session> sessions = new ArrayList<>();
    }

    /**
     * Load all Guest/Public data using a single database connection.
     * @return GuestData containing events
     */
    public static GuestData loadGuestData() {
        GuestData data = new GuestData();
        try (Connection conn = DatabaseConnection.getConnection()) {
          
            
            data.events = loadEvents(conn);

        } catch (SQLException e) {
            System.err.println("Error loading guest data: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    /**
     * Load all Event Admin data using a single database connection.
     * @return EventAdminData containing events and sessions
     */
    public static EventAdminData loadEventAdminData() {
        EventAdminData data = new EventAdminData();
        try (Connection conn = DatabaseConnection.getConnection()) {
            data.events = loadEvents(conn);
            data.sessions = loadSessions(conn);
        } catch (SQLException e) {
            System.err.println("Error loading event admin data: " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }

    /**
     * Load all Presenter Dashboard data using a single database connection.
     * OPTIMIZED: Only loads sessions for this presenter and tickets for those sessions.
     * @param presenterId the ID of the current presenter
     * @return PresenterData containing sessions, statistics
     */
    public static PresenterData loadPresenterData(int presenterId) {
        PresenterData data = new PresenterData();
        try (Connection conn = DatabaseConnection.getConnection()) {

            // Load presenter info
            data.presenter = loadPresenterById(conn, presenterId);
            
            // Get sessions for this presenter from session_presenter table (OPTIMIZED - single query)
            List<Integer> presenterSessionIds = loadPresenterSessionIds(conn, presenterId);
            
            // If no sessions from table, use fallback calculation
            if (presenterSessionIds.isEmpty()) {
                int presenterPosition = getPresenterPosition(conn, presenterId);
                if (presenterPosition >= 0) {
                    // Load only sessions matching this presenter's position
                    data.sessions = loadSessionsByPosition(conn, presenterPosition);
                }
            } else {
                // Load only sessions for this presenter (OPTIMIZED - targeted query)
                data.sessions = loadSessionsByIds(conn, presenterSessionIds);
            }
            
        
            
            // Load tickets only for presenter's sessions (OPTIMIZED - targeted query)
            if (!data.sessions.isEmpty()) {
                List<Integer> sessionIds = data.sessions.stream()
                    .map(Session::getSessionId)
                    .toList();
                data.allTickets = loadTicketsBySessionIds(conn, sessionIds);
                
                // Load events for presenter's sessions (for pie chart)
                List<Integer> eventIds = data.sessions.stream()
                    .map(Session::getEventId)
                    .distinct()
                    .toList();
                data.events = loadEventsByIds(conn, eventIds);
                
                // Pre-calculate event type statistics (for pie chart)
                // Valid EventType enum values: CONFERENCE, WORKSHOP, CONCERT, EXHIBITION
                java.util.Map<Integer, Event> eventMap = new java.util.HashMap<>();
                for (Event e : data.events) {
                    eventMap.put(e.getEventId(), e);
                }
                for (Session s : data.sessions) {
                    Event event = eventMap.get(s.getEventId());
                    if (event != null && event.getType() != null) {
                        String eventType = event.getType().toString();
                        data.eventTypeStats.put(eventType, data.eventTypeStats.getOrDefault(eventType, 0) + 1);
                    }
                }
                
                // Pre-calculate audience per session (for bar chart)
                for (Session s : data.sessions) {
                    int audienceCount = (int) data.allTickets.stream()
                        .filter(t -> t.getSessionID() == s.getSessionId())
                        .filter(t -> t.getStatus() == TicketStatus.ACTIVE || t.getStatus() == TicketStatus.USED)
                        .count();
                    data.sessionAudienceMap.put(s.getSessionId(), audienceCount);
                }
                
            }
            
            // Calculate statistics in memory
            data.sessionsPresented = data.sessions.size();
            data.totalAttendees = calculateTotalAttendees(data.sessions, data.allTickets);
            data.avgAttendance = data.sessionsPresented > 0 ? 
                (double) data.totalAttendees / data.sessionsPresented : 0.0;

          

        } catch (SQLException e) {
            System.err.println("Error loading presenter data: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    /**
     * Load sessions by specific IDs (OPTIMIZED query)
     */
    private static List<Session> loadSessionsByIds(Connection conn, List<Integer> sessionIds) throws SQLException {
        List<Session> list = new ArrayList<>();
        if (sessionIds.isEmpty()) return list;
        
        String placeholders = String.join(",", sessionIds.stream().map(id -> "?").toList());
        String sql = "SELECT session_id, event_id, title, description, start_time, end_time, venue, capacity " +
                     "FROM session WHERE session_id IN (" + placeholders + ") ORDER BY session_id";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < sessionIds.size(); i++) {
                stmt.setInt(i + 1, sessionIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int sessionId = rs.getInt("session_id");
                    int eventId = rs.getInt("event_id");
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    java.time.LocalDateTime startTime = rs.getTimestamp("start_time") != null
                            ? rs.getTimestamp("start_time").toLocalDateTime() : null;
                    java.time.LocalDateTime endTime = rs.getTimestamp("end_time") != null
                            ? rs.getTimestamp("end_time").toLocalDateTime() : null;
                    String venue = rs.getString("venue");
                    int capacity = rs.getInt("capacity");

                    Session s = new Session(sessionId, eventId, title, description, startTime, endTime, venue, capacity);
                    list.add(s);
                }
            }
        }
        return list;
    }

    /**
     * Load events by specific IDs (for presenter's sessions)
     */
    private static List<Event> loadEventsByIds(Connection conn, List<Integer> eventIds) throws SQLException {
        List<Event> list = new ArrayList<>();
        if (eventIds.isEmpty()) return list;
        
        String placeholders = String.join(",", eventIds.stream().map(id -> "?").toList());
        String sql = "SELECT event_id, name, type, status, start_date, end_date, location, duration " +
                     "FROM event WHERE event_id IN (" + placeholders + ")";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < eventIds.size(); i++) {
                stmt.setInt(i + 1, eventIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int eventId = rs.getInt("event_id");
                    String name = rs.getString("name");
                    String location = rs.getString("location");
                    java.time.LocalDateTime startDate = rs.getTimestamp("start_date") != null
                            ? rs.getTimestamp("start_date").toLocalDateTime() : null;
                    java.time.LocalDateTime endDate = rs.getTimestamp("end_date") != null
                            ? rs.getTimestamp("end_date").toLocalDateTime() : null;
                    int duration = rs.getInt("duration");

                    // Parse EventType - only accept valid enum values
                    EventType type = null;
                    String typeStr = rs.getString("type");
                    if (typeStr != null && !typeStr.trim().isEmpty()) {
                        try {
                            type = EventType.valueOf(typeStr.trim().toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            type = null;
                        }
                    }

                    EventStatus status = null;
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        try {
                            status = EventStatus.valueOf(statusStr.toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            status = null;
                        }
                    }

                    Event e = new Event(eventId, name, type, startDate, endDate, location, duration, status);
                    list.add(e);
                }
            }
        }
        return list;
    }

    /**
     * Load sessions by presenter position (fallback for modulo calculation)
     */
    private static List<Session> loadSessionsByPosition(Connection conn, int position) throws SQLException {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT session_id, event_id, title, description, start_time, end_time, venue, capacity " +
                     "FROM session WHERE (session_id % 5) = ? ORDER BY session_id";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, position);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int sessionId = rs.getInt("session_id");
                    int eventId = rs.getInt("event_id");
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    java.time.LocalDateTime startTime = rs.getTimestamp("start_time") != null
                            ? rs.getTimestamp("start_time").toLocalDateTime() : null;
                    java.time.LocalDateTime endTime = rs.getTimestamp("end_time") != null
                            ? rs.getTimestamp("end_time").toLocalDateTime() : null;
                    String venue = rs.getString("venue");
                    int capacity = rs.getInt("capacity");

                    Session s = new Session(sessionId, eventId, title, description, startTime, endTime, venue, capacity);
                    list.add(s);
                }
            }
        }
        return list;
    }

    /**
     * Load tickets by session IDs (OPTIMIZED query)
     */
    private static List<Ticket> loadTicketsBySessionIds(Connection conn, List<Integer> sessionIds) throws SQLException {
        List<Ticket> list = new ArrayList<>();
        if (sessionIds.isEmpty()) return list;
        
        String placeholders = String.join(",", sessionIds.stream().map(id -> "?").toList());
        String sql = "SELECT ticket_id, event_id, session_id, attendee_id, type, price, status, qr_code_data " +
                     "FROM ticket WHERE session_id IN (" + placeholders + ") ORDER BY ticket_id";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < sessionIds.size(); i++) {
                stmt.setInt(i + 1, sessionIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int ticketId = rs.getInt("ticket_id");
                    int eventId = rs.getInt("event_id");
                    int sessionId = rs.getInt("session_id");
                    int attendeeId = rs.getInt("attendee_id");
                    double price = rs.getDouble("price");
                    String qrCodeData = rs.getString("qr_code_data");

                    TicketType type = null;
                    String typeStr = rs.getString("type");
                    if (typeStr != null) {
                        try {
                            type = TicketType.valueOf(typeStr.toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            type = TicketType.GENERAL;
                        }
                    }

                    TicketStatus status = null;
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        try {
                            status = TicketStatus.valueOf(statusStr.toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            status = TicketStatus.ACTIVE;
                        }
                    }

                    Ticket t = new Ticket(ticketId, eventId, sessionId, attendeeId, type, price, status, qrCodeData);
                    list.add(t);
                }
            }
        }
        return list;
    }

    private static Presenter loadPresenterById(Connection conn, int presenterId) throws SQLException {
        String sql = "SELECT p.id, p.username, p.password, p.full_name, p.date_of_birth, p.contact_information, p.role, " +
                     "pr.presenter_role, pr.statistics " +
                     "FROM person p JOIN presenter pr ON p.id = pr.person_id WHERE p.id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, presenterId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String username = rs.getString("username");
                    String password = rs.getString("password");
                    String fullName = rs.getString("full_name");
                    LocalDate dob = rs.getDate("date_of_birth") != null ? rs.getDate("date_of_birth").toLocalDate() : null;
                    String contactInfo = rs.getString("contact_information");
                    String presenterRole = rs.getString("presenter_role");
                    String statistics = rs.getString("statistics");
                    
                    Presenter presenter = new Presenter(id, username, password, fullName, dob, contactInfo, presenterRole, statistics);
                    return presenter;
                }
            }
        }
        return null;
    }

    private static List<Integer> loadPresenterSessionIds(Connection conn, int presenterId) throws SQLException {
        List<Integer> sessionIds = new ArrayList<>();
        String sql = "SELECT session_id FROM session_presenter WHERE presenter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, presenterId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessionIds.add(rs.getInt("session_id"));
                }
            }
        }
        return sessionIds;
    }

    private static int getPresenterPosition(Connection conn, int presenterId) throws SQLException {
        String sql = "SELECT person_id FROM presenter ORDER BY person_id";
        int position = 0;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                if (rs.getInt("person_id") == presenterId) {
                    return position % 5;
                }
                position++;
            }
        }
        return -1;
    }

    private static int calculateTotalAttendees(List<Session> sessions, List<Ticket> allTickets) {
        int total = 0;
        for (Session session : sessions) {
            total += (int) allTickets.stream()
                .filter(t -> t.getSessionID() == session.getSessionId())
                .filter(t -> t.getStatus() == TicketStatus.ACTIVE || t.getStatus() == TicketStatus.USED)
                .count();
        }
        return total;
    }

    /**
     * Load all Attendee Dashboard data using a single database connection.
     *
     * @param attendeeId the ID of the current attendee/user
     * @return AttendeeData containing events, tickets, schedules, and cached sessions
     */
    public static AttendeeData loadAttendeeData(int attendeeId) {
        AttendeeData data = new AttendeeData();

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Load all events
            data.events = loadEvents(conn);

            // Load all sessions and cache them by event ID (eliminates delay when clicking events)
            data.sessions = loadSessions(conn);
            for (Session session : data.sessions) {
                data.sessionsByEventId
                    .computeIfAbsent(session.getEventId(), k -> new ArrayList<>())
                    .add(session);
            }

            // Load tickets for this attendee
            data.tickets = loadTicketsForAttendee(conn, attendeeId);

            // Load schedules for this attendee
            data.schedules = loadSchedulesForUser(conn, attendeeId);

        } catch (SQLException e) {
            System.err.println("Error loading attendee data: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    private static List<Ticket> loadTicketsForAttendee(Connection conn, int attendeeId) throws SQLException {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT ticket_id, event_id, session_id, attendee_id, type, price, status, qr_code_data FROM ticket WHERE attendee_id = ? ORDER BY ticket_id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, attendeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int ticketId = rs.getInt("ticket_id");
                    int eventId = rs.getInt("event_id");
                    int sessionId = rs.getInt("session_id");
                    int attId = rs.getInt("attendee_id");
                    double price = rs.getDouble("price");
                    String qrCodeData = rs.getString("qr_code_data");

                    TicketType type = null;
                    String typeStr = rs.getString("type");
                    if (typeStr != null) {
                        try {
                            type = TicketType.valueOf(typeStr.toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            type = TicketType.GENERAL;
                        }
                    }

                    TicketStatus status = null;
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        try {
                            status = TicketStatus.valueOf(statusStr.toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            status = TicketStatus.ACTIVE;
                        }
                    }

                    Ticket t = new Ticket(ticketId, eventId, sessionId, attId, type, price, status, qrCodeData);
                    list.add(t);
                }
            }
        }
        return list;
    }

    private static List<ScheduleEntry> loadSchedulesForUser(Connection conn, int userId) throws SQLException {
        List<ScheduleEntry> list = new ArrayList<>();
        String sql = "SELECT schedule_id, person_id, session_id, start_time, end_time FROM schedule_entry WHERE person_id = ? ORDER BY start_time";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ScheduleEntry s = new ScheduleEntry();
                    s.setId(rs.getInt("schedule_id"));
                    s.setPersonID(rs.getInt("person_id"));
                    s.setSessionID(rs.getInt("session_id"));
                    s.setStartTime(rs.getTimestamp("start_time") != null
                            ? rs.getTimestamp("start_time").toLocalDateTime() : null);
                    s.setEndTime(rs.getTimestamp("end_time") != null
                            ? rs.getTimestamp("end_time").toLocalDateTime() : null);
                    list.add(s);
                }
            }
        }
        return list;
    }

    /**
     * Load all System Admin data using a single database connection.
     */
    public static SystemAdminData loadSystemAdminData() {
        SystemAdminData data = new SystemAdminData();

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Load all data sequentially but with SAME connection
            data.history = loadHistory(conn);

            data.users = loadUsers(conn);

            data.events = loadEvents(conn);

            data.sessions = loadSessions(conn);

            data.tickets = loadTickets(conn);

            data.schedules = loadSchedules(conn);

            // Calculate statistics in memory (no extra DB calls!)
            data.statistics = calculateStatistics(data.events, data.tickets);

        } catch (SQLException e) {
            System.err.println("Error loading data: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    /**
     * Calculate event statistics in memory from already loaded data. This is
     * MUCH faster than making multiple DB calls per event.
     */
    private static List<EventStatistics> calculateStatistics(List<Event> events, List<Ticket> tickets) {
        List<EventStatistics> statsList = new ArrayList<>();

        for (Event event : events) {
            int eventId = event.getEventId();

            // Filter tickets for this event - all done in memory
            double revenue = tickets.stream()
                    .filter(t -> t.getEventID() == eventId)
                    .filter(t -> t.getStatus() == TicketStatus.ACTIVE || t.getStatus() == TicketStatus.USED)
                    .mapToDouble(Ticket::getPrice)
                    .sum();

            int ticketsSold = (int) tickets.stream()
                    .filter(t -> t.getEventID() == eventId)
                    .filter(t -> t.getStatus() == TicketStatus.ACTIVE || t.getStatus() == TicketStatus.USED)
                    .count();

            int checkedIn = (int) tickets.stream()
                    .filter(t -> t.getEventID() == eventId)
                    .filter(t -> t.getStatus() == TicketStatus.USED)
                    .count();

            EventStatistics stats = new EventStatistics(
                    eventId,
                    event.getName(),
                    revenue,
                    ticketsSold,
                    checkedIn
            );
            statsList.add(stats);
        }

        return statsList;
    }

    private static List<SystemHistory> loadHistory(Connection conn) throws SQLException {
        List<SystemHistory> list = new ArrayList<>();
        String sql = "SELECT log_id, user_id, operation_type, details, timestamp FROM audit_log ORDER BY timestamp DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                SystemHistory h = new SystemHistory();
                h.setLogId(rs.getLong("log_id"));
                h.setUserId(rs.getInt("user_id"));
                h.setOperationType(rs.getString("operation_type"));
                h.setDetails(rs.getString("details"));
                // Handle OffsetDateTime
                java.sql.Timestamp ts = rs.getTimestamp("timestamp");
                if (ts != null) {
                    h.setTimestamp(ts.toInstant().atOffset(java.time.ZoneOffset.UTC));
                }
                list.add(h);
            }
        }
        return list;
    }

    private static List<Person> loadUsers(Connection conn) throws SQLException {
        List<Person> list = new ArrayList<>();
        String sql = "SELECT id, username, password, full_name, date_of_birth, contact_information, role FROM person ORDER BY id";

        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String passwordHash = rs.getString("password");
                String fullName = rs.getString("full_name");
                LocalDate dob = rs.getDate("date_of_birth") != null ? rs.getDate("date_of_birth").toLocalDate() : null;
                String contactInfo = rs.getString("contact_information");
                String roleStr = rs.getString("role");
                Role role = roleStr != null ? Role.valueOf(roleStr.toUpperCase()) : Role.ATTENDEE;

                // Create Admin instance (concrete subclass of Person)
                Admin p = new Admin(id, username, passwordHash, fullName, dob, contactInfo, role);
                list.add(p);
            }
        }
        return list;
    }

    private static List<Event> loadEvents(Connection conn) throws SQLException {
        List<Event> list = new ArrayList<>();
        String sql = "SELECT event_id, name, location, start_date, end_date, type, status, duration, event_image FROM event ORDER BY event_id";

        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int eventId = rs.getInt("event_id");
                String name = rs.getString("name");
                String location = rs.getString("location");
                java.time.LocalDateTime startDate = rs.getTimestamp("start_date") != null
                        ? rs.getTimestamp("start_date").toLocalDateTime() : null;
                java.time.LocalDateTime endDate = rs.getTimestamp("end_date") != null
                        ? rs.getTimestamp("end_date").toLocalDateTime() : null;
                int duration = rs.getInt("duration");
                String eventImage = rs.getString("event_image");

                // Parse EventType - only accept valid enum values
                EventType type = null;
                String typeStr = rs.getString("type");
                if (typeStr != null && !typeStr.trim().isEmpty()) {
                    try {
                        type = EventType.valueOf(typeStr.trim().toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        type = null;
                    }
                }

                EventStatus status = null;
                String statusStr = rs.getString("status");
                if (statusStr != null && !statusStr.trim().isEmpty()) {
                    try {
                        status = EventStatus.valueOf(statusStr.trim().toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        // Ignore invalid status values
                    }
                }

                Event e = new Event(eventId, name, type, startDate, endDate, location, duration, status, eventImage);
                list.add(e);
            }
        }
        return list;
    }

    private static List<Session> loadSessions(Connection conn) throws SQLException {
        List<Session> list = new ArrayList<>();
        String sql = "SELECT session_id, event_id, title, description, start_time, end_time, venue, capacity FROM session ORDER BY session_id";

        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int sessionId = rs.getInt("session_id");
                int eventId = rs.getInt("event_id");
                String title = rs.getString("title");
                String description = rs.getString("description");
                java.time.LocalDateTime startTime = rs.getTimestamp("start_time") != null
                        ? rs.getTimestamp("start_time").toLocalDateTime() : null;
                java.time.LocalDateTime endTime = rs.getTimestamp("end_time") != null
                        ? rs.getTimestamp("end_time").toLocalDateTime() : null;
                String venue = rs.getString("venue");
                int capacity = rs.getInt("capacity");

                Session s = new Session(sessionId, eventId, title, description, startTime, endTime, venue, capacity);
                list.add(s);
            }
        }
        return list;
    }

    private static List<Ticket> loadTickets(Connection conn) throws SQLException {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT ticket_id, event_id, session_id, attendee_id, type, price, status, qr_code_data FROM ticket ORDER BY ticket_id";

        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int ticketId = rs.getInt("ticket_id");
                int eventId = rs.getInt("event_id");
                int sessionId = rs.getInt("session_id");
                int attendeeId = rs.getInt("attendee_id");
                double price = rs.getDouble("price");
                String qrCodeData = rs.getString("qr_code_data");

                TicketType type = null;
                String typeStr = rs.getString("type");
                if (typeStr != null) {
                    try {
                        type = TicketType.valueOf(typeStr.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        type = TicketType.GENERAL;
                    }
                }

                TicketStatus status = null;
                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    try {
                        status = TicketStatus.valueOf(statusStr.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        status = TicketStatus.ACTIVE;
                    }
                }

                Ticket t = new Ticket(ticketId, eventId, sessionId, attendeeId, type, price, status, qrCodeData);
                list.add(t);
            }
        }
        return list;
    }

    private static List<ScheduleEntry> loadSchedules(Connection conn) throws SQLException {
        List<ScheduleEntry> list = new ArrayList<>();
        String sql = "SELECT schedule_id, person_id, session_id, start_time, end_time FROM schedule_entry ORDER BY schedule_id";

        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ScheduleEntry s = new ScheduleEntry();
                s.setId(rs.getInt("schedule_id"));
                s.setPersonID(rs.getInt("person_id"));
                s.setSessionID(rs.getInt("session_id"));
                s.setStartTime(rs.getTimestamp("start_time") != null
                        ? rs.getTimestamp("start_time").toLocalDateTime() : null);
                s.setEndTime(rs.getTimestamp("end_time") != null
                        ? rs.getTimestamp("end_time").toLocalDateTime() : null);
                list.add(s);
            }
        }
        return list;
    }
}
