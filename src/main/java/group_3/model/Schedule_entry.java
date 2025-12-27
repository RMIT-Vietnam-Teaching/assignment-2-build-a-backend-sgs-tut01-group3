package group_3.model;
/**
 * @author Group 3
 */
import java.time.LocalDateTime;

public class Schedule_entry {
    private int id;
    private int personID;
    private int sessionID;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Schedule_entry() {
        this.sessionID = 0;
        this.personID = 0;
        this.startTime = null;
        this.endTime = null;
    }

    public Schedule_entry(int id, int personID, int sessionID, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.personID = personID;
        this.sessionID = sessionID;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPersonID() {
        return personID;
    }

    public void setPersonID(int personID) {
        this.personID = personID;
    }

    public int getSessionID() {
        return sessionID;
    }

    public void setSessionID(int sessionID) {
        this.sessionID = sessionID;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }


}
