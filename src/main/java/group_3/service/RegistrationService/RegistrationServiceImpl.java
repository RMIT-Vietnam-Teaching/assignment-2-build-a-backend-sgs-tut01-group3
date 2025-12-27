package group_3.service.RegistrationService;
/**
 * @author Group 3
 */
import group_3.dao.*;
import group_3.dao.impl.*;
import group_3.model.*;
import group_3.model.enums.TicketStatus;
import group_3.model.enums.TicketType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegistrationServiceImpl implements RegistrationService{
    private final TicketDAO ticketDAO =  new TicketDAOImpl();
    private final ScheduleDAO scheduleDAO = new ScheduleDAOImpl();
    private final SessionDAO sessionDAO = new SessionDAOImpl();

    @Override
    public boolean checkPersonConflict (int PersonId, int newSessionId) {
        ArrayList<Schedule_entry> PersonSchedule = scheduleDAO.findAllScheduleByUserId(PersonId);
        Optional sessionOptional = sessionDAO.findById(newSessionId);
        Session s = (Session) sessionOptional.get();

        for (int i = 0; i < PersonSchedule.size(); i++) {
            Schedule_entry currentEntry = PersonSchedule.get(i);
            if (s.getStartTime().isAfter(currentEntry.getStartTime()) && s.getStartTime().isBefore(currentEntry.getEndTime())) { //checking session start in the middle of assigned session
                return true; //there is conflict
            } if (s.getEndTime().isAfter(currentEntry.getStartTime()) && s.getEndTime().isBefore(currentEntry.getEndTime())) { //checking session end in the middle of assigned session
                return true; //there is conflict
            } if (s.getStartTime().isBefore(currentEntry.getStartTime()) && s.getEndTime().isAfter(currentEntry.getEndTime())) { //checking session overlap the assigned
                return true; //there is conflict
            }
        } return false;
    }

    public boolean registerAttendee(int AttendeeId, int newSessionId, TicketType ticketType, double ticketPrice) {
        if (checkPersonConflict (AttendeeId, newSessionId)) {return false;}
        Optional sessionOptional = sessionDAO.findById(newSessionId);
        Session s = (Session) sessionOptional.get();

        Ticket ticket = new Ticket();
        ticket.setSessionID(newSessionId);
        ticket.setAttendeeID(AttendeeId);
        ticket.setEventID(Integer.parseInt(s.getEventId()));
        ticket.setPrice(ticketPrice);
        ticket.setType(ticketType);
        String qrPath = generateTicketCode(AttendeeId, newSessionId);
        ticket.setQRpath(qrPath);
        ticketDAO.create(ticket);

        Schedule_entry entry = new Schedule_entry();
        entry.setSessionID(newSessionId);
        entry.setPersonID(AttendeeId);
        entry.setStartTime(s.getStartTime());
        entry.setEndTime(s.getEndTime());

        scheduleDAO.create(entry);
        return true;
    }

    @Override
    public boolean cancelRegistration(int ticketId) {
        Ticket ticket = ticketDAO.findById(ticketId);
        if (ticket == null) {
            return false;
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticketDAO.update(ticket);

        scheduleDAO.deleteByUserAndSession(ticket.getAttendeeID(), ticket.getSessionID());
        return true;
    }

    @Override
    public List<Ticket> getTicketsForAttendee(int attendeeId) {
        return ticketDAO.findTicketByAttendeeId(attendeeId);
    }

    public static String generateTicketCode(int attendeeId, int sessionId) {
        return "TKT-" + attendeeId + "-" + sessionId + "-" + System.currentTimeMillis();
    }
}
