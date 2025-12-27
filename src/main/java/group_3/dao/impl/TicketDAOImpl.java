package group_3.dao.impl;

import group_3.dao.TicketDAO;
import group_3.model.*;
import group_3.model.enums.TicketStatus;
import group_3.model.enums.TicketType;
import group_3.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;

public class TicketDAOImpl implements TicketDAO {
    @Override
    public void create(Ticket ticket) { //create new row in ticket table
        String sql = "INSERT INTO ticket (attendee_id, event_id, session_id, type, price, status, qr_code_data) " +
                "VALUES (?,?,?,?,?,?,?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, ticket.getAttendeeID());
            ps.setInt(2, ticket.getEventID());
            ps.setInt(3, ticket.getSessionID());
            ps.setString(4, ticket.getType().toString());
            ps.setDouble(5, ticket.getPrice());
            ps.setString(6, ticket.getStatus().toString());
            ps.setString(7, ticket.getQRpath());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Ticket ticket) { //update the exist row in ticket table
        String sql = "UPDATE ticket" +
                "SET attendee_id = ?, event_id = ?, session_id = ?, type = ?, status = ?, qr_code_data = ? " +
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
            return mapRowToTicket(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        } return null;
    }

    @Override
    public ArrayList<Ticket> findTicketByAttendeeId(int id) {
        String sql = "SELECT * FROM ticket WHERE attendee_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            ArrayList<Ticket> tickets = new ArrayList<>();
            while (rs.next()) {
                tickets.add(mapRowToTicket(rs));
            }  return tickets;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
