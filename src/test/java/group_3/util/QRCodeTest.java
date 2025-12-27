package group_3.util;

import group_3.model.Ticket;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QRCodeTest {
    @Test
    void generateValidQRPayload(){
        Ticket ticket = new Ticket();
        ticket.setTicketID(1);
        ticket.setAttendeeID(10);
        ticket.setEventID(5);
        ticket.setSessionID(0);

        String qr = QRCode.generateTicketQRPayload(ticket);

        assertNotNull(qr);
        assertTrue(qr.contains("\ticketId\": 1"));
    }
}
