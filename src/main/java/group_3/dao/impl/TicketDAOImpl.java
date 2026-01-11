package group_3.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import group_3.dao.TicketDAO;
import group_3.model.Ticket;
import group_3.model.enums.TicketStatus;
import group_3.model.enums.TicketType;
import group_3.util.DatabaseConnection;

/**
 * @author Group 3
 *
 * JDBC-based DAO implementation for creating and managing
 * Ticket records in the database.
 */


public class TicketDAOImpl implements TicketDAO {
    @Override
    public int create(Ticket ticket) { //create new row in ticket table
        String sql = "INSERT INTO ticket (attendee_id, event_id, session_id, type, price, status, qr_code_data) " +
                "VALUES (?,?,?,?,?,?::ticket_status,?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // Ask the database to return generated key
            ps.setInt(1, ticket.getAttendeeID());
            ps.setInt(2, ticket.getEventID());
            ps.setInt(3, ticket.getSessionID());
            ps.setString(4, ticket.getType().toString());
            ps.setDouble(5, ticket.getPrice());
            ps.setString(6, ticket.getStatus().toString());
            ps.setString(7, ticket.getQRpath());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedID = ps.getGeneratedKeys()) {
                    if (generatedID.next()) {
                        int newId = generatedID.getInt(1);
                        ticket.setTicketID(newId); // set the ticket ID as the auto generated one
                        return newId; // Return the generated ID of the ticket
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } return -1; // False to create ticket in the database
    }

    @Override
    public void update(Ticket ticket) { //update the exist row in ticket table
        String sql = "UPDATE ticket " +
                "SET attendee_id = ?, event_id = ?, session_id = ?, type = ?, price = ?, status = ?::ticket_status, qr_code_data = ? " +
                "WHERE ticket_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, ticket.getAttendeeID());
                ps.setInt(2, ticket.getEventID());
                ps.setInt(3, ticket.getSessionID());
                ps.setString(4, ticket.getType().toString());
                ps.setDouble(5, ticket.getPrice());
                ps.setString(6, ticket.getStatus().toString());
                ps.setString(7, ticket.getQRpath());
                ps.setInt(8, ticket.getTicketID());
                ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM ticket WHERE ticket_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public ArrayList<Ticket> findAll() {
        String sql = "SELECT * FROM ticket";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            ArrayList<Ticket> tickets = new ArrayList<>();
            while (rs.next()) {
                tickets.add(mapRowToTicket(rs));
            } return tickets;
        } catch (SQLException e) {
            e.printStackTrace();
        } return null;
    }

    @Override
    public Ticket findById(int id) {
        String sql = "SELECT * FROM ticket WHERE ticket_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRowToTicket(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } return null;
    }

    @Override
    public ArrayList<Ticket> findTicketByAttendeeId(int id) {
        String sql = "SELECT * FROM ticket WHERE attendee_id = ?";
        ArrayList<Ticket> tickets = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tickets.add(mapRowToTicket(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tickets;
    }

    @Override
    public ArrayList<Ticket> findTicketBySessionId(int id) {
        String sql = "SELECT * FROM ticket WHERE session_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            ArrayList<Ticket> tickets = new ArrayList<>();
            while (rs.next()) {
                tickets.add(mapRowToTicket(rs));
            } return tickets;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ArrayList<Ticket> findByEventId(int eventId) {
        String sql = "SELECT * FROM ticket WHERE event_id = ?";
        ArrayList<Ticket> tickets = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tickets.add(mapRowToTicket(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tickets;
    }

    public Ticket mapRowToTicket(ResultSet rs) throws SQLException {
        int id = rs.getInt("ticket_id");
        int attendeeID = rs.getInt("attendee_id");
        int eventID = rs.getInt("event_id");
        int sessionID = rs.getInt("session_id");
        TicketType type = TicketType.valueOf(rs.getString("type"));
        double price = rs.getDouble("price");
        TicketStatus status = TicketStatus.valueOf(rs.getString("status"));
        String qrpath = rs.getString("qr_code_data");

        return new Ticket(id, eventID, sessionID, attendeeID, type, price, status, qrpath);
    }
}
