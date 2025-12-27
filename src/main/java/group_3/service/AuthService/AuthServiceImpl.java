package group_3.service.AuthService;

import group_3.dao.PersonDAO;
import group_3.dao.impl.PersonDAOImpl;
import group_3.model.Person;
import group_3.util.PasswordUtil;

import java.util.Optional;

/**
 * @author Group3
 */
public class AuthServiceImpl implements AuthService {

    private final PersonDAO personDAO = new PersonDAOImpl();

    @Override
    public Optional<Person> login(String username, String rawPassword) {

        Optional<Person> userOpt = personDAO.findByUsername(username);

        if (userOpt.isPresent()) {
            Person user = userOpt.get();
            if (PasswordUtil.verifyPassword(rawPassword, user.getPasswordHash())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
}
