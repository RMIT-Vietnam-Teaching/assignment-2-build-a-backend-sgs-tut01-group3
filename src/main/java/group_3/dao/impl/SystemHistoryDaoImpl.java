package group_3.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import group_3.dao.SystemHistoryDAO;
import group_3.model.SystemHistory;
import group_3.util.DatabaseConnection;

/**
 * @author Group 3
 *
 * JDBC-based DAO implementation for recording system history
 * and audit log entries in the database.
 */


public class SystemHistoryDaoImpl implements SystemHistoryDAO {

    @Override
    public void create(SystemHistory history){
        String sql = "INSERT INTO audit_log (user_id, operation_type, details) VALUES (?,?,?::jsonb)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)
        ){
            if(history.getUserId() != null){
                ps.setInt(1,history.getUserId());
            }
            else{
                ps.setNull(1, Types.INTEGER);
            }

            ps.setString(2, history.getOperationType());
            
            // Wrap details in a JSON object if it's not already valid JSON
            String details = history.getDetails();
            if (details != null && !details.trim().startsWith("{") && !details.trim().startsWith("[")) {
                // Properly escape for JSON and convert backslashes to forward slashes
                String escaped = details
                    .replace("\\", "/")     // Convert backslashes to forward slashes
                    .replace("\"", "\\\"")  // Escape quotes
                    .replace("\n", "\\n")   // Escape newlines
                    .replace("\r", "\\r")   // Escape carriage returns
                    .replace("\t", "\\t");  // Escape tabs
                details = "{\"message\": \"" + escaped + "\"}";
            }
            // If it's already JSON, pass it through as-is (caller should ensure proper escaping)
            ps.setString(3, details);

            ps.executeUpdate();

        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public List<SystemHistory> findAll(){
        String sql = "SELECT log_id, timestamp, user_id, operation_type, details FROM audit_log ORDER BY timestamp DESC ";
        List<SystemHistory> historyList = new ArrayList<>();
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ){
            while(rs.next()){
                historyList.add(mapRow(rs));
            }
        }catch(SQLException e){
            e.printStackTrace();
        }

        return historyList;
    }

    @Override
    public List<SystemHistory> findByUserId(int userId) {
        String sql = "SELECT log_id, timestamp, user_id, operation_type, details FROM audit_log WHERE user_id = ? ORDER BY timestamp DESC";
        List<SystemHistory> historyList = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                historyList.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return historyList;
    }

    @Override
    public List<SystemHistory> findByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT log_id, timestamp, user_id, operation_type, details FROM audit_log " +
                     "WHERE DATE(timestamp) >= ? AND DATE(timestamp) <= ? ORDER BY timestamp DESC";
        List<SystemHistory> historyList = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(startDate));
            ps.setDate(2, java.sql.Date.valueOf(endDate));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                historyList.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return historyList;
    }

    @Override
    public List<SystemHistory> findByOperationType(String operationType) {
        String sql = "SELECT log_id, timestamp, user_id, operation_type, details FROM audit_log WHERE operation_type = ? ORDER BY timestamp DESC";
        List<SystemHistory> historyList = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, operationType);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                historyList.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return historyList;
    }

    private SystemHistory mapRow(ResultSet rs) throws SQLException {
        return new SystemHistory(
                rs.getLong("log_id"),
                rs.getObject("timestamp", OffsetDateTime.class),
                (Integer) rs.getObject("user_id"),
                rs.getString("operation_type"),
                rs.getString("details")
        );
    }
}
