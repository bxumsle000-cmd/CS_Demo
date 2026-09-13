package com.poz.cs_demo.repository;

import com.poz.cs_demo.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 工單處理記錄 / 留言 Repository。
 */
public interface TicketCommentRepository extends JpaRepository<TicketComment, Integer> {


}
