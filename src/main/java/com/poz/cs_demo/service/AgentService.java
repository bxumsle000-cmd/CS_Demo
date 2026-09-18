package com.poz.cs_demo.service;

import com.poz.cs_demo.dto.agent.ChangePasswordRequest;
import com.poz.cs_demo.dto.agent.RegisterRequest;
import com.poz.cs_demo.dto.agent.UpdateAgentStatusRequest;
import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.enums.AgentStatus;
import com.poz.cs_demo.exception.ApiException;
import com.poz.cs_demo.repository.AgentRepository;
import com.poz.cs_demo.security.CurrentAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



/**
 * 登入 / 認證相關的業務邏輯。
 * <p>
 * 提供的方法：
 * <ul>
 *   <li>{@link #register(RegisterRequest)}：建立客服帳號</li>
 *   <li>{@link #changePassword(ChangePasswordRequest)}：修改自己的密碼</li>
 *   <li>{@link #updateStatus(UpdateAgentStatusRequest)}：變更自己的工作狀態</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentAgent currentAgent;

    /**
     * 建立客服帳號：代號不可重複，密碼會先做 BCrypt 雜湊再存。
     *
     * @param request agentId（客服代號）、name（姓名）、password（明碼密碼）
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (agentRepository.existsById(request.agentId())) {
            throw ApiException.unauthorized("AgentId已存在，無法用同樣ID創建帳號");
        }

        Agent agent = Agent.builder()
                .agentId(request.agentId())
                .name(request.name())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        agentRepository.save(agent);
    }

    /**
     * 修改目前登入者自己的密碼：舊密碼要正確，且新密碼不可與舊密碼相同。
     *
     * @param request oldPassword（舊密碼）、newPassword（新密碼）
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Agent agent = agentRepository.findById(currentAgent.currentAgentId())
                .orElseThrow(() -> ApiException.unauthorized("登入已失效"));

        if (!passwordEncoder.matches(request.oldPassword(), agent.getPasswordHash())) {
            throw ApiException.unauthorized("密碼錯誤,若忘記請向主管反應");
        }

        if (passwordEncoder.matches(request.newPassword(), agent.getPasswordHash())) {
            throw ApiException.unauthorized("新密碼不能與舊密碼相同");
        }

        agent.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        agentRepository.save(agent);
    }

    /**
     * 變更目前登入者自己的工作狀態（BREAK / RESTROOM / LUNCH / MEETING 或回到 ONLINE）。
     * <p>
     * ON_CALL 是通話事件由系統設定的，不開放手動選擇，傳進來直接回 400。
     *
     * @param request status（新的工作狀態）；ON_CALL 回 400
     */
    @Transactional
    public void updateStatus(UpdateAgentStatusRequest request) {
        Agent agent = agentRepository.findById(currentAgent.currentAgentId())
                .orElseThrow(() -> ApiException.unauthorized("登入已失效"));

        if (request.status() == AgentStatus.ON_CALL) {
            throw ApiException.badRequest("ON_CALL 由系統設定，不可手動選擇");
        }

        agent.setStatus(request.status());
        agentRepository.save(agent);
    }
}
