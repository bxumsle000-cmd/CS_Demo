package com.poz.cs_demo.dto.followup;

import com.poz.cs_demo.entity.FollowUp;
import com.poz.cs_demo.entity.Ticket;
import com.poz.cs_demo.enums.TicketStatus;

import java.time.LocalDateTime;

public record CreateFollowUpResponse(
        LocalDateTime followUpAt,
        String title,
        String ticketNo,
        String customerName,
        String contactPhone,
        TicketStatus status
) {
    public static CreateFollowUpResponse from(FollowUp followUp){
        Ticket ticket = followUp.getTicket();
        return new CreateFollowUpResponse(
                followUp.getFollowUpAt(),
                ticket.getTitle(),
                ticket.getTicketNo(),
                ticket.getCustomerName(),
                ticket.getContactPhone(),
                ticket.getStatus()
        );
    }
}
