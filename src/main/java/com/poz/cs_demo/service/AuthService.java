package com.poz.cs_demo.service;

import com.poz.cs_demo.dto.agent.AgentResponse;
import com.poz.cs_demo.dto.auth.LoginRequest;
import com.poz.cs_demo.dto.auth.LoginResponse;
import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.enums.AgentStatus;
import com.poz.cs_demo.exception.ApiException;
import com.poz.cs_demo.repository.AgentRepository;
import com.poz.cs_demo.security.CurrentAgent;
import com.poz.cs_demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登入 / 認證相關的業務邏輯。
 * <p>
 * 提供的方法：
 * <ul>
 *   <li>{@link #login(LoginRequest)}：登入</li>
 *   <li>{@link #logout()}：登出</li>
 *   <li>{@link #me()}：取得目前登入者資料</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentAgent currentAgent;
    private final JwtService jwtService;

    /**
     * 登入：驗證帳號密碼，成功後把狀態設為 ONLINE 並回傳 JWT。
     * <p>
     * 之後前端每次呼叫 API 都要在 Authorization header 帶這個 token，
     * 後端靠它辨認「現在是誰」。
     *
     * @param request agentId（客服代號）、password（明碼密碼）
     * @return agentId 與 JWT token
     */
    @Transactional()
    public LoginResponse login(LoginRequest request) {
        Agent agent = agentRepository.findById(request.agentId())
                .orElseThrow(() -> ApiException.unauthorized("帳號或密碼錯誤"));

        if (!passwordEncoder.matches(request.password(), agent.getPasswordHash())) {
            throw ApiException.unauthorized("帳號或密碼錯誤");
        }

        agent.setStatus(AgentStatus.ONLINE);

        String token = jwtService.generateToken(agent.getAgentId());
        return new LoginResponse(agent.getAgentId(), token);
    }

    /**
     * 登出（尚未實作）。
     */
    @Transactional
    public void logout(){
    }

    /**
     * 取得目前登入者的資料（不含密碼）。
     *
     * @return 目前登入者的 agentId、name、status
     */
    @Transactional
    public AgentResponse me(){
        Agent agent = agentRepository.findById(currentAgent.currentAgentId())
                .orElseThrow(()-> ApiException.unauthorized("登入已失效") );

        return  AgentResponse.from(agent);
    }

}
