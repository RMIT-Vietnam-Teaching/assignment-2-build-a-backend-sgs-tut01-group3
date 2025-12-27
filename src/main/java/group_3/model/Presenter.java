package group_3.model;

import java.time.LocalDate;

/**
 * @author Group 3
 */
public class Presenter extends Person {

    private String presenterRole;
    private String statistics; // JSONB

    public Presenter(int id, String username, String passwordHash,
                     String fullName, LocalDate dob,
                     String contactInfo, String presenterRole,
                     String statistics) {

        super(id, username, passwordHash, fullName, dob, contactInfo, group_3.model.enums.Role.PRESENTER);
        this.presenterRole = presenterRole;
        this.statistics = statistics;
    }

    public String getPresenterRole() {
        return presenterRole;
    }

    public String getStatistics() {
        return statistics;
    }

    public void setPresenterRole(String presenterRole) {
        this.presenterRole = presenterRole;
    }

    public void setStatistics(String statistics) {
        this.statistics = statistics;
    }
}
