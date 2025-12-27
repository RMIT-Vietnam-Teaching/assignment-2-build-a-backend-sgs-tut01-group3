package group_3.service.PresenterService;
/**
 * @author Group 3
 */
import group_3.model.Presenter;
import java.util.List;
import java.util.Optional;
import java.util.Map;

/**
 * PresenterService provides operations for managing Presenters and calculating their statistics.
 * Handles both CRUD operations and statistics calculation (sessions presented, ratings, performance metrics).
 * 
 * @author Group21
 */
public interface PresenterService {
    
    // Create operations
    /**
     * Create a new Presenter
     * @param presenter the Presenter to create
     * @return the created Presenter with assigned ID
     */
    Presenter createPresenter(Presenter presenter);
    
    // Read operations
    /**
     * Get a Presenter by ID
     * @param presenterId the Presenter ID
     * @return Optional containing the Presenter if found
     */
    Optional<Presenter> getPresenterById(int presenterId);
    
    /**
     * Get a Presenter by username
     * @param username the username
     * @return Optional containing the Presenter if found
     */
    Optional<Presenter> getPresenterByUsername(String username);
    
    /**
     * Get all Presenters
     * @return List of all Presenters
     */
    List<Presenter> getAllPresenters();
    
    // Update operations
    /**
     * Update a Presenter's personal information
     * @param presenterId the Presenter ID
     * @param fullName new full name
     * @param contactInformation new contact information (JSON)
     */
    void updatePersonalInfo(int presenterId, String fullName, String contactInformation);
    
    /**
     * Update a Presenter's role
     * @param presenterId the Presenter ID
     * @param presenterRole new presenter role
     */
    void updatePresenterRole(int presenterId, String presenterRole);
    
    /**
     * Update a Presenter's complete information
     * @param presenter the Presenter with updated information
     */
    void updatePresenter(Presenter presenter);
    
    // Delete operations
    /**
     * Delete a Presenter by ID
     * @param presenterId the Presenter ID to delete
     */
    void deletePresenter(int presenterId);
    
    // Statistics calculations
    /**
     * Get the number of sessions presented by a Presenter
     * @param presenterId the Presenter ID
     * @return number of sessions presented
     */
    int getSessionsPresented(int presenterId);
    
    /**
     * Get the total attendees across all sessions presented
     * @param presenterId the Presenter ID
     * @return total attendees
     */
    int getTotalAttendeesPresented(int presenterId);
    
    /**
     * Get the average attendance per session
     * @param presenterId the Presenter ID
     * @return average attendance
     */
    double getAverageAttendance(int presenterId);
    
    /**
     * Get detailed statistics for a Presenter
     * @param presenterId the Presenter ID
     * @return Map containing statistics (sessions_presented, total_attendees, average_attendance, etc.)
     */
    Map<String, Object> getPresenterStatistics(int presenterId);
    
    /**
     * Get all sessions presented by a Presenter
     * @param presenterId the Presenter ID
     * @return List of session IDs presented
     */
    List<Integer> getSessionsList(int presenterId);
    
    /**
     * Calculate and update statistics for a Presenter
     * @param presenterId the Presenter ID
     */
    void calculateAndUpdateStatistics(int presenterId);
    
    /**
     * Get top Presenters by number of sessions
     * @param limit the number of top presenters to return
     * @return List of Presenters sorted by sessions presented (descending)
     */
    List<Presenter> getTopPresentersBySessionCount(int limit);
    
    /**
     * Get top Presenters by attendance
     * @param limit the number of top presenters to return
     * @return List of Presenters sorted by total attendance (descending)
     */
    List<Presenter> getTopPresentersByAttendance(int limit);
}
