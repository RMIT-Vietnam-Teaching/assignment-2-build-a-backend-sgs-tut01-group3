package group_3.service.SystemHistoryService;
/**
 * @author Group 3
 */
import group_3.model.SystemHistory;
import java.util.*;

public interface SystemHistoryService {
    void logAction(Integer user_id, String operationType, String details);

    List<SystemHistory> getAllHistory();
}
