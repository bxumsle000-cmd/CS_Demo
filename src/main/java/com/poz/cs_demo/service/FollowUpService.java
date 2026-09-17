package com.poz.cs_demo.service;

import com.poz.cs_demo.dto.followup.AssignableTicketResponse;
import com.poz.cs_demo.dto.followup.CreateFollowUpRequest;
import com.poz.cs_demo.dto.followup.FollowUpGetListRequest;
import com.poz.cs_demo.dto.followup.FollowUpGetListResponse;
import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.entity.FollowUp;
import com.poz.cs_demo.entity.Ticket;
import com.poz.cs_demo.exception.ApiException;
import com.poz.cs_demo.repository.AgentRepository;
import com.poz.cs_demo.repository.FollowUpRepository;
import com.poz.cs_demo.repository.TicketRepository;
import com.poz.cs_demo.security.CurrentAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowUpService {
    private final CurrentAgent currentAgent;
    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;
    private final FollowUpRepository followUpRepository;

    @Transactional
    public List<FollowUpGetListResponse> getList(FollowUpGetListRequest request){
        return followUpRepository.findRangeByAgent(
                currentAgent.currentAgentId(),request.fromInclusive(),request.toExclusive())
                .stream()
                .map(FollowUpGetListResponse::from)
                .toList();
    }

    /**
     * 「加入案件到這天」下拉選單的選項：目前登入者名下、還在處理（處理中 / 等待客戶回覆）的工單。
     * 狀態條件寫死在 Repository 的 JPQL 裡，這裡只負責帶入登入者並轉成 Response。
     */
    @Transactional(readOnly = true)
    public List<AssignableTicketResponse> getNonFinishTickets(){
        return ticketRepository.findOpenByAssignee(currentAgent.currentAgentId())
                .stream()
                .map(AssignableTicketResponse::from)
                .toList();
    }

    @Transactional
    public void createFollowUp(CreateFollowUpRequest request){
        Agent agent = currentOperator();
        Ticket ticket = findTicketOrThrow(request.ticketNo());
        FollowUp followUp = FollowUp.builder()
                .agent(agent)
                .ticket(ticket)
                .followUpAt(request.followUpAt())
                .note(request.note())
                .build();
        followUpRepository.save(followUp);
    }





    // ==========================================================================================
    //  內部工具：五支詳情頁 API 共用的小步驤，組合起來就是各 public 方法的主流程
    // ==========================================================================================

    /** 依單號找工單，找不到就丟 404；詳情頁的每支 API 都會用到 */
    private Ticket findTicketOrThrow(String ticketNo) {
        return ticketRepository.findByTicketNo(ticketNo)
                .orElseThrow(() -> ApiException.notFound("沒有這個單號：" + ticketNo));
    }

    /** 目前登入的客服；查不到代表登入已失效，丟 401 */
    private Agent currentOperator() {
        return agentRepository.findById(currentAgent.currentAgentId())
                .orElseThrow(() -> ApiException.unauthorized("登入已失效"));
    }

}
