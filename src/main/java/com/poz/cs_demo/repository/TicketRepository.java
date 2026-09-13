package com.poz.cs_demo.repository;

import com.poz.cs_demo.entity.Ticket;
import com.poz.cs_demo.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 工單 Repository。
 * 主鍵是 ticket_id（Integer），對外查詢請用 ticket_no（TK-000001）。
 */
public interface TicketRepository extends JpaRepository<Ticket, Integer> {

}
