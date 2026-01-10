package group_3.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import group_3.dao.ScheduleDAO;
import group_3.dao.TicketDAO;
import group_3.dao.impl.PersonDAOImpl;
import group_3.model.Event;
import group_3.model.Person;
import group_3.model.ScheduleEntry;
import group_3.model.Session;
import group_3.model.Ticket;
import group_3.model.enums.TicketStatus;
import group_3.model.enums.TicketType;
import group_3.security.AuthContext;
import group_3.service.AttendeeService.AttendeeService;
import group_3.service.AttendeeService.AttendeeServiceImpl;
import group_3.service.EventAdminService.EventAdminService;
import group_3.service.EventAdminService.EventAdminServiceImpl;
import group_3.service.RegistrationService.RegistrationService;
import group_3.service.RegistrationService.RegistrationServiceImpl;
import group_3.service.UserService.UserService;
import group_3.service.UserService.UserServiceImpl;
import group_3.util.BulkDataLoader;
import group_3.util.DaoProvider;
import group_3.util.PasswordUtil;
import group_3.util.QRCode;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;
import java.time.LocalDate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Dashboard Controller for Attendees.
 * Provides functionality for:
 * - Browsing and registering for events/sessions
 * - Viewing and managing tickets with filters
 * - Viewing personal schedule
 * - Viewing event history
 * - Managing profile
 * 
 * @author Group 3
 */
public class AttendeeDashboardController {

    // Services
    private final EventAdminService eventService;
    private final RegistrationService registrationService;
    private final AttendeeService attendeeService;
    private final UserService userService;
    private final TicketDAO ticketDAO;
    private final ScheduleDAO scheduleDAO;

    private Scene scene;
    private Person currentUser;

    // Browse Events Tab
    private TableView<Event> eventTable;
    private ObservableList<Event> eventList;
    private TableView<Session> sessionTable;
    private ObservableList<Session> sessionList;
    private ComboBox<TicketType> ticketTypeCombo;

    // My Tickets Tab
    private TableView<Ticket> ticketTable;
    private ObservableList<Ticket> ticketList;
    private ImageView qrImageView;
    private Label qrPlaceholder;

    // My Schedule Tab
    private TableView<ScheduleEntry> scheduleTable;
    private ObservableList<ScheduleEntry> scheduleList;

    // Profile Tab
    private TextField usernameField;
    private TextField fullNameField;
    private DatePicker dobPicker;
    private TextField contactField;
    private PasswordField currentPasswordField;
    private PasswordField newPasswordField;
    private PasswordField confirmPasswordField;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    // Loading indicator
    private StackPane loadingOverlay;
    private Label loadingLabel;
    
    // Cached data from BulkDataLoader (for instant session loading without DB calls)
    private BulkDataLoader.AttendeeData cachedAttendeeData;

    public AttendeeDashboardController() {
        this.eventService = new EventAdminServiceImpl();
        this.registrationService = new RegistrationServiceImpl();
        this.attendeeService = new AttendeeServiceImpl();
        this.userService = new UserServiceImpl(new PersonDAOImpl());
        this.ticketDAO = DaoProvider.getTicketDAO();
        this.scheduleDAO = DaoProvider.getScheduleDAO();
        this.currentUser = AuthContext.getCurrentUser();
        
        initializeUI();
    }

    private void initializeUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");
        root.setTop(createHeader());

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab browseTab = new Tab("Browse Events", createBrowseEventsView());
        Tab ticketsTab = new Tab("My Tickets", createMyTicketsView());
        Tab scheduleTab = new Tab("My Schedule", createMyScheduleView());
        Tab historyTab = new Tab("Event History", createEventHistoryView());
        Tab profileTab = new Tab("Profile", createProfileView());

        tabPane.getTabs().addAll(browseTab, ticketsTab, scheduleTab, historyTab, profileTab);
        
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

        Label titleLabel = new Label("Attendee Dashboard");
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
                int userId = currentUser != null ? currentUser.getId() : 0;
                BulkDataLoader.AttendeeData data = BulkDataLoader.loadAttendeeData(userId);
                
                // Cache the data for instant session loading
                cachedAttendeeData = data;

                Platform.runLater(() -> {
                    eventList.setAll(data.events);
                    ticketList.setAll(data.tickets);
                    scheduleList.setAll(data.schedules);
                    loadProfileData();
                    loadingOverlay.setVisible(false);
                  
                });
            } catch (Exception e) {
                System.err.println("[Attendee] Error loading data: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> loadingOverlay.setVisible(false));
            }
        });
        loadThread.setDaemon(true);
        loadThread.start();
    }

    // ======================= TAB 1: BROWSE EVENTS =======================

    private VBox createBrowseEventsView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("Browse & Register for Events");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Split pane for events and sessions
        SplitPane splitPane = new SplitPane();
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // Left: Events
        VBox eventsBox = new VBox(10);
        Label eventsLabel = new Label("1. Select an Event");
        eventsLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        eventTable = new TableView<>();
        eventList = FXCollections.observableArrayList();
        eventTable.setItems(eventList);
        VBox.setVgrow(eventTable, Priority.ALWAYS);

        TableColumn<Event, String> nameCol = new TableColumn<>("Event Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(180);

        TableColumn<Event, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getType() != null ? c.getValue().getType().toString() : "N/A"));
        typeCol.setPrefWidth(100);

        TableColumn<Event, String> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getStartDate() != null ? c.getValue().getStartDate().format(DATE_FORMATTER) : "N/A"));
        startDateCol.setPrefWidth(100);

        TableColumn<Event, String> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEndDate() != null ? c.getValue().getEndDate().format(DATE_FORMATTER) : "N/A"));
        endDateCol.setPrefWidth(100);

        TableColumn<Event, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        locationCol.setPrefWidth(120);

        eventTable.getColumns().addAll(nameCol, typeCol, startDateCol, endDateCol, locationCol);

        // Use cached sessions for instant loading (no delay!)
        eventTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Use pre-loaded cached sessions - NO database call needed!
                if (cachedAttendeeData != null && cachedAttendeeData.sessionsByEventId != null) {
                    List<Session> cachedSessions = cachedAttendeeData.sessionsByEventId.get(newVal.getEventId());
                    if (cachedSessions != null) {
                        sessionList.setAll(cachedSessions);
                    } else {
                        sessionList.clear();
                    }
                } else {
                    // Fallback to database call if cache is not available
                    loadSessionsForEvent(newVal.getEventId());
                }
            }
        });

        eventsBox.getChildren().addAll(eventsLabel, eventTable);

        // Right: Sessions and Registration
        VBox sessionsBox = new VBox(10);
        Label sessionsLabel = new Label("2. Select a Session");
        sessionsLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        sessionTable = new TableView<>();
        sessionList = FXCollections.observableArrayList();
        sessionTable.setItems(sessionList);
        VBox.setVgrow(sessionTable, Priority.ALWAYS);

        TableColumn<Session, String> sessionTitleCol = new TableColumn<>("Session Title");
        sessionTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        sessionTitleCol.setPrefWidth(200);

        TableColumn<Session, String> venueCol = new TableColumn<>("Venue");
        venueCol.setCellValueFactory(new PropertyValueFactory<>("venue"));
        venueCol.setPrefWidth(100);

        TableColumn<Session, Integer> capacityCol = new TableColumn<>("Capacity");
        capacityCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        capacityCol.setPrefWidth(80);

        TableColumn<Session, String> startTimeCol = new TableColumn<>("Start Time");
        startTimeCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStartTime() != null ? c.getValue().getStartTime().format(DATETIME_FORMATTER) : "N/A"));
        startTimeCol.setPrefWidth(120);

        TableColumn<Session, String> endTimeCol = new TableColumn<>("End Time");
        endTimeCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEndTime() != null ? c.getValue().getEndTime().format(DATETIME_FORMATTER) : "N/A"));
        endTimeCol.setPrefWidth(120);

        sessionTable.getColumns().addAll(sessionTitleCol, venueCol, capacityCol, startTimeCol, endTimeCol);

        // Registration controls
        Label ticketLabel = new Label("3. Choose Ticket Type");
        ticketLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        ticketTypeCombo = new ComboBox<>();
        ticketTypeCombo.getItems().addAll(TicketType.values());
        ticketTypeCombo.getSelectionModel().select(0);
        ticketTypeCombo.setMaxWidth(Double.MAX_VALUE);

        ticketTypeCombo.setConverter(new StringConverter<>() {
            public String toString(TicketType t) { return t == null ? "" : t + " - $" + getTicketPrice(t); }
            public TicketType fromString(String s) { return null; }
        });


        Button registerBtn = new Button("Register for Session");
        registerBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> handleRegister());

        sessionsBox.getChildren().addAll(sessionsLabel, sessionTable, new Separator(), 
            ticketLabel, ticketTypeCombo, registerBtn);

        splitPane.getItems().addAll(eventsBox, sessionsBox);
        splitPane.setDividerPositions(0.5);

        view.getChildren().addAll(title, splitPane);
        return view;
    }

   // In AttendeeDashboardController.java

private void loadSessionsForEvent(int eventId) {
    // 1. CLEAR existing data so the user doesn't see old sessions from the previous click
    sessionList.clear();

    // 2. Show loading feedback immediately
    loadingOverlay.setVisible(true);
    loadingLabel.setText("Loading sessions...");
    
    // 3. Create a background Task
    javafx.concurrent.Task<List<Session>> task = new javafx.concurrent.Task<>() {
        @Override
        protected List<Session> call() throws Exception {
            // This code runs in a BACKGROUND thread, so the UI won't freeze
            return eventService.getSessionsByEventId(eventId);
        }
    };

    // 4. Handle Success (Runs on UI Thread when data is ready)
    task.setOnSucceeded(e -> {
        List<Session> results = task.getValue();
        if (results != null) {
            sessionList.setAll(results);
        }
        loadingOverlay.setVisible(false);
    });

    // 5. Handle Failure (Runs on UI Thread if error occurs)
    task.setOnFailed(e -> {
        loadingOverlay.setVisible(false);
        Throwable error = task.getException();
        System.err.println("Error loading sessions: " + error.getMessage());
        // Optional: Show an alert to the user here
    });

    // 6. Start the background thread
    Thread thread = new Thread(task);
    thread.setDaemon(true); // Ensures thread closes if app closes
    thread.start();
}

    private void handleRegister() {
        Event selectedEvent = eventTable.getSelectionModel().getSelectedItem();
        Session selectedSession = sessionTable.getSelectionModel().getSelectedItem();
        TicketType ticketType = ticketTypeCombo.getValue();

        if (selectedEvent == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select an event.");
            return;
        }
        if (selectedSession == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a session.");
            return;
        }
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "You must be logged in to register.");
            return;
        }

        try {
            // Calculate ticket price (simplified - you may want to get this from session/event)
            double ticketPrice = 50.0; // Default for GENERAL

            if (ticketType == TicketType.VIP) {
                ticketPrice = 100.0;
            } else if (ticketType == TicketType.EARLYBIRD) {
                ticketPrice = 80.0;
            } else {
                ticketPrice = 50.0; // GENERAL
            }
            
            boolean success = registrationService.registerAttendee(
                currentUser.getId(), 
                selectedSession.getSessionId(), 
                ticketType,
                ticketPrice
            );
            
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "Registration successful!");
                loadTicketData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Registration failed. Possible schedule conflict.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Registration failed: " + e.getMessage());
        }
    }

    private double getTicketPrice(TicketType type) {
        if (type == null) return 0.0;
        switch (type) {
            case VIP:       return 100.0;
            case EARLYBIRD: return 80.0;
            default:        return 50.0; // GENERAL
        }
    }

    // ======================= TAB 2: MY TICKETS =======================

    private VBox createMyTicketsView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("My Tickets");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Filter bar
        HBox filterBar = new HBox(15);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All Types", "EARLYBIRD", "GENERAL", "VIP");
        typeFilter.setValue("All Types");

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All Status", "ACTIVE", "USED", "CANCELLED");
        statusFilter.setValue("All Status");

        Button filterBtn = new Button("Apply Filter");
        filterBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        filterBtn.setOnAction(e -> filterTickets(typeFilter.getValue(), statusFilter.getValue()));

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> loadTicketData());

        Button exportBtn = new Button("Export Tickets");
        exportBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        exportBtn.setOnAction(e -> exportTickets());

        filterBar.getChildren().addAll(
            new Label("Type:"), typeFilter,
            new Label("Status:"), statusFilter,
            filterBtn, refreshBtn, exportBtn
        );

        // Split pane for tickets and QR
        SplitPane splitPane = new SplitPane();
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // Left: Ticket list
        VBox ticketBox = new VBox(10);
        ticketTable = new TableView<>();
        ticketList = FXCollections.observableArrayList();
        ticketTable.setItems(ticketList);
        VBox.setVgrow(ticketTable, Priority.ALWAYS);

        TableColumn<Ticket, Integer> idCol = new TableColumn<>("Ticket #");
        idCol.setCellValueFactory(new PropertyValueFactory<>("ticketID"));
        idCol.setPrefWidth(80);

        TableColumn<Ticket, Integer> eventCol = new TableColumn<>("Event ID");
        eventCol.setCellValueFactory(new PropertyValueFactory<>("eventID"));
        eventCol.setPrefWidth(80);

        TableColumn<Ticket, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getType() != null ? c.getValue().getType().toString() : "N/A"));
        typeCol.setPrefWidth(100);

        TableColumn<Ticket, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(80);

        TableColumn<Ticket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getStatus() != null ? c.getValue().getStatus().toString() : "N/A"));
        statusCol.setPrefWidth(100);

        TableColumn<Ticket, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(100);
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button cancelBtn = new Button("Cancel");
            {
                cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                cancelBtn.setOnAction(e -> {
                    Ticket ticket = getTableRow().getItem();
                    if (ticket != null) handleCancelTicket(ticket);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Ticket ticket = getTableRow().getItem();
                    cancelBtn.setDisable(ticket.getStatus() != TicketStatus.ACTIVE);
                    setGraphic(cancelBtn);
                }
            }
        });

        ticketTable.getColumns().addAll(idCol, eventCol, typeCol, priceCol, statusCol, actionsCol);

        ticketTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showQRCode(newVal);
            }
        });

        ticketBox.getChildren().add(ticketTable);

        // Right: QR Code display
        VBox qrBox = new VBox(20);
        qrBox.setAlignment(Pos.CENTER);
        qrBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6;");
        qrBox.setPadding(new Insets(20));

        Label qrTitle = new Label("Entry QR Code");
        qrTitle.setFont(Font.font("System", FontWeight.BOLD, 16));

        qrImageView = new ImageView();
        qrImageView.setFitWidth(250);
        qrImageView.setFitHeight(250);
        qrImageView.setPreserveRatio(true);

        qrPlaceholder = new Label("Select a ticket to view QR code");
        qrPlaceholder.setStyle("-fx-text-fill: #adb5bd; -fx-font-style: italic;");

        qrBox.getChildren().addAll(qrTitle, qrImageView, qrPlaceholder);

        splitPane.getItems().addAll(ticketBox, qrBox);
        splitPane.setDividerPositions(0.6);

        view.getChildren().addAll(title, filterBar, splitPane);
        return view;
    }

    private void loadTicketData() {
        if (currentUser == null) return;
        try {
            List<Ticket> tickets = ticketDAO.findTicketByAttendeeId(currentUser.getId());
            ticketList.setAll(tickets);
        } catch (Exception e) {
            System.err.println("Error loading tickets: " + e.getMessage());
        }
    }

    private void filterTickets(String type, String status) {
        if (currentUser == null) return;
        try {
            List<Ticket> allTickets = ticketDAO.findTicketByAttendeeId(currentUser.getId());
            List<Ticket> filtered = allTickets.stream()
                .filter(t -> "All Types".equals(type) || t.getType().toString().equals(type))
                .filter(t -> "All Status".equals(status) || t.getStatus().toString().equals(status))
                .toList();
            ticketList.setAll(filtered);
        } catch (Exception e) {
            System.err.println("Error filtering tickets: " + e.getMessage());
        }
    }

    private void showQRCode(Ticket ticket) {
        try {
            String qrData = ticket.getQRpath();
            if (qrData == null || qrData.isEmpty()) {
                qrData = QRCode.generateTicketQRPayload(ticket);
            }
            Image qrImage = QRCode.generateQRImage(qrData, 250, 250);
            qrImageView.setImage(qrImage);
            qrPlaceholder.setVisible(false);
        } catch (Exception e) {
            qrPlaceholder.setText("Error generating QR code");
            qrPlaceholder.setVisible(true);
        }
    }

    private void handleCancelTicket(Ticket ticket) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Ticket");
        confirm.setHeaderText("Cancel Ticket #" + ticket.getTicketID());
        confirm.setContentText("Are you sure you want to cancel this ticket?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                registrationService.cancelRegistration(ticket.getTicketID());
                showAlert(Alert.AlertType.INFORMATION, "Success", "Ticket cancelled successfully.");
                loadTicketData();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to cancel: " + e.getMessage());
            }
        }
    }

    private void exportTickets() {
        if (ticketList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No tickets to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Tickets");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("my_tickets.csv");

        File file = fileChooser.showSaveDialog(scene.getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("Ticket ID,Event ID,Session ID,Type,Price,Status");
                for (Ticket t : ticketList) {
                    writer.printf("%d,%d,%d,%s,%.2f,%s%n",
                        t.getTicketID(), t.getEventID(), t.getSessionID(),
                        t.getType(), t.getPrice(), t.getStatus());
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "Tickets exported to " + file.getName());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Export failed: " + e.getMessage());
            }
        }
    }

    // ======================= TAB 3: MY SCHEDULE =======================

    private VBox createMyScheduleView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("My Personal Schedule");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        HBox actionBar = new HBox(10);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> loadScheduleData());

        Button exportBtn = new Button("Export Schedule");
        exportBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        exportBtn.setOnAction(e -> exportSchedule());

        actionBar.getChildren().addAll(refreshBtn, exportBtn);

        scheduleTable = new TableView<>();
        scheduleList = FXCollections.observableArrayList();
        scheduleTable.setItems(scheduleList);
        VBox.setVgrow(scheduleTable, Priority.ALWAYS);

        TableColumn<ScheduleEntry, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);

        TableColumn<ScheduleEntry, Integer> sessionCol = new TableColumn<>("Session ID");
        sessionCol.setCellValueFactory(new PropertyValueFactory<>("sessionID"));
        sessionCol.setPrefWidth(100);

        TableColumn<ScheduleEntry, String> startCol = new TableColumn<>("Start Time");
        startCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getStartTime() != null ? c.getValue().getStartTime().format(DATETIME_FORMATTER) : "N/A"));
        startCol.setPrefWidth(150);

        TableColumn<ScheduleEntry, String> endCol = new TableColumn<>("End Time");
        endCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getEndTime() != null ? c.getValue().getEndTime().format(DATETIME_FORMATTER) : "N/A"));
        endCol.setPrefWidth(150);

        scheduleTable.getColumns().addAll(idCol, sessionCol, startCol, endCol);

        view.getChildren().addAll(title, actionBar, scheduleTable);
        return view;
    }

    private void loadScheduleData() {
        if (currentUser == null) return;
        try {
            List<ScheduleEntry> schedules = scheduleDAO.findAllScheduleByUserId(currentUser.getId());
            scheduleList.setAll(schedules);
        } catch (Exception e) {
            System.err.println("Error loading schedule: " + e.getMessage());
        }
    }

    private void exportSchedule() {
        if (scheduleList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No schedule entries to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Schedule");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("my_schedule.csv");

        File file = fileChooser.showSaveDialog(scene.getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("ID,Session ID,Start Time,End Time");
                for (ScheduleEntry s : scheduleList) {
                    writer.printf("%d,%d,%s,%s%n",
                        s.getId(), s.getSessionID(),
                        s.getStartTime() != null ? s.getStartTime().format(DATETIME_FORMATTER) : "",
                        s.getEndTime() != null ? s.getEndTime().format(DATETIME_FORMATTER) : "");
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "Schedule exported to " + file.getName());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Export failed: " + e.getMessage());
            }
        }
    }

    // ======================= TAB 4: EVENT HISTORY =======================

    private VBox createEventHistoryView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("My Event History");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        Label description = new Label("Events you have attended or registered for:");
        description.setStyle("-fx-text-fill: #7f8c8d;");

        TableView<Ticket> historyTable = new TableView<>();
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        TableColumn<Ticket, Integer> ticketCol = new TableColumn<>("Ticket #");
        ticketCol.setCellValueFactory(new PropertyValueFactory<>("ticketID"));
        ticketCol.setPrefWidth(80);

        TableColumn<Ticket, Integer> eventCol = new TableColumn<>("Event ID");
        eventCol.setCellValueFactory(new PropertyValueFactory<>("eventID"));
        eventCol.setPrefWidth(80);

        TableColumn<Ticket, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getType() != null ? c.getValue().getType().toString() : "N/A"));
        typeCol.setPrefWidth(100);

        TableColumn<Ticket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getStatus() != null ? c.getValue().getStatus().toString() : "N/A"));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "ACTIVE" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        case "USED" -> setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                        case "CANCELLED" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        case "EXPIRED" -> setStyle("-fx-text-fill: #7f8c8d;");
                        default -> setStyle("");
                    }
                }
            }
        });

        historyTable.getColumns().addAll(ticketCol, eventCol, typeCol, statusCol);

        // Load history data
        if (currentUser != null) {
            try {
                List<Ticket> history = ticketDAO.findTicketByAttendeeId(currentUser.getId());
                historyTable.setItems(FXCollections.observableArrayList(history));
            } catch (Exception e) {
                System.err.println("Error loading history: " + e.getMessage());
            }
        }

        view.getChildren().addAll(title, description, historyTable);
        return view;
    }

    // ======================= TAB 5: PROFILE =======================

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

        // Contact
        form.add(new Label("Contact Info:"), 0, 3);
        contactField = new TextField();
        form.add(contactField, 1, 3);

        // Password section
        form.add(new Separator(), 0, 4, 2, 1);
        form.add(new Label("Change Password"), 0, 5, 2, 1);

        form.add(new Label("Current Password:"), 0, 6);
        currentPasswordField = new PasswordField();
        form.add(currentPasswordField, 1, 6);

        form.add(new Label("New Password:"), 0, 7);
        newPasswordField = new PasswordField();
        form.add(newPasswordField, 1, 7);

        form.add(new Label("Confirm Password:"), 0, 8);
        confirmPasswordField = new PasswordField();
        form.add(confirmPasswordField, 1, 8);

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
        
        contactField.setText(currentUser.getContactInformation());
        
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    private void saveProfile() {
        if (currentUser == null) return;

        try {
            currentUser.setFullName(fullNameField.getText());
            currentUser.setDateOfBirth(dobPicker.getValue());
            currentUser.setContactInformation(contactField.getText());

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
