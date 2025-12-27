package group_3.service.SystemHistoryService;
/**
 * @author Group 3
 */
import group_3.dao.SystemHistoryDAO;
import group_3.dao.impl.SystemHistoryDaoImpl;
import group_3.model.SystemHistory;

import java.time.*;
import java.util.*;

public class SystemHistoryServiceImpl implements SystemHistoryService{
    private final SystemHistoryDAO systemHistoryDAO = new SystemHistoryDaoImpl();

    @Override
    public void logAction(Integer user_id, String operationType, String details){
        SystemHistory history = new SystemHistory();

        history.setUserId(user_id);
        history.setOperationType(operationType);
        history.setDetails(details);
        history.setTimestamp(OffsetDateTime.now());

        systemHistoryDAO.create(history);
    }

    @Override
    public List<SystemHistory> getAllHistory(){
        return systemHistoryDAO.findAll();
    }
}
