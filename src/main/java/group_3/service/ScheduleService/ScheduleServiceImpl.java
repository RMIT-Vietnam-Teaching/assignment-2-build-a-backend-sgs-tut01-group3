package group_3.service.ScheduleService;
/**
 * @author Group 3
 */
import group_3.dao.ScheduleDAO;
import group_3.dao.SessionDAO;
import group_3.dao.impl.ScheduleDAOImpl;
import group_3.dao.impl.SessionDAOImpl;
import group_3.model.Schedule_entry;
import group_3.model.Session;

import java.time.*;
import java.util.*;

public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleDAO scheduleDAO = new ScheduleDAOImpl();
    private final SessionDAO sessionDAO = new SessionDAOImpl();

    @Override
    public boolean hasConflict(int personId, LocalDateTime startTime, LocalDateTime endTime){
        List<Schedule_entry> existingSchedules = scheduleDAO.findAllScheduleByUserId(personId);

        for(Schedule_entry entry: existingSchedules){
            boolean overlap = startTime.isBefore(entry.getEndTime()) && endTime.isAfter(entry.getStartTime());

            if(overlap){
                return true;
            }
        }
        return false;
    }

    public boolean hasConflict(int personId, int sessionId){
        Optional<Session> optionalSession = sessionDAO.findById(sessionId);

        if(optionalSession.isEmpty()){
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        Session session = optionalSession.get();

        return hasConflict(
                personId,
                session.getStartTime(),
                session.getEndTime()
        );
    }

    @Override
    public void addScheduleEntry(Schedule_entry entry){
        scheduleDAO.create(entry);
    }

    @Override
    public void removeScheduleEntry(int personId, int sessionId){
        scheduleDAO.deleteByUserAndSession(personId,sessionId);
    }

    @Override
    public List<Schedule_entry> getScheduleForPerson(int personId){
        return scheduleDAO.findAllScheduleByUserId(personId);
    }
}
