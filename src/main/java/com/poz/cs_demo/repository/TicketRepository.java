package com.poz.cs_demo.repository;

import com.poz.cs_demo.entity.Ticket;
import com.poz.cs_demo.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 工單 Repository。
 * 主鍵是 ticket_id（Integer），對外查詢請用 ticket_no（TK-000001）。
 */
public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    /**
     * 工單列表搜尋（分頁）。
     * <p>
     * 每個條件都寫成「(:參數 IS NULL OR 欄位條件)」：
     * 參數沒帶（null）時整組條件恆為 true、等於不過濾；有帶才真正比對。
     * <p>
     * JOIN FETCH t.assignee：列表要顯示客服代號，一次把客服撈出來，避免每列再各查一次。
     * 分頁需要另外算總筆數，而 COUNT 不能配 FETCH，所以 countQuery 要獨立寫。
     */
    @Query(value = """
            SELECT t
            FROM Ticket t
            JOIN FETCH t.assignee a
            WHERE (:ticketNo     IS NULL OR t.ticketNo     LIKE %:ticketNo%)
              AND (:customerName IS NULL OR t.customerName LIKE %:customerName%)
              AND (:contactPhone IS NULL OR t.contactPhone LIKE %:contactPhone%)
              AND (:status       IS NULL OR t.status       = :status)
              AND (:assigneeId   IS NULL OR a.agentId      = :assigneeId)
              AND (:updatedFrom  IS NULL OR t.updatedAt    >= :updatedFrom)
              AND (:updatedTo    IS NULL OR t.updatedAt    <  :updatedTo)
            """,
            countQuery = """
            SELECT COUNT(t)
            FROM Ticket t
            JOIN t.assignee a
            WHERE (:ticketNo     IS NULL OR t.ticketNo     LIKE %:ticketNo%)
              AND (:customerName IS NULL OR t.customerName LIKE %:customerName%)
              AND (:contactPhone IS NULL OR t.contactPhone LIKE %:contactPhone%)
              AND (:status       IS NULL OR t.status       = :status)
              AND (:assigneeId   IS NULL OR a.agentId      = :assigneeId)
              AND (:updatedFrom  IS NULL OR t.updatedAt    >= :updatedFrom)
              AND (:updatedTo    IS NULL OR t.updatedAt    <  :updatedTo)
            """)
    Page<Ticket> search(String ticketNo,
                        String customerName,
                        String contactPhone,
                        TicketStatus status,
                        String assigneeId,
                        LocalDateTime updatedFrom,
                        LocalDateTime updatedTo,
                        Pageable pageable);

    Optional<Ticket> findByTicketNo(String ticketNo);
}
