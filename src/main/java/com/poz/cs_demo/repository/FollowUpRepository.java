package com.poz.cs_demo.repository;

import com.poz.cs_demo.entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 行事曆回電安排 Repository。
 */
public interface FollowUpRepository extends JpaRepository<FollowUp, Integer> {

}
