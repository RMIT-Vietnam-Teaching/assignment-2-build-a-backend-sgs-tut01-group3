package group_3.util;

/*
    @author group3
 */

import group_3.model.Ticket;

public final class QRCode {
    private QRCode(){}

    public static String generateTicketQRPayload (Ticket ticket){
        return String.format(
                "{ \"ticketId\": %d, \"attendeeId\": %d, \"eventId\": %d, \"sessionId\": %s }",
                ticket.getTicketID(),
                ticket.getAttendeeID(),
                ticket.getEventID(),
                ticket.getSessionID() == 0 ? "null" : ticket.getSessionID()
        );
    }
}
