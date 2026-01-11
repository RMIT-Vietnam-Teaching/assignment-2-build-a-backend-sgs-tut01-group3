package group_3.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import group_3.dao.EventDAO;
import group_3.dao.SessionDAO;
import group_3.dao.TicketDAO;
import group_3.model.Event;
import group_3.model.EventStatistics;
import group_3.service.EventAdminService.EventAdminService;
import group_3.service.EventAdminService.EventAdminServiceImpl;
import group_3.service.EventStatisticsService.EventStatisticsService;
import group_3.service.EventStatisticsService.EventStatisticsServiceImpl;
import group_3.util.DaoProvider;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * @author Group 3
 *
 * Controller for displaying event details and handling edit/delete actions
 * in the JavaFX application.
 */

public class EventDetailController {
    
    private Stage stage;
    private Event currentEvent;
    private EventListController listController;
    private EventDAO eventDAO;
    private EventStatisticsService statisticsService;
    private EventAdminService eventAdminService;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm");
    
    public EventDetailController(Event event, EventListController listController) {
        this.currentEvent = event;
        this.listController = listController;
        this.eventDAO = DaoProvider.getEventDAO();
        this.eventAdminService = new EventAdminServiceImpl();

        SessionDAO sessionDAO = DaoProvider.getSessionDAO();
        TicketDAO ticketDAO = DaoProvider.getTicketDAO();
        this.statisticsService = new EventStatisticsServiceImpl(eventDAO, sessionDAO, ticketDAO);
    }
    
    public void show() {
        stage = new Stage();
        stage.setTitle("Event Details - " + currentEvent.getName());
        stage.setScene(createScene());
        stage.setWidth(1000);
        stage.setHeight(850);
        stage.show();
    }
    
    private Scene createScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-background-color: white;");
        
        root.setTop(createHeader());
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white;");
        scrollPane.setContent(createContent());
        root.setCenter(scrollPane);
        
        return new Scene(root);
    }
    
    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");
        
        Button backBtn = createButton("← Back", "#95a5a6");
        backBtn.setOnAction(e -> stage.close());
        
        Label title = new Label(currentEvent.getName());
        title.setFont(new Font("System Bold", 24));
        HBox.setHgrow(title, Priority.ALWAYS);
        
        Button editBtn = createButton("Edit Event", "#3498db");
        editBtn.setOnAction(e -> handleEdit());
        
        Button deleteBtn = createButton("Delete Event", "#e74c3c");
        deleteBtn.setOnAction(e -> handleDelete());
        
        header.getChildren().addAll(backBtn, title, editBtn, deleteBtn);
        return header;
    }
    
    private VBox createContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20, 40, 20, 40));
        
        content.getChildren().add(createImageSection());
        content.getChildren().add(createBasicInfoSection());
        content.getChildren().add(createScheduleSection());
        content.getChildren().add(createSessionsSection());
        content.getChildren().add(createStatisticsSection());
        
        return content;
    }
    
    private VBox createImageSection() {
        VBox section = new VBox(10);
        section.setAlignment(javafx.geometry.Pos.CENTER);
        section.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 15; -fx-background-color: #f8f9fa;");
        section.setPrefHeight(350);
        
        if (currentEvent.getEventImage() != null && !currentEvent.getEventImage().isEmpty()) {
            try {
                String imageUrl = currentEvent.getEventImage();
                
                // Convert file path to proper file:// URI if it's a local file
                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && !imageUrl.startsWith("file://")) {
                    java.io.File file = new java.io.File(imageUrl);
                    if (file.exists()) {
                        imageUrl = file.toURI().toString();
                    }
                }
                
                Image image = new Image(imageUrl, 600, 300, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(600);
                imageView.setFitHeight(300);
                imageView.setPreserveRatio(true);
                section.getChildren().add(imageView);
            } catch (Exception e) {
                Label placeholder = new Label("Image not available");
                placeholder.setStyle("-fx-text-fill: #999; -fx-font-size: 16px;");
                section.getChildren().add(placeholder);
            }
        } else {
            Label placeholder = new Label("📷 No Event Image");
            placeholder.setStyle("-fx-text-fill: #bbb; -fx-font-size: 18px; -fx-font-weight: bold;");
            section.getChildren().add(placeholder);
        }
        
        return section;
    }
    
    private VBox createBasicInfoSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 15; -fx-background-color: #f8f9fa;");
        
        Label title = new Label("Basic Information");
        title.setFont(new Font("System Bold", 18));
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        
        grid.add(createDetailLabel("Event ID:"), 0, 0);
        grid.add(createValueLabel(String.valueOf(currentEvent.getEventId())), 1, 0);
        
        grid.add(createDetailLabel("Event Name:"), 0, 1);
        grid.add(createValueLabel(currentEvent.getName()), 1, 1);
        
        grid.add(createDetailLabel("Type:"), 0, 2);
        grid.add(createValueLabel(currentEvent.getType().toString()), 1, 2);
        
        grid.add(createDetailLabel("Status:"), 0, 3);
        Label statusLabel = createValueLabel(currentEvent.getStatus().toString());
        applyStatusStyle(statusLabel, currentEvent.getStatus().toString());
        grid.add(statusLabel, 1, 3);
        
        section.getChildren().addAll(title, grid);
        return section;
    }
    
    private VBox createScheduleSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 15; -fx-background-color: #f8f9fa;");
        
        Label title = new Label("Schedule & Location");
        title.setFont(new Font("System Bold", 18));
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        
        String startStr = currentEvent.getStartDate() != null 
            ? currentEvent.getStartDate().format(DATE_FORMATTER) 
            : "N/A";
        grid.add(createDetailLabel("Start Date:"), 0, 0);
        grid.add(createValueLabel(startStr), 1, 0);
        
        String endStr = currentEvent.getEndDate() != null 
            ? currentEvent.getEndDate().format(DATE_FORMATTER) 
            : "N/A";
        grid.add(createDetailLabel("End Date:"), 0, 1);
        grid.add(createValueLabel(endStr), 1, 1);
        
        grid.add(createDetailLabel("Duration:"), 0, 2);
        grid.add(createValueLabel(currentEvent.getDuration() + " day(s)"), 1, 2);
        
        grid.add(createDetailLabel("Location:"), 0, 3);
        grid.add(createValueLabel(currentEvent.getLocation()), 1, 3);
        
        section.getChildren().addAll(title, grid);
        return section;
    }
    
    // Cache sessions loaded via batch query
    private java.util.List<group_3.model.Session> cachedSessions = new java.util.ArrayList<>();
    
    private VBox createSessionsSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 15; -fx-background-color: #f8f9fa;");
        
        Label title = new Label("Associated Sessions");
        title.setFont(new Font("System Bold", 18));
        
        // Get SessionDAO to fetch session details
        SessionDAO sessionDAO = DaoProvider.getSessionDAO();
        cachedSessions = sessionDAO.findByEventId(currentEvent.getEventId());
        
        // Build session display list from cached sessions
        java.util.List<String> sessionDisplayList = cachedSessions.stream()
            .map(s -> s.getTitle() + " (ID: " + s.getSessionId() + ")")
            .collect(java.util.stream.Collectors.toList());
        
        ListView<String> sessionList = new ListView<>(
            FXCollections.observableArrayList(sessionDisplayList));
        sessionList.setPrefHeight(150);
        sessionList.setStyle("-fx-border-color: #bbb; -fx-border-radius: 3;");
        
        HBox buttons = new HBox(10);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Button createBtn = createButton("Create New Session", "#27ae60");
        createBtn.setOnAction(e -> {
            SessionEditorController editor = new SessionEditorController(currentEvent.getEventId(), this);
            editor.show();
        });
        
        Button editBtn = createButton("View/Edit Session", "#3498db");
        editBtn.setOnAction(e -> {
            int idx = sessionList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < cachedSessions.size()) {
                // Use cached session - no DB call needed
                group_3.model.Session selectedSession = cachedSessions.get(idx);
                SessionEditorController editor = new SessionEditorController(selectedSession, this);
                editor.show();
            } else {
                showError("No Selection", "Please select a session from the list first.");
            }
        });
        
        buttons.getChildren().addAll(createBtn, editBtn);
        
        section.getChildren().addAll(title, sessionList, buttons);
        return section;
    }
    
    public void refreshSessions() {
        try {
            // Reload the current event from database to get updated session data
            int eventId = currentEvent.getEventId();
            Optional<Event> updatedEvent = eventDAO.findById(eventId);
            
            if (updatedEvent.isPresent()) {
                this.currentEvent = updatedEvent.get();
                // Refresh the entire view to show updated data
                stage.setScene(createScene());
            }
        } catch (Exception e) {
            showError("Refresh Error", "Failed to refresh sessions: " + e.getMessage());
        }
    }
    
    private VBox createStatisticsSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 15; -fx-background-color: #f8f9fa;");
        
        Label title = new Label("Event Statistics");
        title.setFont(new Font("System Bold", 18));
        
        GridPane stats = new GridPane();
        stats.setHgap(30);
        stats.setVgap(15);
        stats.setStyle("-fx-padding: 15;");
        
        // Show loading placeholder immediately
        Label loadingLabel = new Label("Loading statistics...");
        loadingLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
        stats.add(loadingLabel, 0, 0);
        
        // Load statistics asynchronously to prevent UI freeze
        Thread statsThread = new Thread(() -> {
            try {
                int eventId = currentEvent.getEventId();
                Optional<EventStatistics> statsOpt = statisticsService.getEventStatistics(eventId);
                
                Platform.runLater(() -> {
                    stats.getChildren().clear();
                    if (statsOpt.isPresent()) {
                        EventStatistics stat = statsOpt.get();
                        
                        VBox revenueCard = createStatCard("Total Revenue", "$" + String.format("%.2f", stat.getTotalRevenue()));
                        VBox ticketsCard = createStatCard("Tickets Sold", String.valueOf(stat.getTotalTicketsSold()));
                        VBox attendanceCard = createStatCard("Attendance Rate", String.format("%.1f%%", stat.getAttendanceRate()));
                        
                        stats.add(revenueCard, 0, 0);
                        stats.add(ticketsCard, 1, 0);
                        stats.add(attendanceCard, 2, 0);
                    } else {
                        Label noStats = new Label("No statistics available");
                        noStats.setStyle("-fx-text-fill: #999;");
                        stats.add(noStats, 0, 0);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    stats.getChildren().clear();
                    Label errorLabel = new Label("Statistics not available");
                    errorLabel.setStyle("-fx-text-fill: #999;");
                    stats.add(errorLabel, 0, 0);
                });
            }
        });
        statsThread.setDaemon(true);
        statsThread.start();
        
        Button viewStatsBtn = createButton("View Full Statistics", "#3498db");
        viewStatsBtn.setOnAction(e -> handleViewFullStatistics());

        Button downloadReportBtn = createButton("Download PDF Report", "#27ae60");
        downloadReportBtn.setOnAction(e -> handleDownloadReport());

        HBox actions = new HBox(10, viewStatsBtn, downloadReportBtn);
        actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        section.getChildren().addAll(title, stats, actions);
        return section;
    }
    
    private VBox createStatCard(String label, String value) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #e0e0e0; " +
                     "-fx-border-width: 1; -fx-border-radius: 5;");
        card.setAlignment(javafx.geometry.Pos.CENTER);
        
        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 28px; -fx-font-weight: bold;");
        
        card.getChildren().addAll(labelLbl, valueLbl);
        return card;
    }
    
    private Label createDetailLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
        label.setPrefWidth(150);
        return label;
    }
    
    private Label createValueLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #2c3e50;");
        label.setWrapText(true);
        return label;
    }
    
    private void applyStatusStyle(Label label, String status) {
        String bgColor;
        switch (status.toUpperCase()) {
            case "SCHEDULED":
                bgColor = "#3498db";
                break;
            case "ONGOING":
                bgColor = "#f39c12";
                break;
            case "COMPLETED":
                bgColor = "#27ae60";
                break;
            case "CANCELLED":
                bgColor = "#e74c3c";
                break;
            default:
                bgColor = "#95a5a6";
        }
        label.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; " +
                      "-fx-padding: 5px 10px; -fx-border-radius: 3; -fx-font-weight: bold;");
    }
    
    private Button createButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                    "-fx-padding: 8px 20px; -fx-font-weight: bold;");
        return btn;
    }
    
    private void handleEdit() {
        EventFormController formController = new EventFormController(currentEvent, listController);
        formController.show();
        stage.close();
    }
    
    private void handleDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Event: " + currentEvent.getName());
        alert.setContentText("Are you sure? This will also delete all associated sessions. This action cannot be undone.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int eventId = currentEvent.getEventId();
                // Use EventAdminService which handles cascade deletion
                eventAdminService.deleteEvent(eventId);
                
                // Automatically refresh parent list view
                if (listController != null) {
                    listController.refreshEvents();
                }
                
                stage.close();
            } catch (Exception e) {
                showError("Error", e.getMessage());
            }
        }
    }
    
    private void handleViewFullStatistics() {
        // Run database query in background thread
        Thread statsThread = new Thread(() -> {
            try {
                int eventId = currentEvent.getEventId();
                Optional<EventStatistics> statsOpt = statisticsService.getEventStatistics(eventId);
                
                Platform.runLater(() -> {
                    if (statsOpt.isPresent()) {
                        EventStatistics stat = statsOpt.get();
                        String message = String.format(
                            "Event: %s\nRevenue: $%.2f\nTickets Sold: %d\nChecked In: %d\nAttendance Rate: %.1f%%",
                            stat.getEventName(),
                            stat.getTotalRevenue(),
                            stat.getTotalTicketsSold(),
                            stat.getTotalCheckedIn(),
                            stat.getAttendanceRate()
                        );
                        showInfo("Event Statistics", message);
                    } else {
                        showInfo("Event Statistics", "No statistics available for this event.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error", e.getMessage());
                });
            }
        });
        statsThread.setDaemon(true);
        statsThread.start();
    }

    private void handleDownloadReport() {
        // First show file chooser (on UI thread)
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Event Report");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName("event_" + currentEvent.getEventId() + "_report.pdf");
        
        File target = fileChooser.showSaveDialog(stage);
        if (target == null) {
            // User cancelled - do nothing
            return;
        }
        
        // Show loading indicator
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Generating Report");
        loadingAlert.setHeaderText(null);
        loadingAlert.setContentText("Generating PDF report... Please wait.");
        loadingAlert.getButtonTypes().clear(); // Remove OK button during loading
        loadingAlert.show();
        
        // Run PDF generation in background thread
        Thread reportThread = new Thread(() -> {
            try {
                int eventId = currentEvent.getEventId();
                String generatedPath = eventAdminService.exportEventReportPdf(eventId);
                
                Path destination = target.toPath();
                Path parent = destination.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.copy(Paths.get(generatedPath), destination, StandardCopyOption.REPLACE_EXISTING);
                
                Platform.runLater(() -> {
                    loadingAlert.close();
                    showInfo("Report Saved", "Report saved to: " + destination.toAbsolutePath());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingAlert.close();
                    showError("Report Error", e.getMessage());
                });
            }
        });
        reportThread.setDaemon(true);
        reportThread.start();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
