package com.poz.cs_demo.repository;

import com.poz.cs_demo.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 工單處理記錄 / 留言 Repository。
 */
public interface TicketCommentRepository extends JpaRepository<TicketComment, Integer> {

    /**
     * 依單號查某張工單的所有處理記錄，舊到新排序。
     * 方法名的 Ticket_TicketNo 代表「沿著 ticket 關聯，比對 Ticket 的 ticketNo 欄位」。
     */
    List<TicketComment> findByTicket_TicketNoOrderByCreatedAtAsc(String ticketNo);
}
