package group_3.service.EventStatisticsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import group_3.dao.EventDAO;
import group_3.dao.SessionDAO;
import group_3.dao.TicketDAO;
import group_3.model.Event;
import group_3.model.EventStatistics;
import group_3.model.Session;
import group_3.model.SessionStatistics;
import group_3.model.Ticket;
import group_3.model.enums.TicketStatus;

/**
 * @author Group 3
 *
 * Implementation of EventStatisticsService.
 * Provides business logic for calculating event statistics, revenue, and attendance metrics.
 */
public class EventStatisticsServiceImpl implements EventStatisticsService {
    
    private final EventDAO eventDAO;
    private final SessionDAO sessionDAO;
    private final TicketDAO ticketDAO;
    
    /**
     * Constructor with dependency injection.
     * 
     * @param eventDAO the event data access object
     * @param sessionDAO the session data access object
     * @param ticketDAO the ticket data access object
     */
    public EventStatisticsServiceImpl(EventDAO eventDAO, SessionDAO sessionDAO, TicketDAO ticketDAO) {
        this.eventDAO = eventDAO;
        this.sessionDAO = sessionDAO;
        this.ticketDAO = ticketDAO;
    }
    
    @Override
    public Optional<EventStatistics> getEventStatistics(int eventId) {
        Optional<Event> eventOpt = eventDAO.findById(eventId);
        if (eventOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Event event = eventOpt.get();
        
        // Optimized: Fetch tickets for this event once, then calculate all stats in one pass
        ArrayList<Ticket> eventTickets = ticketDAO.findByEventId(eventId);
        if (eventTickets == null) {
            eventTickets = new ArrayList<>();
        }
        
        // Calculate all stats in a single pass through the tickets
        double revenue = 0.0;
        int ticketsSold = 0;
        int checkedIn = 0;
        
        for (Ticket ticket : eventTickets) {
            TicketStatus status = ticket.getStatus();
            if (status == TicketStatus.ACTIVE || status == TicketStatus.USED) {
                ticketsSold++;
                revenue += ticket.getPrice();
                if (status == TicketStatus.USED) {
                    checkedIn++;
                }
            }
        }
        
        EventStatistics stats = new EventStatistics(
            eventId,
            event.getName(),
            revenue,
            ticketsSold,
            checkedIn
        );
        
        return Optional.of(stats);
    }
    
    @Override
    public double calculateEventRevenue(int eventId) {
        ArrayList<Ticket> tickets = ticketDAO.findByEventId(eventId);
        if (tickets == null) {
            return 0.0;
        }
        
        return tickets.stream()
            .filter(ticket -> ticket.getStatus() == TicketStatus.ACTIVE || 
                            ticket.getStatus() == TicketStatus.USED)
            .mapToDouble(Ticket::getPrice)
            .sum();
    }
    
    @Override
    public double calculateEventAttendanceRate(int eventId) {
        int totalSold = getTotalTicketsSold(eventId);
        if (totalSold == 0) {
            return 0.0;
        }
        
        int checkedIn = getTotalCheckedIn(eventId);
        return (checkedIn * 100.0) / totalSold;
    }
    
    @Override
    public Optional<SessionStatistics> getSessionStatistics(int sessionId) {
        Optional<Session> sessionOpt = sessionDAO.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Session session = sessionOpt.get();
        ArrayList<Ticket> tickets = ticketDAO.findTicketBySessionId(sessionId);
        
        if (tickets == null) {
            tickets = new ArrayList<>();
        }
        
        int ticketsSold = (int) tickets.stream()
            .filter(ticket -> ticket.getStatus() == TicketStatus.ACTIVE || 
                            ticket.getStatus() == TicketStatus.USED)
            .count();
        
        int checkedIn = (int) tickets.stream()
            .filter(ticket -> ticket.getStatus() == TicketStatus.USED)
            .count();
        
        int eventId = session.getEventId();
        
        SessionStatistics stats = new SessionStatistics(
            sessionId,
            session.getTitle(),
            eventId,
            ticketsSold,
            checkedIn,
            session.getCapacity()
        );
        
        return Optional.of(stats);
    }
    
    @Override
    public List<SessionStatistics> getEventSessionStatistics(int eventId) {
        List<Session> sessions = sessionDAO.findByEventId(eventId);
        List<SessionStatistics> statsList = new ArrayList<>();
        
        for (Session session : sessions) {
            int sessionId = session.getSessionId();
            Optional<SessionStatistics> stats = getSessionStatistics(sessionId);
            stats.ifPresent(statsList::add);
        }
        
        return statsList;
    }
    
    @Override
    public List<SessionStatistics> getMostPopularSessions(int eventId, int limit) {
        List<Session> sessions = sessionDAO.findByEventId(eventId);
        if (sessions == null || sessions.isEmpty()) {
            return new ArrayList<>();
        }

        List<SessionStatistics> statsList = new ArrayList<>();
        for (Session session : sessions) {
            ArrayList<Ticket> tickets = ticketDAO.findTicketBySessionId(session.getSessionId());
            if (tickets == null) {
            tickets = new ArrayList<>();
            }

            int sold = (int) tickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.ACTIVE || ticket.getStatus() == TicketStatus.USED)
                .count();
            int checkedIn = (int) tickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.USED)
                .count();

            SessionStatistics stats = new SessionStatistics(
                session.getSessionId(),
                session.getTitle(),
                session.getEventId(),
                sold,
                checkedIn,
                session.getCapacity()
            );
            statsList.add(stats);
        }

        return statsList.stream()
            .sorted((s1, s2) -> Integer.compare(s2.getTotalTicketsSold(), s1.getTotalTicketsSold()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<SessionStatistics> getMostPopularSessionsGlobal(int limit) {
        List<Session> allSessions = sessionDAO.findAll();
        List<SessionStatistics> statsList = new ArrayList<>();
        
        for (Session session : allSessions) {
            int sessionId = session.getSessionId();
            Optional<SessionStatistics> stats = getSessionStatistics(sessionId);
            stats.ifPresent(statsList::add);
        }
        
        return statsList.stream()
            .sorted((s1, s2) -> Integer.compare(s2.getTotalTicketsSold(), s1.getTotalTicketsSold()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    @Override
    public int getTotalTicketsSold(int eventId) {
        ArrayList<Ticket> tickets = ticketDAO.findByEventId(eventId);
        if (tickets == null) {
            return 0;
        }
        
        return (int) tickets.stream()
            .filter(ticket -> ticket.getStatus() == TicketStatus.ACTIVE || 
                            ticket.getStatus() == TicketStatus.USED)
            .count();
    }
    
    @Override
    public int getTotalCheckedIn(int eventId) {
        ArrayList<Ticket> tickets = ticketDAO.findByEventId(eventId);
        if (tickets == null) {
            return 0;
        }
        
        return (int) tickets.stream()
            .filter(ticket -> ticket.getStatus() == TicketStatus.USED)
            .count();
    }

    @Override
    public List<EventStatistics> getAllEventStatistics() {
        List<Event> allEvents = eventDAO.findAll();
        List<EventStatistics> statsList = new ArrayList<>();
        
        for (Event event : allEvents) {
            int eventId = event.getEventId();
            Optional<EventStatistics> stats = getEventStatistics(eventId);
            stats.ifPresent(statsList::add);
        }
        
        return statsList;
    }
}
