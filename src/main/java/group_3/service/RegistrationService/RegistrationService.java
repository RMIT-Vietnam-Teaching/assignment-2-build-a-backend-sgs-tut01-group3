package group_3.service.RegistrationService;
/**
 * @author Group 3
 */
import group_3.model.Ticket;
import group_3.model.enums.TicketType;
import java.util.*;

public interface RegistrationService {
    boolean checkPersonConflict(int attendeeId, int sessionId);

    boolean registerAttendee(int AttendeeId, int newSessionId, TicketType ticketType, double ticketPrice);

    boolean cancelRegistration(int ticketId);

    List<Ticket> getTicketsForAttendee( int attendeeId );
}
