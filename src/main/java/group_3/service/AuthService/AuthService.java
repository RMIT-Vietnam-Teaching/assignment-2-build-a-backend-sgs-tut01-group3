package group_3.service.AuthService;
/**
 * @author Group 3
 */
import group_3.model.Person;

import java.util.Optional;

public interface AuthService {
    Optional<Person> login(String username, String rawPassword);

    void logout();

    boolean isAuthenticated();

    boolean hasRole();

    Person getCurrentUser();
}
