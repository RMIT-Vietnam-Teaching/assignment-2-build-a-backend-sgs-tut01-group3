package group_3.dao;

import group_3.model.SystemHistory;
import java.util.*;

public interface SystemHistoryDAO {
    void create(SystemHistory history);

    List<SystemHistory> findAll();
}
