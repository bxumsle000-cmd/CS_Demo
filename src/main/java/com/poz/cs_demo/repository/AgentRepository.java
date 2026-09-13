package com.poz.cs_demo.repository;

import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.enums.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 客服人員 Repository。
 * 主鍵是 String（agent_id，例如 CSC00001），所以第二個泛型參數是 String。
 */
public interface AgentRepository extends JpaRepository<Agent, String> {

}
