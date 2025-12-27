package group_3.dao.impl;

/**
 * System History DAO for creating and finding activities
 *
 * Author: Group 3
 */

import group_3.dao.SystemHistoryDAO;
import group_3.model.SystemHistory;
import group_3.util.DatabaseConnection;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

public class SystemHistoryDaoImpl implements SystemHistoryDAO {

    @Override
    public void create(SystemHistory history){
        String sql = "INSERT INTO audit_log (user_id, operation_type, details) VALUES (?,?,?::jsonb)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ){
            if(history.getUserId() != null){
                ps.setInt(1,history.getUserId());
            }
            else{
                ps.setNull(1, Types.INTEGER);
            }

            ps.setString(2, history.getOperationType());
            ps.setString(3, history.getDetails());

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
            ResultSet rs = ps.executeQuery();
        ){
            while(rs.next()){
                historyList.add(mapRow(rs));
            }
        }catch(SQLException e){
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
