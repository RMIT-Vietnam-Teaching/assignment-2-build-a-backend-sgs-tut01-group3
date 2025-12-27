package group_3.service.UserService;

import group_3.model.Person;
import group_3.model.Attendee;
import group_3.model.Presenter;
import group_3.dao.PersonDAO;
import group_3.dao.impl.PersonDAOImpl;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of UserService for managing all Person types.
 * Provides CRUD operations for Attendees, Presenters, and generic Person objects.
 * 
 * @author Group3
 */
public class UserServiceImpl implements UserService {
    
    private PersonDAO personDAO;
    
    public UserServiceImpl() {
        this.personDAO = new PersonDAOImpl();
    }
    
    public UserServiceImpl(PersonDAO personDAO) {
        this.personDAO = personDAO;
    }
    
    @Override
    public Attendee createAttendee(Attendee attendee) {
        if (attendee == null) {
            throw new IllegalArgumentException("Attendee cannot be null");
        }
        personDAO.create(attendee);
        return attendee;
    }
    
    @Override
    public Presenter createPresenter(Presenter presenter) {
        if (presenter == null) {
            throw new IllegalArgumentException("Presenter cannot be null");
        }
        personDAO.create(presenter);
        return presenter;
    }
    
    @Override
    public Person createPerson(Person person) {
        if (person == null) {
            throw new IllegalArgumentException("Person cannot be null");
        }
        personDAO.create(person);
        return person;
    }
    
    @Override
    public Optional<Person> getUserById(int userId) {
        return personDAO.findById(userId);
    }
    
    @Override
    public Optional<Person> getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        return personDAO.findByUsername(username);
    }
    
    @Override
    public List<Person> getAllUsers() {
        return personDAO.findAll();
    }
    
    @Override
    public List<Attendee> getAllAttendees() {
        return personDAO.findAll().stream()
            .filter(person -> person instanceof Attendee)
            .map(person -> (Attendee) person)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Presenter> getAllPresenters() {
        return personDAO.findAll().stream()
            .filter(person -> person instanceof Presenter)
            .map(person -> (Presenter) person)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Attendee> getAttendeeById(int attendeeId) {
        Optional<Person> person = personDAO.findById(attendeeId);
        if (person.isPresent() && person.get() instanceof Attendee) {
            return Optional.of((Attendee) person.get());
        }
        return Optional.empty();
    }
    
    @Override
    public Optional<Presenter> getPresenterById(int presenterId) {
        Optional<Person> person = personDAO.findById(presenterId);
        if (person.isPresent() && person.get() instanceof Presenter) {
            return Optional.of((Presenter) person.get());
        }
        return Optional.empty();
    }
    
    @Override
    public void updateUser(Person person) {
        if (person == null) {
            throw new IllegalArgumentException("Person cannot be null");
        }
        personDAO.update(person);
    }
    
    @Override
    public void updateAttendee(Attendee attendee) {
        if (attendee == null) {
            throw new IllegalArgumentException("Attendee cannot be null");
        }
        personDAO.update(attendee);
    }
    
    @Override
    public void updatePresenter(Presenter presenter) {
        if (presenter == null) {
            throw new IllegalArgumentException("Presenter cannot be null");
        }
        personDAO.update(presenter);
    }
    
    @Override
    public void deleteUser(int userId) {
        personDAO.delete(userId);
    }
    
    @Override
    public void deleteAttendee(int attendeeId) {
        personDAO.delete(attendeeId);
    }
    
    @Override
    public void deletePresenter(int presenterId) {
        personDAO.delete(presenterId);
    }
    
    @Override
    public boolean userExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return personDAO.findByUsername(username).isPresent();
    }
    
    @Override
    public int getTotalUserCount() {
        return personDAO.findAll().size();
    }
    
    @Override
    public int getTotalAttendeeCount() {
        return (int) personDAO.findAll().stream()
            .filter(person -> person instanceof Attendee)
            .count();
    }
    
    @Override
    public int getTotalPresenterCount() {
        return (int) personDAO.findAll().stream()
            .filter(person -> person instanceof Presenter)
            .count();
    }
}
