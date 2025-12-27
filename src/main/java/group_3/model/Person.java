package group_3.model;

import java.time.LocalDate;
import group_3.model.enums.Role;

/**
 * @author Group 3
 */
public abstract class Person {

    protected int id;
    protected String username;
    protected String passwordHash;
    protected String fullName;
    protected LocalDate dateOfBirth;
    protected String contactInformation; // JSON string
    protected Role role;

    protected Person() {}

    protected Person(int id, String username, String passwordHash,
                     String fullName, LocalDate dateOfBirth,
                     String contactInformation, Role role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.contactInformation = contactInformation;
        this.role = role;
    }

    // Getters & setters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getContactInformation() { return contactInformation; }
    public Role getRole() { return role; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setContactInformation(String contactInformation) { this.contactInformation = contactInformation; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
