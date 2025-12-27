package group_3.model;
/**
 * @author Group 3
 */
import group_3.model.enums.*;

public class Ticket {
    private int ticketID;
    private int eventID;
    private int sessionID;
    private int attendeeID;
    private TicketType type;
    private double price;
    private TicketStatus status;
    private String QRpath;

    public Ticket() {
        this.ticketID = 0;
        this.eventID = 0;
        this.sessionID = 0;
        this.attendeeID = 0;
        this.type = null;
        this.price = 0;
        this.status = null;
    }

    public Ticket(int ticketID, int eventID, int sessionID, int attendeeID, TicketType type, double price, TicketStatus status, String QRpath) {
        this.ticketID = ticketID;
        this.eventID = eventID;
        this.sessionID = sessionID;
        this.attendeeID = attendeeID;
        this.type = type;
        this.price = price;
        this.status = status;
        this.QRpath = QRpath;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public TicketType getType() {
        return type;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public int getAttendeeID() {
        return attendeeID;
    }

    public void setAttendeeID(int attendeeID) {
        this.attendeeID = attendeeID;
    }

    public int getSessionID() {
        return sessionID;
    }

    public void setSessionID(int sessionID) {
        this.sessionID = sessionID;
    }

    public int getEventID() {
        return eventID;
    }

    public void setEventID(int eventID) {
        this.eventID = eventID;
    }

    public int getTicketID() {
        return ticketID;
    }

    public void setTicketID(int ticketID) {
        this.ticketID = ticketID;
    }

    public String getQRpath() { return QRpath; }

    public void setQRpath(String QRpath) {
        this.QRpath = QRpath;
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "Ticket ID ='" + ticketID + '\'' +
                ", Event ID ='" + eventID + '\'' +
                ", Session ID ='" + sessionID + '\'' +
                ", Attendee ID ='" + attendeeID + '\'' +
                ", Type ='" + type + '\'' +
                ", Price =" + price +
                ", Status ='" + status + '\'' +
                ", QR Path ='" + QRpath + '\'' +
                '}';
    }
}
