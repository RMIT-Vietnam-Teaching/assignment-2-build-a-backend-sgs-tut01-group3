package group_3;

import group_3.util.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main Application Entry Point.
 *
 * This is the central entry point that: 1. Initializes the database connection
 * 2. Launches the JavaFX application with Login screen 3. Routes users to
 * appropriate dashboards based on their role
 *
 * @author Group 3
 */

public class Main extends Application {

    @Override
    public void init() throws Exception {
        // Initialize database before UI loads
        try {
            System.out.println("Initializing database...");
            DatabaseConnection.setUpDatabase();
            System.out.println("Database initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize database");
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load Login screen as the starting point
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
       

        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("Login - Event Management System");
        primaryStage.setScene(scene);
        primaryStage.setWidth(550);
        primaryStage.setHeight(650);
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(550);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
