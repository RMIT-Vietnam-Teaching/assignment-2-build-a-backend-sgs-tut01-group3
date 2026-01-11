package group_3.dao;

import java.util.ArrayList;

import group_3.model.Ticket;

/**
 * @author Group 3
 *
 * DAO interface defining operations for accessing and managing Ticket data.
 */


public interface TicketDAO {

    int create(Ticket ticket);

    void update(Ticket ticket);

    boolean delete(int id);

    ArrayList<Ticket> findAll();

    Ticket findById(int id);

    ArrayList<Ticket> findTicketByAttendeeId(int id);

    ArrayList<Ticket> findTicketBySessionId(int id);
    
    /**
     * Find all tickets for a specific event.
     * More efficient than findAll() when you only need tickets for one event.
     * 
     * @param eventId the event ID
     * @return list of tickets for the event
     */
    ArrayList<Ticket> findByEventId(int eventId);
}
