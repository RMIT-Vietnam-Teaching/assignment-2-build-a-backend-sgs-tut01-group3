package group_3.dao;

import group_3.model.*;

import java.util.ArrayList;

public interface ScheduleDAO {
    void create (ScheduleEntry scheduleEntry);
    void update(ScheduleEntry scheduleEntry);
    boolean delete(int id);
    boolean deleteByUserAndSession (int userID, int sessionID);
    ScheduleEntry findById(int id);
    ArrayList<ScheduleEntry> findAllSchedule();
    ArrayList<ScheduleEntry> findAllScheduleByUserId(int userId);
}
