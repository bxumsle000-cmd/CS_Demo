package com.poz.cs_demo.dto.followup;

import com.poz.cs_demo.enums.TicketStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record CreateFollowUpRequest(
        String ticketNo,
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime date,
        @DateTimeFormat(pattern = "HH:mm") LocalDateTime time
) {
}
