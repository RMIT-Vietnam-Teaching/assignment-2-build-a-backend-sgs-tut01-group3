package group_3.model;

import java.time.LocalDate;

/**
 * @author Group 3
 */
public class Attendee extends Person {

    private String history; // JSONB

    public Attendee(int id, String username, String passwordHash,
                    String fullName, LocalDate dob,
                    String contactInfo, String history) {

        super(id, username, passwordHash, fullName, dob, contactInfo, group_3.model.enums.Role.ATTENDEE);
        this.history = history;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(String history) {
        this.history = history;
    }
}
