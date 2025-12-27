package group_3.service.PresenterService;
/**
 * @author Group 3
 */
import group_3.model.Presenter;
import group_3.model.Session;
import group_3.model.Ticket;
import group_3.model.enums.TicketStatus;
import group_3.dao.PersonDAO;
import group_3.dao.SessionDAO;
import group_3.dao.TicketDAO;
import group_3.dao.impl.PersonDAOImpl;
import group_3.dao.impl.SessionDAOImpl;
import group_3.dao.impl.TicketDAOImpl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of PresenterService for managing Presenters and calculating statistics.
 * Provides CRUD operations and statistics calculation based on sessions and attendance.
 * 
 * @author Group21
 */
public class PresenterServiceImpl implements PresenterService {
    
    private PersonDAO personDAO;
    private SessionDAO sessionDAO;
    private TicketDAO ticketDAO;
    
    public PresenterServiceImpl() {
        this.personDAO = new PersonDAOImpl();
        this.sessionDAO = new SessionDAOImpl();
        this.ticketDAO = new TicketDAOImpl();
    }
    
    public PresenterServiceImpl(PersonDAO personDAO, SessionDAO sessionDAO, TicketDAO ticketDAO) {
        this.personDAO = personDAO;
        this.sessionDAO = sessionDAO;
        this.ticketDAO = ticketDAO;
    }
    
    @Override
    public Presenter createPresenter(Presenter presenter) {
        if (presenter == null) {
            throw new IllegalArgumentException("Presenter cannot be null");
        }
        personDAO.create(presenter);
        return presenter;
    }
    
    @Override
    public Optional<Presenter> getPresenterById(int presenterId) {
        Optional<group_3.model.Person> person = personDAO.findById(presenterId);
        if (person.isPresent() && person.get() instanceof Presenter) {
            return Optional.of((Presenter) person.get());
        }
        return Optional.empty();
    }
    
    @Override
    public Optional<Presenter> getPresenterByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        Optional<group_3.model.Person> person = personDAO.findByUsername(username);
        if (person.isPresent() && person.get() instanceof Presenter) {
            return Optional.of((Presenter) person.get());
        }
        return Optional.empty();
    }
    
    @Override
    public List<Presenter> getAllPresenters() {
        return personDAO.findAll().stream()
            .filter(person -> person instanceof Presenter)
            .map(person -> (Presenter) person)
            .collect(Collectors.toList());
    }
    
    @Override
    public void updatePersonalInfo(int presenterId, String fullName, String contactInformation) {
        Optional<Presenter> presenter = getPresenterById(presenterId);
        if (!presenter.isPresent()) {
            throw new IllegalArgumentException("Presenter with ID " + presenterId + " not found");
        }
        
        Presenter p = presenter.get();
        if (fullName != null && !fullName.trim().isEmpty()) {
            p.setFullName(fullName);
        }
        if (contactInformation != null) {
            p.setContactInformation(contactInformation);
        }
        personDAO.update(p);
    }
    
    @Override
    public void updatePresenterRole(int presenterId, String presenterRole) {
        Optional<Presenter> presenter = getPresenterById(presenterId);
        if (!presenter.isPresent()) {
            throw new IllegalArgumentException("Presenter with ID " + presenterId + " not found");
        }
        
        Presenter p = presenter.get();
        p.setPresenterRole(presenterRole);
        personDAO.update(p);
    }
    
    @Override
    public void updatePresenter(Presenter presenter) {
        if (presenter == null) {
            throw new IllegalArgumentException("Presenter cannot be null");
        }
        personDAO.update(presenter);
    }
    
    @Override
    public void deletePresenter(int presenterId) {
        personDAO.delete(presenterId);
    }
    
    @Override
    public int getSessionsPresented(int presenterId) {
        List<Session> allSessions = sessionDAO.findAll();
        return (int) allSessions.stream()
            .filter(session -> session.getPresenterIds() != null && 
                    session.getPresenterIds().contains(String.valueOf(presenterId)))
            .count();
    }
    
    @Override
    public int getTotalAttendeesPresented(int presenterId) {
        List<Integer> sessions = getSessionsList(presenterId);
        int totalAttendees = 0;
        
        for (Integer sessionId : sessions) {
            ArrayList<Ticket> tickets = ticketDAO.findTicketBySessionId(sessionId);
            // Count tickets that are USED or ACTIVE (attended)
            totalAttendees += (int) tickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.USED || 
                                 ticket.getStatus() == TicketStatus.ACTIVE)
                .count();
        }
        
        return totalAttendees;
    }
    
    @Override
    public double getAverageAttendance(int presenterId) {
        int sessionsPresented = getSessionsPresented(presenterId);
        if (sessionsPresented == 0) {
            return 0.0;
        }
        
        int totalAttendees = getTotalAttendeesPresented(presenterId);
        return (double) totalAttendees / sessionsPresented;
    }
    
    @Override
    public Map<String, Object> getPresenterStatistics(int presenterId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        int sessionsPresented = getSessionsPresented(presenterId);
        int totalAttendees = getTotalAttendeesPresented(presenterId);
        double averageAttendance = getAverageAttendance(presenterId);
        
        stats.put("presenter_id", presenterId);
        stats.put("sessions_presented", sessionsPresented);
        stats.put("total_attendees", totalAttendees);
        stats.put("average_attendance", Math.round(averageAttendance * 100.0) / 100.0); // Round to 2 decimals
        stats.put("last_updated", new Date().toString());
        
        return stats;
    }
    
    @Override
    public List<Integer> getSessionsList(int presenterId) {
        List<Session> allSessions = sessionDAO.findAll();
        return allSessions.stream()
            .filter(session -> session.getPresenterIds() != null && 
                    session.getPresenterIds().contains(String.valueOf(presenterId)))
            .map(session -> Integer.parseInt(session.getSessionId()))
            .collect(Collectors.toList());
    }
    
    @Override
    public void calculateAndUpdateStatistics(int presenterId) {
        Optional<Presenter> presenter = getPresenterById(presenterId);
        if (!presenter.isPresent()) {
            throw new IllegalArgumentException("Presenter with ID " + presenterId + " not found");
        }
        
        Map<String, Object> stats = getPresenterStatistics(presenterId);
        
        // Convert map to JSON string manually
        StringBuilder jsonBuilder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            if (!first) jsonBuilder.append(",");
            jsonBuilder.append("\"").append(entry.getKey()).append("\":\"")
                .append(entry.getValue()).append("\"");
            first = false;
        }
        jsonBuilder.append("}");
        
        Presenter p = presenter.get();
        p.setStatistics(jsonBuilder.toString());
        personDAO.update(p);
    }
    
    @Override
    public List<Presenter> getTopPresentersBySessionCount(int limit) {
        List<Presenter> allPresenters = getAllPresenters();
        
        return allPresenters.stream()
            .sorted((p1, p2) -> Integer.compare(
                getSessionsPresented(p2.getId()),
                getSessionsPresented(p1.getId())
            ))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Presenter> getTopPresentersByAttendance(int limit) {
        List<Presenter> allPresenters = getAllPresenters();
        
        return allPresenters.stream()
            .sorted((p1, p2) -> Integer.compare(
                getTotalAttendeesPresented(p2.getId()),
                getTotalAttendeesPresented(p1.getId())
            ))
            .limit(limit)
            .collect(Collectors.toList());
    }
}
