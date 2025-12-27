package group_3.dao.impl;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import group_3.dao.SessionDAO;
import group_3.model.Session;
import group_3.util.DatabaseConnection;

/**
 * Implementation of SessionDAO interface.
 * Handles database operations for Session entities.
 * 
 * Author: Tram Anh Tuan - s4075376 
 */
public class SessionDAOImpl implements SessionDAO {

    private Connection getConnection() throws Exception {
        return DatabaseConnection.getConnection();
    }

    @Override
    public void create(Session session) {
        String sql = "INSERT INTO session (event_id, title, description, scheduled_date, start_time, end_time, venue, capacity) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int eventId = session.getEventId() != null ? Integer.parseInt(session.getEventId()) : 1;
            
            ps.setInt(1, eventId);
            ps.setString(2, session.getTitle());
            ps.setString(3, session.getDescription());
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(session.getStartTime()));
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(session.getEndTime() != null ? session.getEndTime() : session.getStartTime().plusHours(1)));
            ps.setString(6, session.getVenue());
            ps.setInt(7, session.getCapacity());

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                session.setSessionId(String.valueOf(rs.getInt(1)));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating session: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Session> findById(int sessionId) {
        String sql = "SELECT * FROM session WHERE session_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sessionId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToSession(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding session by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Session> findByTitle(String title) {
        String sql = "SELECT * FROM session WHERE title = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, title);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToSession(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding session by title: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Session> findAll() {
        String sql = "SELECT * FROM session";
        List<Session> sessions = new ArrayList<>();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                sessions.add(mapRowToSession(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding all sessions: " + e.getMessage(), e);
        }
        return sessions;
    }

    @Override
    public List<Session> findByEventId(int eventId) {
        String sql = "SELECT * FROM session WHERE event_id = ?";
        List<Session> sessions = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, eventId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                sessions.add(mapRowToSession(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding sessions by event ID: " + e.getMessage(), e);
        }
        return sessions;
    }

    @Override
    public void update(Session session) {
        String sql = "UPDATE session SET event_id = ?, title = ?, description = ?, scheduled_date = ?, " +
                "start_time = ?, end_time = ?, venue = ?, capacity = ? WHERE session_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int eventId = session.getEventId() != null ? Integer.parseInt(session.getEventId()) : 1;
            
            ps.setInt(1, eventId);
            ps.setString(2, session.getTitle());
            ps.setString(3, session.getDescription());
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(session.getStartTime()));
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(session.getEndTime() != null ? session.getEndTime() : session.getStartTime().plusHours(1)));
            ps.setString(6, session.getVenue());
            ps.setInt(7, session.getCapacity());
            ps.setInt(8, Integer.parseInt(session.getSessionId()));

            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error updating session: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int sessionId) {
        String sql = "DELETE FROM session WHERE session_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting session: " + e.getMessage(), e);
        }
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM session";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error counting sessions: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public boolean exists(int sessionId) {
        String sql = "SELECT 1 FROM session WHERE session_id = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sessionId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            throw new RuntimeException("Error checking session existence: " + e.getMessage(), e);
        }
    }

    @Override
    public int countByEventId(int eventId) {
        String sql = "SELECT COUNT(*) FROM session WHERE event_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, eventId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error counting sessions by event: " + e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Maps a database result row to a Session object.
     * @param rs the result set row
     * @return a Session object
     * @throws SQLException if there's a SQL error
     */
    private Session mapRowToSession(ResultSet rs) throws SQLException {
        int id = rs.getInt("session_id");
        int eventId = rs.getInt("event_id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        java.sql.Timestamp startTimeSql = rs.getTimestamp("start_time");
        java.sql.Timestamp endTimeSql = rs.getTimestamp("end_time");
        String venue = rs.getString("venue");
        int capacity = rs.getInt("capacity");

        LocalDateTime startTime = startTimeSql != null ? startTimeSql.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime endTime = endTimeSql != null ? endTimeSql.toLocalDateTime() : startTime.plusHours(1);

        Session session = new Session(
                String.valueOf(id),
                String.valueOf(eventId),
                title,
                description,
                startTime,
                endTime,
                venue,
                capacity
        );
        return session;
    }
}

