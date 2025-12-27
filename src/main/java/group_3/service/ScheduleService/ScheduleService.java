package group_3.service.ScheduleService;

import java.time.*;
import group_3.model.Schedule_entry;
import java.util.*;
/**
 * @author Group 3
 */
public interface ScheduleService {
    boolean hasConflict(int personId, LocalDateTime startTime, LocalDateTime endTime);

    void addScheduleEntry(Schedule_entry entry);

    void removeScheduleEntry(int personId, int sessionId);

    List<Schedule_entry> getScheduleForPerson(int personId);
}
