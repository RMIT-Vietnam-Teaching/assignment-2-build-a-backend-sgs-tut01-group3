package group_3.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import group_3.dao.SessionDAO;
import group_3.dao.SessionMaterialDAO;
import group_3.dao.TicketDAO;
import group_3.dao.impl.PersonDAOImpl;
import group_3.dao.impl.SessionMaterialDAOImpl;
import group_3.model.Event;
import group_3.model.Person;
import group_3.model.Presenter;
import group_3.model.Session;
import group_3.model.SessionMaterial;
import group_3.model.Ticket;
import group_3.model.enums.TicketStatus;
import group_3.security.AuthContext;
import group_3.service.EventAdminService.EventAdminService;
import group_3.service.EventAdminService.EventAdminServiceImpl;
import group_3.service.PresenterService.PresenterService;
import group_3.service.PresenterService.PresenterServiceImpl;
import group_3.service.UserService.UserService;
import group_3.service.UserService.UserServiceImpl;
import group_3.util.BulkDataLoader;
import group_3.util.DaoProvider;
import group_3.util.PasswordUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.text.Font;
import javafx.util.StringConverter;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Dashboard Controller for Presenters.
 * Provides functionality for:
 * - Viewing assigned sessions
 * - Uploading session materials
 * - Viewing presenter statistics
 * - Exporting activity summary
 * - Managing profile
 * 
 * @author Group 3
 */
public class PresenterDashboardController {

    // Services
    private final PresenterService presenterService;
    private final EventAdminService eventService;
    private final UserService userService;
    private final SessionDAO sessionDAO;
    private final TicketDAO ticketDAO;
    private final SessionMaterialDAO sessionMaterialDAO;

    private Scene scene;
    private Person currentUser;
    private Presenter currentPresenter;
    
    // Cached presenter data from BulkDataLoader (for charts without additional DB calls)
    private BulkDataLoader.PresenterData cachedPresenterData;

    // My Sessions Tab
    private TableView<Session> sessionTable;
    private ObservableList<Session> sessionList;

    // Materials Tab
    private TableView<SessionMaterial> materialTable;
    private ObservableList<SessionMaterial> materialList;

    // Statistics Tab
    private Label totalSessionsValue;
    private Label totalAttendeesValue;
    private Label avgAttendanceValue;
    private PieChart eventTypeChart;
    private BarChart<String, Number> audienceChart;

    // Profile Tab
    private TextField usernameField;
    private TextField fullNameField;
    private DatePicker dobPicker;
    private TextField bioField;
    private TextField presenterRoleField;
    private PasswordField currentPasswordField;
    private PasswordField newPasswordField;
    private PasswordField confirmPasswordField;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Loading indicator
    private StackPane loadingOverlay;
    private Label loadingLabel;

    public PresenterDashboardController() {
        this.presenterService = new PresenterServiceImpl();
        this.eventService = new EventAdminServiceImpl();
        this.userService = new UserServiceImpl(new PersonDAOImpl());
        this.sessionDAO = DaoProvider.getSessionDAO();
        this.ticketDAO = DaoProvider.getTicketDAO();
        this.sessionMaterialDAO = new SessionMaterialDAOImpl();
        this.currentUser = AuthContext.getCurrentUser();
        
        // Load presenter data
        if (currentUser != null) {
            Optional<Presenter> presenter = presenterService.getPresenterById(currentUser.getId());
            this.currentPresenter = presenter.orElse(null);
        }
        
        initializeUI();
    }

    private void initializeUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");
        root.setTop(createHeader());

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab sessionsTab = new Tab("My Sessions", createMySessionsView());
        Tab materialsTab = new Tab("Session Materials", createMaterialsView());
        Tab statsTab = new Tab("Statistics", createStatisticsView());
        Tab profileTab = new Tab("Profile", createProfileView());

        tabPane.getTabs().addAll(sessionsTab, materialsTab, statsTab, profileTab);
        
        // Create loading overlay
        loadingOverlay = new StackPane();
        loadingOverlay.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8);");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(80, 80);
        loadingLabel = new Label("Loading data...");
        loadingLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        VBox loadingBox = new VBox(10, progressIndicator, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingOverlay.getChildren().add(loadingBox);
        
        StackPane centerStack = new StackPane(tabPane, loadingOverlay);
        root.setCenter(centerStack);

        scene = new Scene(root);
        
        loadAllDataAsync();
    }

    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #2c3e50;");

        Label titleLabel = new Label("Presenter Dashboard");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);


        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        logoutBtn.setOnAction(e -> handleLogout());

        header.getChildren().addAll(titleLabel, spacer, logoutBtn);
        return header;
    }

    private void loadAllDataAsync() {
        // Show loading overlay
        Platform.runLater(() -> {
            loadingOverlay.setVisible(true);
            loadingLabel.setText("Loading data...");
        });
        
        Thread loadThread = new Thread(() -> {
            try {   
                // Use BulkDataLoader for single connection loading
                int presenterId = currentUser != null ? currentUser.getId() : 0;
                BulkDataLoader.PresenterData data = BulkDataLoader.loadPresenterData(presenterId);
                
                // Cache the data for use in updateStatisticsView (avoids additional DB calls)
                cachedPresenterData = data;
                
                // Build stats map from loaded data
                Map<String, Object> stats = new HashMap<>();
                stats.put("sessions_presented", data.sessionsPresented);
                stats.put("total_attendees", data.totalAttendees);
                stats.put("average_attendance", Math.round(data.avgAttendance * 100.0) / 100.0);

                Platform.runLater(() -> {
                    sessionList.setAll(data.sessions);
                    updateStatisticsView(stats);
                    loadProfileData();
                    loadMaterialData(); // Load materials after sessions are loaded
                    loadingOverlay.setVisible(false);
                    
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> loadingOverlay.setVisible(false));
            }
        });
        loadThread.setDaemon(true);
        loadThread.start();
    }

    // ======================= TAB 1: MY SESSIONS =======================

    private VBox createMySessionsView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("My Assigned Sessions");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        HBox actionBar = new HBox(10);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> loadSessionData());

        actionBar.getChildren().add(refreshBtn);

        sessionTable = new TableView<>();
        sessionList = FXCollections.observableArrayList();
        sessionTable.setItems(sessionList);
        VBox.setVgrow(sessionTable, Priority.ALWAYS);

        TableColumn<Session, Integer> idCol = new TableColumn<>("Session ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("sessionId"));
        idCol.setPrefWidth(100);

        TableColumn<Session, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(250);

        TableColumn<Session, Integer> eventCol = new TableColumn<>("Event ID");
        eventCol.setCellValueFactory(new PropertyValueFactory<>("eventId"));
        eventCol.setPrefWidth(100);

        TableColumn<Session, String> venueCol = new TableColumn<>("Venue");
        venueCol.setCellValueFactory(new PropertyValueFactory<>("venue"));
        venueCol.setPrefWidth(150);

        TableColumn<Session, String> startCol = new TableColumn<>("Start Time");
        startCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getStartTime() != null ? c.getValue().getStartTime().format(DATETIME_FORMATTER) : "N/A"));
        startCol.setPrefWidth(150);

        TableColumn<Session, Integer> capacityCol = new TableColumn<>("Capacity");
        capacityCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        capacityCol.setPrefWidth(80);

        TableColumn<Session, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(120);
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("View Details");
            {
                viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                viewBtn.setOnAction(e -> {
                    Session session = getTableRow().getItem();
                    if (session != null) viewSessionDetails(session);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow().getItem() == null ? null : viewBtn);
            }
        });

        sessionTable.getColumns().addAll(idCol, titleCol, eventCol, venueCol, startCol, capacityCol, actionsCol);

        view.getChildren().addAll(title, actionBar, sessionTable);
        return view;
    }

    private void loadSessionData() {
        if (currentUser == null) return;
        try {
            // Use BulkDataLoader for single connection
            BulkDataLoader.PresenterData data = BulkDataLoader.loadPresenterData(currentUser.getId());
            sessionList.setAll(data.sessions);
        } catch (Exception e) {
            System.err.println("Error loading sessions: " + e.getMessage());
        }
    }

    private void viewSessionDetails(Session session) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Session Details");
        alert.setHeaderText(session.getTitle());
        alert.setContentText(
            "Session ID: " + session.getSessionId() + "\n" +
            "Event ID: " + session.getEventId() + "\n" +
            "Venue: " + session.getVenue() + "\n" +
            "Capacity: " + session.getCapacity() + "\n" +
            "Description: " + (session.getDescription() != null ? session.getDescription() : "N/A")
        );
        alert.showAndWait();
    }

    // ======================= TAB 2: SESSION MATERIALS =======================

    private VBox createMaterialsView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("Session Materials");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        HBox actionBar = new HBox(10);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        Button uploadBtn = new Button("Upload Material");
        uploadBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        uploadBtn.setOnAction(e -> handleUploadMaterial());

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> loadMaterialData());

        actionBar.getChildren().addAll(uploadBtn, refreshBtn);

        materialTable = new TableView<>();
        materialList = FXCollections.observableArrayList();
        materialTable.setItems(materialList);
        VBox.setVgrow(materialTable, Priority.ALWAYS);

        TableColumn<SessionMaterial, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("materialId"));
        idCol.setPrefWidth(60);

        TableColumn<SessionMaterial, Integer> sessionCol = new TableColumn<>("Session ID");
        sessionCol.setCellValueFactory(new PropertyValueFactory<>("sessionId"));
        sessionCol.setPrefWidth(100);

        TableColumn<SessionMaterial, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(200);

        TableColumn<SessionMaterial, String> typeCol = new TableColumn<>("File Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("fileType"));
        typeCol.setPrefWidth(100);

        TableColumn<SessionMaterial, String> urlCol = new TableColumn<>("URL");
        urlCol.setCellValueFactory(new PropertyValueFactory<>("contentUrl"));
        urlCol.setPrefWidth(250);

        TableColumn<SessionMaterial, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(100);
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            {
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                deleteBtn.setOnAction(e -> {
                    SessionMaterial material = getTableRow().getItem();
                    if (material != null) handleDeleteMaterial(material);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow().getItem() == null ? null : deleteBtn);
            }
        });

        materialTable.getColumns().addAll(idCol, sessionCol, titleCol, typeCol, urlCol, actionsCol);

        // Placeholder message
        materialTable.setPlaceholder(new Label("No materials uploaded yet. Click 'Upload Material' to add."));

        view.getChildren().addAll(title, actionBar, materialTable);
        return view;
    }

    private void loadMaterialData() {
        // Load materials from database for all sessions that belong to this presenter
        materialList.clear();
        try {
            for (Session session : sessionList) {
                List<SessionMaterial> materials = sessionMaterialDAO.findBySessionId(session.getSessionId());
                materialList.addAll(materials);
            }
        } catch (Exception e) {
            System.err.println("Error loading materials: " + e.getMessage());
        }
    }

    private void handleUploadMaterial() {
        Session selectedSession = sessionTable.getSelectionModel().getSelectedItem();
        
        Dialog<SessionMaterial> dialog = new Dialog<>();
        dialog.setTitle("Upload Session Material");
        dialog.setHeaderText("Add new material for a session");

        ButtonType uploadButtonType = new ButtonType("Upload", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(uploadButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<Session> sessionCombo = new ComboBox<>();
        sessionCombo.setItems(sessionList);
        sessionCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Session item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getSessionId() + " - " + item.getTitle());
            }
        });
        sessionCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Session item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getSessionId() + " - " + item.getTitle());
            }
        });
        if (selectedSession != null) {
            sessionCombo.setValue(selectedSession);
        }

        TextField titleField = new TextField();
        titleField.setPromptText("Material title");

        TextField descField = new TextField();
        descField.setPromptText("Description");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("PDF", "PPTX", "DOC", "VIDEO", "LINK", "OTHER");
        typeCombo.setValue("PDF");

        TextField urlField = new TextField();
        urlField.setPromptText("File URL or path");

        Button browseBtn = new Button("Browse...");
        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Material File");
            File file = fileChooser.showOpenDialog(scene.getWindow());
            if (file != null) {
                urlField.setText(file.toURI().toString());
            }
        });

        HBox urlBox = new HBox(10, urlField, browseBtn);
        HBox.setHgrow(urlField, Priority.ALWAYS);

        grid.add(new Label("Session:"), 0, 0);
        grid.add(sessionCombo, 1, 0);
        grid.add(new Label("Title:"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descField, 1, 2);
        grid.add(new Label("File Type:"), 0, 3);
        grid.add(typeCombo, 1, 3);
        grid.add(new Label("URL/Path:"), 0, 4);
        grid.add(urlBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == uploadButtonType) {
                Session session = sessionCombo.getValue();
                if (session == null) {
                    showAlert(Alert.AlertType.WARNING, "Warning", "Please select a session.");
                    return null;
                }
                SessionMaterial material = new SessionMaterial();
                material.setSessionId(session.getSessionId());
                material.setTitle(titleField.getText());
                material.setDescription(descField.getText());
                material.setFileType(typeCombo.getValue());
                material.setContentUrl(urlField.getText());
                return material;
            }
            return null;
        });

        Optional<SessionMaterial> result = dialog.showAndWait();
        result.ifPresent(material -> {
            try {
                // Save to database
                sessionMaterialDAO.create(material);
                materialList.add(material);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Material uploaded successfully!");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save material: " + e.getMessage());
            }
        });
    }

    private void handleDeleteMaterial(SessionMaterial material) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Material");
        confirm.setHeaderText("Delete: " + material.getTitle());
        confirm.setContentText("Are you sure you want to delete this material?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                sessionMaterialDAO.delete(material.getMaterialId());
                materialList.remove(material);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Material deleted.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete material: " + e.getMessage());
            }
        }
    }

    // ======================= TAB 3: STATISTICS =======================

    private VBox createStatisticsView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(20));

        Label title = new Label("Presenter Statistics");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Summary cards
        HBox summaryCards = new HBox(20);
        summaryCards.setAlignment(Pos.CENTER);

        VBox sessionsCard = createStatCard("Sessions Presented", "0", "#3498db");
        totalSessionsValue = (Label) sessionsCard.getChildren().get(1);

        VBox attendeesCard = createStatCard("Total Audience", "0", "#27ae60");
        totalAttendeesValue = (Label) attendeesCard.getChildren().get(1);

        VBox avgCard = createStatCard("Avg. Attendance", "0", "#9b59b6");
        avgAttendanceValue = (Label) avgCard.getChildren().get(1);

        summaryCards.getChildren().addAll(sessionsCard, attendeesCard, avgCard);

        // Charts
        HBox chartsBox = new HBox(20);
        VBox.setVgrow(chartsBox, Priority.ALWAYS);

        // Event type distribution pie chart
        eventTypeChart = new PieChart();
        eventTypeChart.setTitle("Event Type Distribution");
        eventTypeChart.setLegendVisible(true);

        // Audience size bar chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Session");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Audience Size");
        
        audienceChart = new BarChart<>(xAxis, yAxis);
        audienceChart.setTitle("Audience Size by Session");
        audienceChart.setLegendVisible(false);

        chartsBox.getChildren().addAll(eventTypeChart, audienceChart);
        HBox.setHgrow(eventTypeChart, Priority.ALWAYS);
        HBox.setHgrow(audienceChart, Priority.ALWAYS);

        // Export button
        HBox actionBar = new HBox(10);
        Button exportBtn = new Button("Export Activity Summary");
        exportBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        exportBtn.setOnAction(e -> exportActivitySummary());

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> loadStatisticsData());

        actionBar.getChildren().addAll(refreshBtn, exportBtn);

        view.getChildren().addAll(title, summaryCards, chartsBox, actionBar);
        return view;
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10;");
        card.setPrefWidth(200);
        card.setPrefHeight(100);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private void updateStatisticsView(Map<String, Object> stats) {
        if (stats == null) {
            totalSessionsValue.setText("0");
            totalAttendeesValue.setText("0");
            avgAttendanceValue.setText("0");
            return;
        }

        // Update summary cards
        Object sessions = stats.get("sessions_presented");
        totalSessionsValue.setText(sessions != null ? sessions.toString() : "0");

        Object attendees = stats.get("total_attendees");
        totalAttendeesValue.setText(attendees != null ? attendees.toString() : "0");

        Object avg = stats.get("average_attendance");
        avgAttendanceValue.setText(avg != null ? String.format("%.1f", avg) : "0");

        // Update pie chart using pre-calculated data from BulkDataLoader (NO additional DB calls!)
        // Only display valid EventType enum values: CONFERENCE, WORKSHOP, CONCERT, EXHIBITION
        eventTypeChart.getData().clear();
        if (cachedPresenterData != null && !cachedPresenterData.eventTypeStats.isEmpty()) {
            for (Map.Entry<String, Integer> entry : cachedPresenterData.eventTypeStats.entrySet()) {
                // Validate that the key is a valid EventType enum value
                try {
                    group_3.model.enums.EventType.valueOf(entry.getKey());
                    eventTypeChart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid event types - only show CONFERENCE, WORKSHOP, CONCERT, EXHIBITION
                    System.err.println("Skipping invalid event type: " + entry.getKey());
                }
            }
        }
        if (eventTypeChart.getData().isEmpty()) {
            eventTypeChart.getData().add(new PieChart.Data("No Data", 1));
        }

        // Update bar chart using pre-calculated audience data (NO additional DB calls!)
        audienceChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Audience");
        
        int sessionCount = 0;
        for (Session session : sessionList) {
            // Use pre-calculated audience count from cachedPresenterData
            int audienceSize = 0;
            if (cachedPresenterData != null && cachedPresenterData.sessionAudienceMap != null) {
                audienceSize = cachedPresenterData.sessionAudienceMap.getOrDefault(session.getSessionId(), 0);
            }
            
            String sessionLabel = session.getTitle().length() > 15 ? 
                session.getTitle().substring(0, 12) + "..." : session.getTitle();
            series.getData().add(new XYChart.Data<>(sessionLabel, audienceSize));
            sessionCount++;
            if (sessionCount >= 5) break; // Limit to 5 sessions for readability
        }
        
        if (!series.getData().isEmpty()) {
            audienceChart.getData().add(series);
        }
    }

    private void loadStatisticsData() {
        if (currentUser == null) return;
        try {
            // Use BulkDataLoader for single connection
            BulkDataLoader.PresenterData data = BulkDataLoader.loadPresenterData(currentUser.getId());
            // Update cache for chart data
            cachedPresenterData = data;
            Map<String, Object> stats = new HashMap<>();
            stats.put("sessions_presented", data.sessionsPresented);
            stats.put("total_attendees", data.totalAttendees);
            stats.put("average_attendance", Math.round(data.avgAttendance * 100.0) / 100.0);
            updateStatisticsView(stats);
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
        }
    }

    private void exportActivitySummary() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Activity Summary");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("presenter_activity_summary.csv");

        File file = fileChooser.showSaveDialog(scene.getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("Presenter Activity Summary");
                writer.println("Generated: " + LocalDate.now());
                writer.println();
                writer.println("Metric,Value");
                writer.println("Total Sessions," + totalSessionsValue.getText());
                writer.println("Total Audience," + totalAttendeesValue.getText());
                writer.println("Average Attendance," + avgAttendanceValue.getText());
                writer.println();
                writer.println("Sessions:");
                writer.println("Session ID,Title,Event ID,Venue,Capacity");
                for (Session s : sessionList) {
                    writer.printf("%d,%s,%d,%s,%d%n",
                        s.getSessionId(), s.getTitle(), s.getEventId(),
                        s.getVenue(), s.getCapacity());
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "Activity summary exported to " + file.getName());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Export failed: " + e.getMessage());
            }
        }
    }

    // ======================= TAB 4: PROFILE =======================

    private VBox createProfileView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(20));

        Label title = new Label("My Profile");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(15);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");

        // Username (read-only)
        form.add(new Label("Username:"), 0, 0);
        usernameField = new TextField();
        usernameField.setEditable(false);
        usernameField.setStyle("-fx-background-color: #f0f0f0;");
        form.add(usernameField, 1, 0);

        // Full Name
        form.add(new Label("Full Name:"), 0, 1);
        fullNameField = new TextField();
        form.add(fullNameField, 1, 1);

        // Date of Birth
        form.add(new Label("Date of Birth:"), 0, 2);
        dobPicker = new DatePicker();
        dobPicker.setEditable(false); // Prevent manual typing
        // Clear the editor to prevent invalid data parsing issues
        dobPicker.getEditor().setEditable(false);
        dobPicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? DATE_FORMATTER.format(date) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                try {
                    return (string != null && !string.isEmpty()) ? LocalDate.parse(string, DATE_FORMATTER) : null;
                } catch (Exception e) {
                    return null;
                }
            }
        });
        form.add(dobPicker, 1, 2);

        // Bio
        form.add(new Label("Bio:"), 0, 3);
        bioField = new TextField();
        bioField.setPromptText("Your professional bio");
        form.add(bioField, 1, 3);

        // Presenter Role
        form.add(new Label("Presenter Role:"), 0, 4);
        presenterRoleField = new TextField();
        presenterRoleField.setPromptText("e.g., Keynote Speaker, Guest Speaker");
        form.add(presenterRoleField, 1, 4);

        // Password section
        form.add(new Separator(), 0, 5, 2, 1);
        Label pwdLabel = new Label("Change Password");
        pwdLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        form.add(pwdLabel, 0, 6, 2, 1);

        form.add(new Label("Current Password:"), 0, 7);
        currentPasswordField = new PasswordField();
        form.add(currentPasswordField, 1, 7);

        form.add(new Label("New Password:"), 0, 8);
        newPasswordField = new PasswordField();
        form.add(newPasswordField, 1, 8);

        form.add(new Label("Confirm Password:"), 0, 9);
        confirmPasswordField = new PasswordField();
        form.add(confirmPasswordField, 1, 9);

        // Buttons
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Button saveBtn = new Button("Save Changes");
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> saveProfile());

        Button resetBtn = new Button("Reset");
        resetBtn.setOnAction(e -> loadProfileData());

        buttons.getChildren().addAll(saveBtn, resetBtn);

        view.getChildren().addAll(title, form, buttons);
        return view;
    }

    private void loadProfileData() {
        if (currentUser == null) return;
        
        usernameField.setText(currentUser.getUsername());
        fullNameField.setText(currentUser.getFullName());
        
        // Handle date of birth - may have invalid data in database
        try {
            if (currentUser.getDateOfBirth() != null) {
                dobPicker.setValue(currentUser.getDateOfBirth());
            } else {
                dobPicker.setValue(null);
                dobPicker.getEditor().clear();
            }
        } catch (Exception e) {
            System.err.println("Error setting date of birth: " + e.getMessage());
            dobPicker.setValue(null);
            dobPicker.getEditor().clear();
        }
        
        // Parse bio from contact information (JSON)
        String contact = currentUser.getContactInformation();
        if (contact != null && contact.contains("bio")) {
            // Simple extraction - in real code, use JSON parser
            bioField.setText(contact.replace("{\"bio\": \"", "").replace("\"}", ""));
        }
        
        if (currentPresenter != null) {
            presenterRoleField.setText(currentPresenter.getPresenterRole());
        }
        
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    private void saveProfile() {
        if (currentUser == null) return;

        try {
            currentUser.setFullName(fullNameField.getText());
            currentUser.setDateOfBirth(dobPicker.getValue());
            currentUser.setContactInformation("{\"bio\": \"" + bioField.getText() + "\"}");

            // Check if password change is requested
            String currentPwd = currentPasswordField.getText();
            String newPwd = newPasswordField.getText();
            String confirmPwd = confirmPasswordField.getText();

            if (!currentPwd.isEmpty() || !newPwd.isEmpty() || !confirmPwd.isEmpty()) {
                // Validate all password fields are filled
                if (currentPwd.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Please enter your current password.");
                    return;
                }
                if (newPwd.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Please enter a new password.");
                    return;
                }
                if (newPwd.length() < 6) {
                    showAlert(Alert.AlertType.ERROR, "Error", "New password must be at least 6 characters.");
                    return;
                }
                if (!newPwd.equals(confirmPwd)) {
                    showAlert(Alert.AlertType.ERROR, "Error", "New passwords do not match.");
                    return;
                }
                // Verify current password
                if (!PasswordUtil.verifyPassword(currentPwd, currentUser.getPasswordHash())) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Current password is incorrect.");
                    return;
                }
                // Hash and set new password
                String newHash = PasswordUtil.hash(newPwd);
                currentUser.setPasswordHash(newHash);
            }

            userService.updateUser(currentUser);

            // Update presenter role
            if (currentPresenter != null) {
                presenterService.updatePresenterRole(currentPresenter.getId(), presenterRoleField.getText());
            }

            showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update profile: " + e.getMessage());
        }
    }

    // ======================= UTILITY METHODS =======================

    private void handleLogout() {
        try {
            AuthContext.clear();
            LoginController loginController = new LoginController();
            Scene loginScene = loginController.getScene();
            if (loginScene != null) {
                Stage stage = (Stage) scene.getWindow();
                stage.setScene(loginScene);
                stage.setTitle("Login - Event Management System");
                stage.setWidth(500);
                stage.setHeight(600);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Scene getScene() {
        return scene;
    }
}
