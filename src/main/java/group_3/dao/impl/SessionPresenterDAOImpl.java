package group_3.dao.impl;

import group_3.dao.SessionPresenterDAO;
import group_3.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Group 3
 *
 *
 */
public class SessionPresenterDAOImpl implements SessionPresenterDAO {

    private Connection getConnection() throws SQLException   {
        return DatabaseConnection.getConnection();
    }

    @Override
    public boolean existsByPresenterId(int presenterId){
        String sql = """
            SELECT 1 FROM session_presenter WHERE presenter_id = ? LIMIT 1
        """;
        try(Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
                ps.setInt(1,presenterId);
                ResultSet rs = ps.executeQuery();
                return rs.next();
        }catch(SQLException e){
            throw new RuntimeException(e);
        }
    }
}
