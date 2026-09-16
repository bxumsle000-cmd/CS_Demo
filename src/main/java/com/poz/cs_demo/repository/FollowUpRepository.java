package com.poz.cs_demo.repository;

import com.poz.cs_demo.entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 行事曆回電安排 Repository。
 */
public interface FollowUpRepository extends JpaRepository<FollowUp, Integer> {

    /**
     * 查某個客服在指定區間內的回電安排，依時間由早到晚排序。
     * <p>
     * 時間用半開區間 [from, to)：followUpAt 精度到秒，用「< 隔天 00:00」才不會漏掉結束日當天的安排。
     * <p>
     * JOIN FETCH f.ticket：右側明細要顯示單號、標題、姓名、電話、狀態，這些都在 Ticket 上，
     * 而 FollowUp.ticket 是 LAZY，不一次撈出來的話每一列都會再各查一次（N+1）。
     * agent 不用 FETCH：就是查詢者自己，明細也不顯示；
     * 而且 agentId 剛好是 Agent 的主鍵，比對時直接用 follow_ups.agent_id 這個外鍵欄位，不會真的去 join agents。
     * <p>
     * 行事曆本來就是整月一次看完，沒有分頁的概念，所以回 List 而不是 Page。
     */
    @Query("""
            SELECT f
            FROM FollowUp f
            JOIN FETCH f.ticket t
            WHERE f.agent.agentId = :agentId
              AND f.followUpAt   >= :from
              AND f.followUpAt   <  :to
            ORDER BY f.followUpAt
            """)
    List<FollowUp> findRangeByAgent(String agentId,
                                    LocalDateTime from,
                                    LocalDateTime to);

}
