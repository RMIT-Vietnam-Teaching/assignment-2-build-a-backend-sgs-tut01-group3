package group_3.service.EventAdminService;

import group_3.dao.EventDAO;
import group_3.dao.SessionDAO;
import group_3.dao.TicketDAO;
import group_3.model.Event;
import group_3.model.Session;
import group_3.model.Ticket;
import group_3.model.enums.EventStatus;
import group_3.model.enums.EventType;
import group_3.model.enums.TicketStatus;
import group_3.model.enums.TicketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
/**
 * Comprehensive unit tests for EventAdminServiceImpl.
 * Tests cover all CRUD operations for events and sessions,
 * as well as presenter assignment functionality.
 * 
 * @author Group 3
 */
@ExtendWith(MockitoExtension.class)
public class EventAdminServiceImplTest {

    @Mock
    private EventDAO eventDAO;

    @Mock
    private SessionDAO sessionDAO;

    @Mock
    private TicketDAO ticketDAO;

    private EventAdminServiceImpl eventAdminService;

    private Event testEvent;
    private Session testSession;
    private Ticket testTicket;

    @BeforeEach
    void setUp() {
        eventAdminService = new EventAdminServiceImpl(eventDAO, sessionDAO, ticketDAO);
        
        // Setup test event
        testEvent = new Event(
            "1",
            "Tech Conference 2025",
            EventType.CONFERENCE,
            LocalDateTime.of(2025, 6, 15, 9, 0),
            LocalDateTime.of(2025, 6, 17, 18, 0),
            "Convention Center",
            3,
            EventStatus.SCHEDULED
        );

        // Setup test session
        testSession = new Session(
            "1",
            "1",
            "Keynote Speech",
            "Opening keynote by industry expert",
            LocalDateTime.of(2025, 6, 15, 9, 0),
            LocalDateTime.of(2025, 6, 15, 10, 30),
            "Main Hall",
            500
        );

        // Setup test ticket
        testTicket = new Ticket(
            1,
            1,
            1,
            100,
            TicketType.GENERAL,
            50.0,
            TicketStatus.ACTIVE,
            "QR-123"
        );
    }

    // ==================== EVENT OPERATIONS TESTS ====================

    @Nested
    @DisplayName("Event Create Tests")
    class EventCreateTests {

        @Test
        @DisplayName("Should create event successfully")
        void createEvent_Success() {
            // Arrange
            doAnswer(invocation -> {
                Event event = invocation.getArgument(0);
                event.setEventId("1");
                return null;
            }).when(eventDAO).create(any(Event.class));

            // Act
            Event result = eventAdminService.createEvent(testEvent);

            // Assert
            assertNotNull(result);
            assertEquals("1", result.getEventId());
            assertEquals("Tech Conference 2025", result.getName());
            verify(eventDAO, times(1)).create(testEvent);
        }

        @Test
        @DisplayName("Should throw exception when event is null")
        void createEvent_NullEvent_ThrowsException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.createEvent(null)
            );
            assertEquals("Event cannot be null", exception.getMessage());
            verify(eventDAO, never()).create(any());
        }

        @Test
        @DisplayName("Should throw exception when event name is null")
        void createEvent_NullName_ThrowsException() {
            // Arrange
            testEvent.setName(null);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.createEvent(testEvent)
            );
            assertEquals("Event name cannot be null or empty", exception.getMessage());
            verify(eventDAO, never()).create(any());
        }

        @Test
        @DisplayName("Should throw exception when event name is empty")
        void createEvent_EmptyName_ThrowsException() {
            // Arrange
            testEvent.setName("   ");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.createEvent(testEvent)
            );
            assertEquals("Event name cannot be null or empty", exception.getMessage());
            verify(eventDAO, never()).create(any());
        }
    }

    @Nested
    @DisplayName("Event Read Tests")
    class EventReadTests {

        @Test
        @DisplayName("Should get event by ID successfully")
        void getEventById_Found() {
            // Arrange
            when(eventDAO.findById(1)).thenReturn(Optional.of(testEvent));

            // Act
            Optional<Event> result = eventAdminService.getEventById(1);

            // Assert
            assertTrue(result.isPresent());
            assertEquals("Tech Conference 2025", result.get().getName());
            verify(eventDAO, times(1)).findById(1);
        }

        @Test
        @DisplayName("Should return empty when event not found")
        void getEventById_NotFound() {
            // Arrange
            when(eventDAO.findById(999)).thenReturn(Optional.empty());

            // Act
            Optional<Event> result = eventAdminService.getEventById(999);

            // Assert
            assertFalse(result.isPresent());
            verify(eventDAO, times(1)).findById(999);
        }

        @Test
        @DisplayName("Should return empty for invalid ID")
        void getEventById_InvalidId() {
            // Act
            Optional<Event> result = eventAdminService.getEventById(0);

            // Assert
            assertFalse(result.isPresent());
            verify(eventDAO, never()).findById(anyInt());
        }

        @Test
        @DisplayName("Should return empty for negative ID")
        void getEventById_NegativeId() {
            // Act
            Optional<Event> result = eventAdminService.getEventById(-1);

            // Assert
            assertFalse(result.isPresent());
            verify(eventDAO, never()).findById(anyInt());
        }

        @Test
        @DisplayName("Should get event by name successfully")
        void getEventByName_Found() {
            // Arrange
            when(eventDAO.findByName("Tech Conference 2025")).thenReturn(Optional.of(testEvent));

            // Act
            Optional<Event> result = eventAdminService.getEventByName("Tech Conference 2025");

            // Assert
            assertTrue(result.isPresent());
            assertEquals("1", result.get().getEventId());
            verify(eventDAO, times(1)).findByName("Tech Conference 2025");
        }

        @Test
        @DisplayName("Should return empty when event name not found")
        void getEventByName_NotFound() {
            // Arrange
            when(eventDAO.findByName("Nonexistent Event")).thenReturn(Optional.empty());

            // Act
            Optional<Event> result = eventAdminService.getEventByName("Nonexistent Event");

            // Assert
            assertFalse(result.isPresent());
            verify(eventDAO, times(1)).findByName("Nonexistent Event");
        }

        @Test
        @DisplayName("Should return empty for null name")
        void getEventByName_NullName() {
            // Act
            Optional<Event> result = eventAdminService.getEventByName(null);

            // Assert
            assertFalse(result.isPresent());
            verify(eventDAO, never()).findByName(any());
        }

        @Test
        @DisplayName("Should return empty for empty name")
        void getEventByName_EmptyName() {
            // Act
            Optional<Event> result = eventAdminService.getEventByName("  ");

            // Assert
            assertFalse(result.isPresent());
            verify(eventDAO, never()).findByName(any());
        }

        @Test
        @DisplayName("Should get all events successfully")
        void getAllEvents_Success() {
            // Arrange
            Event event2 = new Event(
                "2",
                "Workshop 2025",
                EventType.WORKSHOP,
                LocalDateTime.of(2025, 7, 1, 10, 0),
                LocalDateTime.of(2025, 7, 1, 16, 0),
                "Training Room",
                1,
                EventStatus.SCHEDULED
            );
            when(eventDAO.findAll()).thenReturn(Arrays.asList(testEvent, event2));

            // Act
            List<Event> result = eventAdminService.getAllEvents();

            // Assert
            assertEquals(2, result.size());
            verify(eventDAO, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no events")
        void getAllEvents_Empty() {
            // Arrange
            when(eventDAO.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<Event> result = eventAdminService.getAllEvents();

            // Assert
            assertTrue(result.isEmpty());
            verify(eventDAO, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("Event Update Tests")
    class EventUpdateTests {

        @Test
        @DisplayName("Should update event successfully")
        void updateEvent_Success() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);
            testEvent.setName("Updated Conference Name");

            // Act
            eventAdminService.updateEvent(testEvent);

            // Assert
            verify(eventDAO, times(1)).update(testEvent);
        }

        @Test
        @DisplayName("Should throw exception when event is null")
        void updateEvent_NullEvent_ThrowsException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.updateEvent(null)
            );
            assertEquals("Event cannot be null", exception.getMessage());
            verify(eventDAO, never()).update(any());
        }

        @Test
        @DisplayName("Should throw exception when event ID is null")
        void updateEvent_NullId_ThrowsException() {
            // Arrange
            testEvent.setEventId(null);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.updateEvent(testEvent)
            );
            assertEquals("Event ID cannot be null or empty", exception.getMessage());
            verify(eventDAO, never()).update(any());
        }

        @Test
        @DisplayName("Should throw exception when event does not exist")
        void updateEvent_EventNotExists_ThrowsException() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.updateEvent(testEvent)
            );
            assertEquals("Event with ID 1 does not exist", exception.getMessage());
            verify(eventDAO, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Event Delete Tests")
    class EventDeleteTests {

        @Test
        @DisplayName("Should delete event successfully")
        void deleteEvent_Success() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);
            when(sessionDAO.findByEventId(1)).thenReturn(Collections.emptyList());

            // Act
            eventAdminService.deleteEvent(1);

            // Assert
            verify(eventDAO, times(1)).delete(1);
        }

        @Test
        @DisplayName("Should delete event with sessions")
        void deleteEvent_WithSessions() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);
            when(sessionDAO.findByEventId(1)).thenReturn(Collections.singletonList(testSession));

            // Act
            eventAdminService.deleteEvent(1);

            // Assert
            verify(sessionDAO, times(1)).delete(1);
            verify(eventDAO, times(1)).delete(1);
        }

        @Test
        @DisplayName("Should throw exception for invalid ID")
        void deleteEvent_InvalidId_ThrowsException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.deleteEvent(0)
            );
            assertEquals("Event ID must be positive", exception.getMessage());
            verify(eventDAO, never()).delete(anyInt());
        }

        @Test
        @DisplayName("Should throw exception when event does not exist")
        void deleteEvent_EventNotExists_ThrowsException() {
            // Arrange
            when(eventDAO.exists(999)).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.deleteEvent(999)
            );
            assertEquals("Event with ID 999 does not exist", exception.getMessage());
            verify(eventDAO, never()).delete(anyInt());
        }
    }

    @Nested
    @DisplayName("Event Count Tests")
    class EventCountTests {

        @Test
        @DisplayName("Should get event count successfully")
        void getEventCount_Success() {
            // Arrange
            when(eventDAO.count()).thenReturn(5);

            // Act
            int result = eventAdminService.getEventCount();

            // Assert
            assertEquals(5, result);
            verify(eventDAO, times(1)).count();
        }

        @Test
        @DisplayName("Should return zero when no events")
        void getEventCount_NoEvents() {
            // Arrange
            when(eventDAO.count()).thenReturn(0);

            // Act
            int result = eventAdminService.getEventCount();

            // Assert
            assertEquals(0, result);
            verify(eventDAO, times(1)).count();
        }
    }

    // ==================== SESSION OPERATIONS TESTS ====================

    @Nested
    @DisplayName("Session Create Tests")
    class SessionCreateTests {

        @Test
        @DisplayName("Should create session successfully")
        void createSession_Success() {
            // Arrange
            doAnswer(invocation -> {
                Session session = invocation.getArgument(0);
                session.setSessionId("1");
                return null;
            }).when(sessionDAO).create(any(Session.class));

            // Act
            Session result = eventAdminService.createSession(testSession);

            // Assert
            assertNotNull(result);
            assertEquals("1", result.getSessionId());
            assertEquals("Keynote Speech", result.getTitle());
            verify(sessionDAO, times(1)).create(testSession);
        }

        @Test
        @DisplayName("Should throw exception when session is null")
        void createSession_NullSession_ThrowsException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.createSession(null)
            );
            assertEquals("Session cannot be null", exception.getMessage());
            verify(sessionDAO, never()).create(any());
        }

        @Test
        @DisplayName("Should throw exception when session title is null")
        void createSession_NullTitle_ThrowsException() {
            // Arrange
            testSession.setTitle(null);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.createSession(testSession)
            );
            assertEquals("Session title cannot be null or empty", exception.getMessage());
            verify(sessionDAO, never()).create(any());
        }

        @Test
        @DisplayName("Should throw exception when session title is empty")
        void createSession_EmptyTitle_ThrowsException() {
            // Arrange
            testSession.setTitle("   ");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.createSession(testSession)
            );
            assertEquals("Session title cannot be null or empty", exception.getMessage());
            verify(sessionDAO, never()).create(any());
        }
    }

    @Nested
    @DisplayName("Session Read Tests")
    class SessionReadTests {

        @Test
        @DisplayName("Should get session by ID successfully")
        void getSessionById_Found() {
            // Arrange
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));

            // Act
            Optional<Session> result = eventAdminService.getSessionById(1);

            // Assert
            assertTrue(result.isPresent());
            assertEquals("Keynote Speech", result.get().getTitle());
            verify(sessionDAO, times(1)).findById(1);
        }

        @Test
        @DisplayName("Should return empty when session not found")
        void getSessionById_NotFound() {
            // Arrange
            when(sessionDAO.findById(999)).thenReturn(Optional.empty());

            // Act
            Optional<Session> result = eventAdminService.getSessionById(999);

            // Assert
            assertFalse(result.isPresent());
            verify(sessionDAO, times(1)).findById(999);
        }

        @Test
        @DisplayName("Should return empty for invalid session ID")
        void getSessionById_InvalidId() {
            // Act
            Optional<Session> result = eventAdminService.getSessionById(0);

            // Assert
            assertFalse(result.isPresent());
            verify(sessionDAO, never()).findById(anyInt());
        }

        @Test
        @DisplayName("Should get session by title successfully")
        void getSessionByTitle_Found() {
            // Arrange
            when(sessionDAO.findByTitle("Keynote Speech")).thenReturn(Optional.of(testSession));

            // Act
            Optional<Session> result = eventAdminService.getSessionByTitle("Keynote Speech");

            // Assert
            assertTrue(result.isPresent());
            assertEquals("1", result.get().getSessionId());
            verify(sessionDAO, times(1)).findByTitle("Keynote Speech");
        }

        @Test
        @DisplayName("Should return empty for null title")
        void getSessionByTitle_NullTitle() {
            // Act
            Optional<Session> result = eventAdminService.getSessionByTitle(null);

            // Assert
            assertFalse(result.isPresent());
            verify(sessionDAO, never()).findByTitle(any());
        }

        @Test
        @DisplayName("Should get all sessions successfully")
        void getAllSessions_Success() {
            // Arrange
            Session session2 = new Session(
                "2",
                "1",
                "Panel Discussion",
                "Industry leaders discuss trends",
                LocalDateTime.of(2025, 6, 15, 11, 0),
                LocalDateTime.of(2025, 6, 15, 12, 30),
                "Room A",
                200
            );
            when(sessionDAO.findAll()).thenReturn(Arrays.asList(testSession, session2));

            // Act
            List<Session> result = eventAdminService.getAllSessions();

            // Assert
            assertEquals(2, result.size());
            verify(sessionDAO, times(1)).findAll();
        }

        @Test
        @DisplayName("Should get sessions by event ID successfully")
        void getSessionsByEventId_Success() {
            // Arrange
            when(sessionDAO.findByEventId(1)).thenReturn(Collections.singletonList(testSession));

            // Act
            List<Session> result = eventAdminService.getSessionsByEventId(1);

            // Assert
            assertEquals(1, result.size());
            assertEquals("Keynote Speech", result.get(0).getTitle());
            verify(sessionDAO, times(1)).findByEventId(1);
        }

        @Test
        @DisplayName("Should return empty list for invalid event ID")
        void getSessionsByEventId_InvalidId() {
            // Act
            List<Session> result = eventAdminService.getSessionsByEventId(0);

            // Assert
            assertTrue(result.isEmpty());
            verify(sessionDAO, never()).findByEventId(anyInt());
        }
    }

    @Nested
    @DisplayName("Session Update Tests")
    class SessionUpdateTests {

        @Test
        @DisplayName("Should update session successfully")
        void updateSession_Success() {
            // Arrange
            when(sessionDAO.exists(1)).thenReturn(true);
            testSession.setTitle("Updated Keynote Speech");

            // Act
            eventAdminService.updateSession(testSession);

            // Assert
            verify(sessionDAO, times(1)).update(testSession);
        }

        @Test
        @DisplayName("Should throw exception when session is null")
        void updateSession_NullSession_ThrowsException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.updateSession(null)
            );
            assertEquals("Session cannot be null", exception.getMessage());
            verify(sessionDAO, never()).update(any());
        }

        @Test
        @DisplayName("Should throw exception when session ID is null")
        void updateSession_NullId_ThrowsException() {
            // Arrange
            testSession.setSessionId(null);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.updateSession(testSession)
            );
            assertEquals("Session ID cannot be null or empty", exception.getMessage());
            verify(sessionDAO, never()).update(any());
        }

        @Test
        @DisplayName("Should throw exception when session does not exist")
        void updateSession_SessionNotExists_ThrowsException() {
            // Arrange
            when(sessionDAO.exists(1)).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.updateSession(testSession)
            );
            assertEquals("Session with ID 1 does not exist", exception.getMessage());
            verify(sessionDAO, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Session Delete Tests")
    class SessionDeleteTests {

        @Test
        @DisplayName("Should delete session successfully")
        void deleteSession_Success() {
            // Arrange
            when(sessionDAO.exists(1)).thenReturn(true);

            // Act
            eventAdminService.deleteSession(1);

            // Assert
            verify(sessionDAO, times(1)).delete(1);
        }

        @Test
        @DisplayName("Should throw exception for invalid session ID")
        void deleteSession_InvalidId_ThrowsException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.deleteSession(0)
            );
            assertEquals("Session ID must be positive", exception.getMessage());
            verify(sessionDAO, never()).delete(anyInt());
        }

        @Test
        @DisplayName("Should throw exception when session does not exist")
        void deleteSession_SessionNotExists_ThrowsException() {
            // Arrange
            when(sessionDAO.exists(999)).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.deleteSession(999)
            );
            assertEquals("Session with ID 999 does not exist", exception.getMessage());
            verify(sessionDAO, never()).delete(anyInt());
        }
    }

    @Nested
    @DisplayName("Session Count Tests")
    class SessionCountTests {

        @Test
        @DisplayName("Should get session count successfully")
        void getSessionCount_Success() {
            // Arrange
            when(sessionDAO.count()).thenReturn(10);

            // Act
            int result = eventAdminService.getSessionCount();

            // Assert
            assertEquals(10, result);
            verify(sessionDAO, times(1)).count();
        }

        @Test
        @DisplayName("Should get session count by event successfully")
        void getSessionCountByEvent_Success() {
            // Arrange
            when(sessionDAO.countByEventId(1)).thenReturn(3);

            // Act
            int result = eventAdminService.getSessionCountByEvent(1);

            // Assert
            assertEquals(3, result);
            verify(sessionDAO, times(1)).countByEventId(1);
        }

        @Test
        @DisplayName("Should return zero for invalid event ID")
        void getSessionCountByEvent_InvalidId() {
            // Act
            int result = eventAdminService.getSessionCountByEvent(0);

            // Assert
            assertEquals(0, result);
            verify(sessionDAO, never()).countByEventId(anyInt());
        }
    }

    // ==================== COMBINED OPERATIONS TESTS ====================

    @Nested
    @DisplayName("Combined Operations Tests")
    class CombinedOperationsTests {

        @Test
        @DisplayName("Should add session to event successfully")
        void addSessionToEvent_Success() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);
            when(sessionDAO.exists(1)).thenReturn(true);
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));
            when(eventDAO.findById(1)).thenReturn(Optional.of(testEvent));

            // Act
            eventAdminService.addSessionToEvent(1, 1);

            // Assert
            verify(sessionDAO, times(1)).update(any(Session.class));
        }

        @Test
        @DisplayName("Should throw exception when adding session with invalid event ID")
        void addSessionToEvent_InvalidEventId() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.addSessionToEvent(0, 1)
            );
            assertEquals("Event ID and Session ID must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when event does not exist")
        void addSessionToEvent_EventNotExists() {
            // Arrange
            when(eventDAO.exists(999)).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.addSessionToEvent(999, 1)
            );
            assertEquals("Event with ID 999 does not exist", exception.getMessage());
        }

        @Test
        @DisplayName("Should remove session from event successfully")
        void removeSessionFromEvent_Success() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);
            when(sessionDAO.exists(1)).thenReturn(true);
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));
            when(eventDAO.findById(1)).thenReturn(Optional.of(testEvent));

            // Act
            eventAdminService.removeSessionFromEvent(1, 1);

            // Assert
            verify(sessionDAO, times(1)).update(any(Session.class));
        }

        @Test
        @DisplayName("Should get event with sessions successfully")
        void getEventWithSessions_Success() {
            // Arrange
            when(eventDAO.findById(1)).thenReturn(Optional.of(testEvent));
            when(sessionDAO.findByEventId(1)).thenReturn(Collections.singletonList(testSession));

            // Act
            Optional<Event> result = eventAdminService.getEventWithSessions(1);

            // Assert
            assertTrue(result.isPresent());
            assertFalse(result.get().getSessionIds().isEmpty());
            verify(eventDAO, times(1)).findById(1);
            verify(sessionDAO, times(1)).findByEventId(1);
        }

        @Test
        @DisplayName("Should return empty for invalid event ID")
        void getEventWithSessions_InvalidId() {
            // Act
            Optional<Event> result = eventAdminService.getEventWithSessions(0);

            // Assert
            assertFalse(result.isPresent());
            verify(eventDAO, never()).findById(anyInt());
        }
    }

    // ==================== EXISTENCE CHECKS TESTS ====================

    @Nested
    @DisplayName("Existence Check Tests")
    class ExistenceCheckTests {

        @Test
        @DisplayName("Should return true when event exists")
        void eventExists_True() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);

            // Act
            boolean result = eventAdminService.eventExists(1);

            // Assert
            assertTrue(result);
            verify(eventDAO, times(1)).exists(1);
        }

        @Test
        @DisplayName("Should return false when event does not exist")
        void eventExists_False() {
            // Arrange
            when(eventDAO.exists(999)).thenReturn(false);

            // Act
            boolean result = eventAdminService.eventExists(999);

            // Assert
            assertFalse(result);
            verify(eventDAO, times(1)).exists(999);
        }

        @Test
        @DisplayName("Should return false for invalid event ID")
        void eventExists_InvalidId() {
            // Act
            boolean result = eventAdminService.eventExists(0);

            // Assert
            assertFalse(result);
            verify(eventDAO, never()).exists(anyInt());
        }

        @Test
        @DisplayName("Should return true when session exists")
        void sessionExists_True() {
            // Arrange
            when(sessionDAO.exists(1)).thenReturn(true);

            // Act
            boolean result = eventAdminService.sessionExists(1);

            // Assert
            assertTrue(result);
            verify(sessionDAO, times(1)).exists(1);
        }

        @Test
        @DisplayName("Should return false when session does not exist")
        void sessionExists_False() {
            // Arrange
            when(sessionDAO.exists(999)).thenReturn(false);

            // Act
            boolean result = eventAdminService.sessionExists(999);

            // Assert
            assertFalse(result);
            verify(sessionDAO, times(1)).exists(999);
        }

        @Test
        @DisplayName("Should return false for invalid session ID")
        void sessionExists_InvalidId() {
            // Act
            boolean result = eventAdminService.sessionExists(-1);

            // Assert
            assertFalse(result);
            verify(sessionDAO, never()).exists(anyInt());
        }
    }

    // ==================== PRESENTER ASSIGNMENT TESTS ====================

    @Nested
    @DisplayName("Presenter Assignment Tests")
    class PresenterAssignmentTests {

        @Test
        @DisplayName("Should assign presenter to session successfully")
        void assignPresenterToSession_Success() {
            // Arrange
            when(sessionDAO.exists(1)).thenReturn(true);
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));

            // Act
            boolean result = eventAdminService.assignPresenterToSession(1, "presenter-1");

            // Assert
            assertTrue(result);
            verify(sessionDAO, times(1)).update(any(Session.class));
        }

        @Test
        @DisplayName("Should return false when presenter already assigned")
        void assignPresenterToSession_AlreadyAssigned() {
            // Arrange
            testSession.addPresenter("presenter-1");
            when(sessionDAO.exists(1)).thenReturn(true);
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));

            // Act
            boolean result = eventAdminService.assignPresenterToSession(1, "presenter-1");

            // Assert
            assertFalse(result);
            verify(sessionDAO, never()).update(any(Session.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid session ID")
        void assignPresenterToSession_InvalidSessionId() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.assignPresenterToSession(0, "presenter-1")
            );
            assertEquals("Session ID must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null presenter ID")
        void assignPresenterToSession_NullPresenterId() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.assignPresenterToSession(1, null)
            );
            assertEquals("Presenter ID cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for empty presenter ID")
        void assignPresenterToSession_EmptyPresenterId() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.assignPresenterToSession(1, "  ")
            );
            assertEquals("Presenter ID cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when session does not exist")
        void assignPresenterToSession_SessionNotExists() {
            // Arrange
            when(sessionDAO.exists(999)).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.assignPresenterToSession(999, "presenter-1")
            );
            assertEquals("Session with ID 999 does not exist", exception.getMessage());
        }

        @Test
        @DisplayName("Should unassign presenter from session successfully")
        void unassignPresenterFromSession_Success() {
            // Arrange
            testSession.addPresenter("presenter-1");
            when(sessionDAO.exists(1)).thenReturn(true);
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));

            // Act
            boolean result = eventAdminService.unassignPresenterFromSession(1, "presenter-1");

            // Assert
            assertTrue(result);
            verify(sessionDAO, times(1)).update(any(Session.class));
        }

        @Test
        @DisplayName("Should return false when presenter not assigned")
        void unassignPresenterFromSession_NotAssigned() {
            // Arrange
            when(sessionDAO.exists(1)).thenReturn(true);
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));

            // Act
            boolean result = eventAdminService.unassignPresenterFromSession(1, "presenter-1");

            // Assert
            assertFalse(result);
            verify(sessionDAO, never()).update(any(Session.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid session ID when unassigning")
        void unassignPresenterFromSession_InvalidSessionId() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.unassignPresenterFromSession(-1, "presenter-1")
            );
            assertEquals("Session ID must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null presenter ID when unassigning")
        void unassignPresenterFromSession_NullPresenterId() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.unassignPresenterFromSession(1, null)
            );
            assertEquals("Presenter ID cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should get presenters by session ID successfully")
        void getPresentersBySessionId_Success() {
            // Arrange
            testSession.addPresenter("presenter-1");
            testSession.addPresenter("presenter-2");
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));

            // Act
            List<String> result = eventAdminService.getPresentersBySessionId(1);

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.contains("presenter-1"));
            assertTrue(result.contains("presenter-2"));
        }

        @Test
        @DisplayName("Should return empty list for invalid session ID")
        void getPresentersBySessionId_InvalidId() {
            // Act
            List<String> result = eventAdminService.getPresentersBySessionId(0);

            // Assert
            assertTrue(result.isEmpty());
            verify(sessionDAO, never()).findById(anyInt());
        }

        @Test
        @DisplayName("Should return empty list when session not found")
        void getPresentersBySessionId_SessionNotFound() {
            // Arrange
            when(sessionDAO.findById(999)).thenReturn(Optional.empty());

            // Act
            List<String> result = eventAdminService.getPresentersBySessionId(999);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should check if presenter is assigned to session")
        void isPresenterAssignedToSession_True() {
            // Arrange
            testSession.addPresenter("presenter-1");
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));

            // Act
            boolean result = eventAdminService.isPresenterAssignedToSession(1, "presenter-1");

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when presenter is not assigned")
        void isPresenterAssignedToSession_False() {
            // Arrange
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));

            // Act
            boolean result = eventAdminService.isPresenterAssignedToSession(1, "presenter-1");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for invalid session ID when checking assignment")
        void isPresenterAssignedToSession_InvalidSessionId() {
            // Act
            boolean result = eventAdminService.isPresenterAssignedToSession(0, "presenter-1");

            // Assert
            assertFalse(result);
            verify(sessionDAO, never()).findById(anyInt());
        }

        @Test
        @DisplayName("Should return false for null presenter ID when checking assignment")
        void isPresenterAssignedToSession_NullPresenterId() {
            // Act
            boolean result = eventAdminService.isPresenterAssignedToSession(1, null);

            // Assert
            assertFalse(result);
            verify(sessionDAO, never()).findById(anyInt());
        }

        @Test
        @DisplayName("Should return false when session not found")
        void isPresenterAssignedToSession_SessionNotFound() {
            // Arrange
            when(sessionDAO.findById(999)).thenReturn(Optional.empty());

            // Act
            boolean result = eventAdminService.isPresenterAssignedToSession(999, "presenter-1");

            // Assert
            assertFalse(result);
        }
    }

    // ==================== INTEGRATION SCENARIO TESTS ====================

    @Nested
    @DisplayName("Integration Scenario Tests")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("Should handle complete event lifecycle")
        void completeEventLifecycle() {
            // Create event
            doAnswer(invocation -> {
                Event event = invocation.getArgument(0);
                event.setEventId("1");
                return null;
            }).when(eventDAO).create(any(Event.class));
            
            Event createdEvent = eventAdminService.createEvent(testEvent);
            assertNotNull(createdEvent);
            assertEquals("1", createdEvent.getEventId());

            // Update event
            when(eventDAO.exists(1)).thenReturn(true);
            testEvent.setName("Updated Conference");
            eventAdminService.updateEvent(testEvent);
            verify(eventDAO, times(1)).update(testEvent);

            // Delete event
            when(sessionDAO.findByEventId(1)).thenReturn(Collections.emptyList());
            eventAdminService.deleteEvent(1);
            verify(eventDAO, times(1)).delete(1);
        }

        @Test
        @DisplayName("Should handle complete session lifecycle with presenter assignment")
        void completeSessionLifecycleWithPresenter() {
            // Create session
            doAnswer(invocation -> {
                Session session = invocation.getArgument(0);
                session.setSessionId("1");
                return null;
            }).when(sessionDAO).create(any(Session.class));
            
            Session createdSession = eventAdminService.createSession(testSession);
            assertNotNull(createdSession);

            // Assign presenter
            when(sessionDAO.exists(1)).thenReturn(true);
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));
            boolean assigned = eventAdminService.assignPresenterToSession(1, "presenter-1");
            assertTrue(assigned);

            // Verify presenter assignment
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));
            boolean isAssigned = eventAdminService.isPresenterAssignedToSession(1, "presenter-1");
            assertTrue(isAssigned);

            // Unassign presenter
            boolean unassigned = eventAdminService.unassignPresenterFromSession(1, "presenter-1");
            assertTrue(unassigned);

            // Delete session
            eventAdminService.deleteSession(1);
            verify(sessionDAO, times(1)).delete(1);
        }

        @Test
        @DisplayName("Should handle multiple presenter assignments")
        void multiplePresenterAssignments() {
            // Arrange
            when(sessionDAO.exists(1)).thenReturn(true);
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));

            // Assign multiple presenters
            assertTrue(eventAdminService.assignPresenterToSession(1, "presenter-1"));
            testSession.addPresenter("presenter-1");
            
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));
            assertTrue(eventAdminService.assignPresenterToSession(1, "presenter-2"));
            testSession.addPresenter("presenter-2");

            // Get all presenters
            when(sessionDAO.findById(1)).thenReturn(Optional.of(testSession));
            List<String> presenters = eventAdminService.getPresentersBySessionId(1);
            assertEquals(2, presenters.size());
        }
    }

    // ==================== TICKET MANAGEMENT TESTS ====================

    @Nested
    @DisplayName("Ticket Management Tests")
    class TicketManagementTests {

        @Test
        @DisplayName("Should generate ticket successfully")
        void generateTicket_Success() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);
            when(sessionDAO.exists(1)).thenReturn(true);

            // Act
            Ticket result = eventAdminService.generateTicket(100, 1, 1, TicketType.GENERAL, 50.0);

            // Assert
            assertNotNull(result);
            assertEquals(100, result.getAttendeeID());
            assertEquals(1, result.getEventID());
            assertEquals(1, result.getSessionID());
            assertEquals(TicketType.GENERAL, result.getType());
            assertEquals(50.0, result.getPrice());
            assertEquals(TicketStatus.ACTIVE, result.getStatus());
            verify(ticketDAO, times(1)).create(any(Ticket.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid attendee ID")
        void generateTicket_InvalidAttendeeId() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.generateTicket(0, 1, 1, TicketType.GENERAL, 50.0)
            );
            assertEquals("Attendee ID must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for invalid event ID")
        void generateTicket_InvalidEventId() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.generateTicket(100, 0, 1, TicketType.GENERAL, 50.0)
            );
            assertEquals("Event ID must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when event does not exist")
        void generateTicket_EventNotExists() {
            // Arrange
            when(eventDAO.exists(999)).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.generateTicket(100, 999, 1, TicketType.GENERAL, 50.0)
            );
            assertEquals("Event with ID 999 does not exist", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null ticket type")
        void generateTicket_NullTicketType() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);
            when(sessionDAO.exists(1)).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.generateTicket(100, 1, 1, null, 50.0)
            );
            assertEquals("Ticket type cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for negative price")
        void generateTicket_NegativePrice() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);
            when(sessionDAO.exists(1)).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.generateTicket(100, 1, 1, TicketType.GENERAL, -10.0)
            );
            assertEquals("Price cannot be negative", exception.getMessage());
        }

        @Test
        @DisplayName("Should update ticket status successfully")
        void updateTicketStatus_Success() {
            // Arrange
            when(ticketDAO.findById(1)).thenReturn(testTicket);

            // Act
            boolean result = eventAdminService.updateTicketStatus(1, TicketStatus.USED);

            // Assert
            assertTrue(result);
            verify(ticketDAO, times(1)).update(any(Ticket.class));
        }

        @Test
        @DisplayName("Should return false when ticket not found")
        void updateTicketStatus_TicketNotFound() {
            // Arrange
            when(ticketDAO.findById(999)).thenReturn(null);

            // Act
            boolean result = eventAdminService.updateTicketStatus(999, TicketStatus.USED);

            // Assert
            assertFalse(result);
            verify(ticketDAO, never()).update(any(Ticket.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid ticket ID")
        void updateTicketStatus_InvalidTicketId() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.updateTicketStatus(0, TicketStatus.USED)
            );
            assertEquals("Ticket ID must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null status")
        void updateTicketStatus_NullStatus() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.updateTicketStatus(1, null)
            );
            assertEquals("Ticket status cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should get ticket by ID successfully")
        void getTicketById_Success() {
            // Arrange
            when(ticketDAO.findById(1)).thenReturn(testTicket);

            // Act
            Ticket result = eventAdminService.getTicketById(1);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTicketID());
        }

        @Test
        @DisplayName("Should return null for invalid ticket ID")
        void getTicketById_InvalidId() {
            // Act
            Ticket result = eventAdminService.getTicketById(0);

            // Assert
            assertNull(result);
            verify(ticketDAO, never()).findById(anyInt());
        }

        @Test
        @DisplayName("Should get tickets by event ID successfully")
        void getTicketsByEventId_Success() {
            // Arrange
            List<Ticket> tickets = Arrays.asList(testTicket);
            when(ticketDAO.findAll()).thenReturn(new ArrayList<>(tickets));

            // Act
            List<Ticket> result = eventAdminService.getTicketsByEventId(1);

            // Assert
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return empty list for invalid event ID")
        void getTicketsByEventId_InvalidId() {
            // Act
            List<Ticket> result = eventAdminService.getTicketsByEventId(0);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should get tickets by session ID successfully")
        void getTicketsBySessionId_Success() {
            // Arrange
            List<Ticket> tickets = Arrays.asList(testTicket);
            when(ticketDAO.findTicketBySessionId(1)).thenReturn(new ArrayList<>(tickets));

            // Act
            List<Ticket> result = eventAdminService.getTicketsBySessionId(1);

            // Assert
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should delete ticket successfully")
        void deleteTicket_Success() {
            // Arrange
            when(ticketDAO.delete(1)).thenReturn(true);

            // Act
            boolean result = eventAdminService.deleteTicket(1);

            // Assert
            assertTrue(result);
            verify(ticketDAO, times(1)).delete(1);
        }

        @Test
        @DisplayName("Should return false for invalid ticket ID when deleting")
        void deleteTicket_InvalidId() {
            // Act
            boolean result = eventAdminService.deleteTicket(0);

            // Assert
            assertFalse(result);
            verify(ticketDAO, never()).delete(anyInt());
        }
    }

    // ==================== REPORT GENERATION TESTS ====================

    @Nested
    @DisplayName("Report Generation Tests")
    class ReportGenerationTests {

        @Test
        @DisplayName("Should generate event attendance report successfully")
        void generateEventAttendanceReport_Success() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);
            when(eventDAO.findById(1)).thenReturn(Optional.of(testEvent));
            when(sessionDAO.findByEventId(1)).thenReturn(Collections.singletonList(testSession));
            when(ticketDAO.findAll()).thenReturn(new ArrayList<>(Arrays.asList(testTicket)));

            // Act
            Map<String, Object> report = eventAdminService.generateEventAttendanceReport(1);

            // Assert
            assertNotNull(report);
            assertEquals(1, report.get("eventId"));
            assertEquals("Tech Conference 2025", report.get("eventName"));
            assertNotNull(report.get("totalAttendees"));
            assertNotNull(report.get("sessionAttendance"));
            assertNotNull(report.get("generatedAt"));
        }

        @Test
        @DisplayName("Should return error for invalid event ID in attendance report")
        void generateEventAttendanceReport_InvalidEventId() {
            // Act
            Map<String, Object> report = eventAdminService.generateEventAttendanceReport(0);

            // Assert
            assertEquals("Invalid event ID", report.get("error"));
        }

        @Test
        @DisplayName("Should generate session occupancy report successfully")
        void generateSessionOccupancyReport_Success() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);
            when(eventDAO.findById(1)).thenReturn(Optional.of(testEvent));
            when(sessionDAO.findByEventId(1)).thenReturn(Collections.singletonList(testSession));
            when(ticketDAO.findAll()).thenReturn(new ArrayList<>(Arrays.asList(testTicket)));

            // Act
            Map<String, Object> report = eventAdminService.generateSessionOccupancyReport(1);

            // Assert
            assertNotNull(report);
            assertEquals(1, report.get("eventId"));
            assertEquals("Tech Conference 2025", report.get("eventName"));
            assertNotNull(report.get("totalCapacity"));
            assertNotNull(report.get("totalRegistered"));
            assertNotNull(report.get("overallOccupancyRate"));
            assertNotNull(report.get("sessionOccupancy"));
        }

        @Test
        @DisplayName("Should return error for invalid event ID in occupancy report")
        void generateSessionOccupancyReport_InvalidEventId() {
            // Act
            Map<String, Object> report = eventAdminService.generateSessionOccupancyReport(-1);

            // Assert
            assertEquals("Invalid event ID", report.get("error"));
        }

        @Test
        @DisplayName("Should generate ticket usage report successfully")
        void generateTicketUsageReport_Success() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);
            when(eventDAO.findById(1)).thenReturn(Optional.of(testEvent));
            when(ticketDAO.findAll()).thenReturn(new ArrayList<>(Arrays.asList(testTicket)));

            // Act
            Map<String, Object> report = eventAdminService.generateTicketUsageReport(1);

            // Assert
            assertNotNull(report);
            assertEquals(1, report.get("eventId"));
            assertEquals("Tech Conference 2025", report.get("eventName"));
            assertNotNull(report.get("totalTickets"));
            assertNotNull(report.get("ticketsByType"));
            assertNotNull(report.get("ticketsByStatus"));
            assertNotNull(report.get("totalRevenue"));
        }

        @Test
        @DisplayName("Should return error for invalid event ID in ticket usage report")
        void generateTicketUsageReport_InvalidEventId() {
            // Act
            Map<String, Object> report = eventAdminService.generateTicketUsageReport(0);

            // Assert
            assertEquals("Invalid event ID", report.get("error"));
        }

        @Test
        @DisplayName("Should throw exception for invalid event ID when exporting report")
        void exportEventReport_InvalidEventId() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.exportEventReport(0, "ATTENDANCE")
            );
            assertEquals("Invalid event ID", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null report type when exporting")
        void exportEventReport_NullReportType() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventAdminService.exportEventReport(1, null)
            );
            assertEquals("Report type cannot be null or empty", exception.getMessage());
        }
    }

    // ==================== VISITOR VIEW TESTS ====================

    @Nested
    @DisplayName("Visitor View Tests")
    class VisitorViewTests {

        @Test
        @DisplayName("Should get available events for visitors")
        void getAvailableEventsForVisitors_Success() {
            // Arrange
            Event futureEvent = new Event(
                "2",
                "Future Workshop",
                EventType.WORKSHOP,
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(11),
                "Training Room",
                1,
                EventStatus.SCHEDULED
            );
            when(eventDAO.findAll()).thenReturn(Arrays.asList(testEvent, futureEvent));

            // Act
            List<Event> result = eventAdminService.getAvailableEventsForVisitors();

            // Assert
            assertNotNull(result);
            assertTrue(result.size() >= 1);
        }

        @Test
        @DisplayName("Should return empty list when no events available")
        void getAvailableEventsForVisitors_NoEvents() {
            // Arrange
            when(eventDAO.findAll()).thenReturn(null);

            // Act
            List<Event> result = eventAdminService.getAvailableEventsForVisitors();

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should filter events by type")
        void filterEventsByType_Success() {
            // Arrange
            when(eventDAO.findAll()).thenReturn(Collections.singletonList(testEvent));

            // Act
            List<Event> result = eventAdminService.filterEventsByType("CONFERENCE");

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return all available events for null type filter")
        void filterEventsByType_NullType() {
            // Arrange
            when(eventDAO.findAll()).thenReturn(Collections.singletonList(testEvent));

            // Act
            List<Event> result = eventAdminService.filterEventsByType(null);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should filter events by location")
        void filterEventsByLocation_Success() {
            // Arrange
            when(eventDAO.findAll()).thenReturn(Collections.singletonList(testEvent));

            // Act
            List<Event> result = eventAdminService.filterEventsByLocation("Convention");

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return all available events for null location filter")
        void filterEventsByLocation_NullLocation() {
            // Arrange
            when(eventDAO.findAll()).thenReturn(Collections.singletonList(testEvent));

            // Act
            List<Event> result = eventAdminService.filterEventsByLocation(null);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should check available capacity")
        void hasAvailableCapacity_True() {
            // Arrange
            when(eventDAO.exists(1)).thenReturn(true);
            when(sessionDAO.findByEventId(1)).thenReturn(Collections.singletonList(testSession));
            when(ticketDAO.findAll()).thenReturn(new ArrayList<>(Arrays.asList(testTicket)));

            // Act
            boolean result = eventAdminService.hasAvailableCapacity(1);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false for invalid event ID when checking capacity")
        void hasAvailableCapacity_InvalidEventId() {
            // Act
            boolean result = eventAdminService.hasAvailableCapacity(0);

            // Assert
            assertFalse(result);
        }
    }
}
