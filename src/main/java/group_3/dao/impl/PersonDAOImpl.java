package group_3.dao.impl;

import group_3.model.*;
import group_3.model.enums.Role;
import group_3.util.DatabaseConnection;
import group_3.dao.PersonDAO;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class PersonDAOImpl implements PersonDAO {

    private Connection getConnection() throws Exception {
        return DatabaseConnection.getConnection();
    }

    @Override
    public Optional<Person> findByUsername(String username) {
        String sql = "SELECT * FROM person WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToPerson(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    private Person mapRowToPerson(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String fullName = rs.getString("full_name");
        LocalDate dob = rs.getDate("date_of_birth").toLocalDate();
        String contact = rs.getString("contact_information");
        Role role = Role.valueOf(rs.getString("role"));

        if (role == Role.ATTENDEE) {
            return new Attendee(id, username, passwordHash, fullName, dob, contact, null);
        } else if (role == Role.PRESENTER) {
            return new Presenter(id, username, passwordHash, fullName, dob, contact, null, null);
        }
        return null; // Admins handled separately
    }

    @Override
    public void create(Person person) {
        String sql = "INSERT INTO person (username, password_hash, full_name, date_of_birth, contact_information, role) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, person.getUsername());
            ps.setString(2, person.getPasswordHash());
            ps.setString(3, person.getFullName());
            ps.setDate(4, java.sql.Date.valueOf(person.getDateOfBirth()));
            ps.setString(5, person.getContactInformation());
            ps.setString(6, person.getRole().name());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Person> findById(int id) {
        String sql = "SELECT * FROM person WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToPerson(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<Person> findAll() {
        String sql = "SELECT * FROM person";
        List<Person> persons = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                persons.add(mapRowToPerson(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return persons;
    }

    @Override
    public void update(Person person) {
        String sql = "UPDATE person SET username = ?, password_hash = ?, full_name = ?, date_of_birth = ?, contact_information = ?, role = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, person.getUsername());
            ps.setString(2, person.getPasswordHash());
            ps.setString(3, person.getFullName());
            ps.setDate(4, java.sql.Date.valueOf(person.getDateOfBirth()));
            ps.setString(5, person.getContactInformation());
            ps.setString(6, person.getRole().name());
            ps.setInt(7, person.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM person WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
