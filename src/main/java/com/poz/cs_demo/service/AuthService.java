package com.poz.cs_demo.service;

import com.poz.cs_demo.dto.AgentResponse;
import com.poz.cs_demo.dto.LoginRequest;
import com.poz.cs_demo.dto.LoginResponse;
import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.enums.AgentStatus;
import com.poz.cs_demo.exception.ApiException;
import com.poz.cs_demo.repository.AgentRepository;
import com.poz.cs_demo.security.CurrentAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentAgent currentAgent;

    /**
     * 帳號不存在與密碼錯誤都回同一句話，
     * 避免讓外人靠錯誤訊息推測哪些帳號存在。
     */

    @Transactional()
    public LoginResponse login(LoginRequest request) {
        // 1. 是否有這個 id
        //    findById 回傳 Optional<Agent>：有就拿出來，沒有就丟 401
        Agent agent = agentRepository.findById(request.agentId())
                .orElseThrow(() -> ApiException.unauthorized("帳號或密碼錯誤"));

        // 2. 密碼是否正確
        //    matches(明文, 雜湊值)：BCrypt 會自己從雜湊值取出 salt 再比對，不能用 equals
        if (!passwordEncoder.matches(request.password(), agent.getPasswordHash())) {
            throw ApiException.unauthorized("帳號或密碼錯誤");
        }

        agent.setStatus(AgentStatus.ONLINE);

        return new LoginResponse(request.agentId(),currentAgent.currentToken());
    }

    @Transactional
    public void logout(){
    }

    @Transactional
    public AgentResponse me(){
        Agent agent = agentRepository.findById(currentAgent.currentAgentId())
                .orElseThrow(()-> ApiException.unauthorized("登入已失效") );

        return  AgentResponse.from(agent);
    }


}


