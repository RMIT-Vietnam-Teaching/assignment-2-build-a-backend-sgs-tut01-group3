package group_3.model;
/**
 * @author Group 3
 */
import java.time.OffsetDateTime;

public class SystemHistory {
    private long logId;
    private OffsetDateTime timestamp;
    private Integer userId;
    private String operationType;
    private String details;

    public SystemHistory(){}

    public SystemHistory(long logId, OffsetDateTime timestamp,Integer userId, String operationType, String details){
        this.logId = logId;
        this.timestamp = timestamp;
        this.userId = userId;
        this.operationType = operationType;
        this.details = details;
    }

    // GETTER
    public long getLogId(){
        return this.logId;
    }

    public OffsetDateTime getTimestamp(){
        return this.timestamp;
    }

    public Integer getUserId(){
        return this.userId;
    }

    public String getOperationType(){
        return this.operationType;
    }

    public String getDetails(){
        return this.details;
    }

    // SETTER
    public void setLogId(long logId){
        this.logId =logId;
    }

    public void setTimestamp(OffsetDateTime timestamp){
        this.timestamp = timestamp;
    }

    public void setUserId(Integer userId){
        this.userId = userId;
    }

    public void setOperationType(String operationType){
        this.operationType = operationType;
    }

    public void setDetails(String details) {
        this.details = details;
    }


}
