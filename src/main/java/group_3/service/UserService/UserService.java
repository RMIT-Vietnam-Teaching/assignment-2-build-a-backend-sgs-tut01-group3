package group_3.service.UserService;

import group_3.model.Person;
import group_3.model.Attendee;
import group_3.model.Presenter;
import java.util.List;
import java.util.Optional;

/**
 * UserService provides CRUD operations for all Person types.
 * This is primarily used by System Admin for user management.
/**
 * @author Group 3
 */
public interface UserService {
    
    // Create operations
    /**
     * Create a new Attendee user
     * @param attendee the Attendee to create
     * @return the created Attendee with assigned ID
     */
    Attendee createAttendee(Attendee attendee);
    
    /**
     * Create a new Presenter user
     * @param presenter the Presenter to create
     * @return the created Presenter with assigned ID
     */
    Presenter createPresenter(Presenter presenter);
    
    /**
     * Create a new Person (generic)
     * @param person the Person to create
     * @return the created Person with assigned ID
     */
    Person createPerson(Person person);
    
    // Read operations
    /**
     * Find a user by ID
     * @param userId the user ID
     * @return Optional containing the Person if found
     */
    Optional<Person> getUserById(int userId);
    
    /**
     * Find a user by username
     * @param username the username to search
     * @return Optional containing the Person if found
     */
    Optional<Person> getUserByUsername(String username);
    
    /**
     * Get all users
     * @return List of all Person objects
     */
    List<Person> getAllUsers();
    
    /**
     * Get all Attendees
     * @return List of all Attendee objects
     */
    List<Attendee> getAllAttendees();
    
    /**
     * Get all Presenters
     * @return List of all Presenter objects
     */
    List<Presenter> getAllPresenters();
    
    /**
     * Find an Attendee by ID
     * @param attendeeId the Attendee ID
     * @return Optional containing the Attendee if found
     */
    Optional<Attendee> getAttendeeById(int attendeeId);
    
    /**
     * Find a Presenter by ID
     * @param presenterId the Presenter ID
     * @return Optional containing the Presenter if found
     */
    Optional<Presenter> getPresenterById(int presenterId);
    
    // Update operations
    /**
     * Update a user's information
     * @param person the Person with updated information
     */
    void updateUser(Person person);
    
    /**
     * Update an Attendee's information
     * @param attendee the Attendee with updated information
     */
    void updateAttendee(Attendee attendee);
    
    /**
     * Update a Presenter's information
     * @param presenter the Presenter with updated information
     */
    void updatePresenter(Presenter presenter);
    
    // Delete operations
    /**
     * Delete a user by ID
     * @param userId the user ID to delete
     */
    void deleteUser(int userId);
    
    /**
     * Delete an Attendee by ID
     * @param attendeeId the Attendee ID to delete
     */
    void deleteAttendee(int attendeeId);
    
    /**
     * Delete a Presenter by ID
     * @param presenterId the Presenter ID to delete
     */
    void deletePresenter(int presenterId);
    
    // Utility operations
    /**
     * Check if a username already exists
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    boolean userExists(String username);
    
    /**
     * Count total number of users
     * @return total user count
     */
    int getTotalUserCount();
    
    /**
     * Count total number of Attendees
     * @return total Attendee count
     */
    int getTotalAttendeeCount();
    
    /**
     * Count total number of Presenters
     * @return total Presenter count
     */
    int getTotalPresenterCount();
}
