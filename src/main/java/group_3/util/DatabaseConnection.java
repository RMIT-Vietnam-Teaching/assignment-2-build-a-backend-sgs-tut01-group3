package group_3.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * @author Group 3
 *
 * Utility class for establishing and managing database connections,
 * including a cached connection and initialization checks.
 */


public class DatabaseConnection {

    // Hardcoded defaults: Supabase-style URL with embedded credentials
    private static final String URL  = "jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres?user=postgres.rhevgrivrjisjwwzmfur&password=yj5CF-iT7.CZ!di";

    /**
     * Get a new database connection. Each call returns a fresh connection.
     * Callers are responsible for closing the connection when done.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }
        
        return DriverManager.getConnection(URL);
    }

    private static boolean isDatabaseFullyInitialized() {
        String[] requiredTables = {"person", "attendee", "event", "session", "ticket", "audit_log"};
        try (Connection conn = DriverManager.getConnection(URL)) {
            for (String table : requiredTables) {
                String checkQuery = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = '" + table + "')";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(checkQuery)) {
                    if (rs.next() && !rs.getBoolean(1)) {
                        System.out.println("Missing table: " + table);
                        return false;
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Could not check database state: " + e.getMessage());
            return false;
        }
    }

    private static void executeSQLScript(Connection conn, String scriptPath) throws SQLException {
        InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(scriptPath);

        if (inputStream == null) {
            throw new SQLException("SQL script not found: " + scriptPath);
        }

        StringBuilder sql = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) continue;

                sql.append(line).append("\n");
                if (line.endsWith(";")) {
                    String sqlStr = sql.toString().trim();
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(sqlStr);
                    }
                    sql.setLength(0);
                }
            }
        } catch (IOException e) {
            throw new SQLException("Failed to read SQL script: " + scriptPath, e);
        }
    }

    public static void setUpDatabase() {
        if (isDatabaseFullyInitialized()) {
            return;
        }
        
        System.out.println("Database incomplete, running setup...");
        try {
            setupSchema();
            loadInitialData();
        } catch (SQLException e) {
            System.err.println("[Error] Database setup failed: " + e.getMessage());
        }
    }

    public static void setupSchema() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL)) {
            String[] dropStatements = {
                "DROP TABLE IF EXISTS audit_log CASCADE",
                "DROP TABLE IF EXISTS schedule_entry CASCADE",
                "DROP TABLE IF EXISTS ticket CASCADE",
                "DROP TABLE IF EXISTS session_presenter CASCADE",
                "DROP TABLE IF EXISTS session_material CASCADE",
                "DROP TABLE IF EXISTS session CASCADE",
                "DROP TABLE IF EXISTS event CASCADE",
                "DROP TABLE IF EXISTS presenter CASCADE",
                "DROP TABLE IF EXISTS attendee CASCADE",
                "DROP TABLE IF EXISTS person CASCADE",
                "DROP TYPE IF EXISTS user_role CASCADE",
                "DROP TYPE IF EXISTS event_status CASCADE",
                "DROP TYPE IF EXISTS ticket_status CASCADE"
            };
            
            for (String dropSql : dropStatements) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(dropSql);
                }
            }
            System.out.println("All tables dropped");
            
            executeSQLScript(conn, "sql/schema.sql");
            System.out.println("Schema setup completed");
        }
    }

    public static void loadInitialData() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL)) {
            executeSQLScript(conn, "sql/initial_data.sql");
            System.out.println("Initial data loaded");
        } catch (SQLException e) {
            System.err.println("[Error] Loading initial data failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Main method to reset the database.
     * Usage: mvn compile exec:java -Dexec.mainClass="group_3.util.DatabaseConnection"
     */
    public static void main(String[] args) {
        System.out.println("=== Database Reset Tool ===");
        try {
            System.out.println("Step 1: Dropping all tables and recreating schema...");
            setupSchema();
            
            System.out.println("Step 2: Loading initial data...");
            loadInitialData();
            
            System.out.println("=== Database reset completed successfully! ===");
        } catch (SQLException e) {
            System.err.println("Database reset failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
