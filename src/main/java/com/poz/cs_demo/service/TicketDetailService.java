package com.poz.cs_demo.service;

import com.poz.cs_demo.dto.detail.TicketCommentResponse;
import com.poz.cs_demo.dto.detail.TicketDetailResponse;
import com.poz.cs_demo.dto.detail.UpdateTicketAssigneeRequest;
import com.poz.cs_demo.dto.detail.UpdateTicketStatusRequest;
import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.entity.Ticket;
import com.poz.cs_demo.entity.TicketComment;
import com.poz.cs_demo.enums.TicketStatus;
import com.poz.cs_demo.exception.ApiException;
import com.poz.cs_demo.repository.AgentRepository;
import com.poz.cs_demo.repository.TicketCommentRepository;
import com.poz.cs_demo.repository.TicketRepository;
import com.poz.cs_demo.security.CurrentAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketDetailService {
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final AgentRepository agentRepository;
    private final CurrentAgent currentAgent;

    /** 依單號取得工單內容；單號不存在回 404 */
    @Transactional
    public TicketDetailResponse getDetail(String ticketNo) {
        Ticket ticket = findTicketOrThrow(ticketNo);
        return TicketDetailResponse.from(ticket, findComments(ticketNo));
    }

    @Transactional
    public TicketDetailResponse updateStatus(String ticketNo, UpdateTicketStatusRequest request) {
        Ticket ticket = findTicketOrThrow(ticketNo);

        TicketStatus oldStatus = ticket.getStatus();   // 先記下舊狀態，setStatus 之後就拿不到了
        ticket.setStatus(request.status());            // ticket 是查出來的，JPA 會自動 UPDATE

        addComment(ticket, currentOperator(), "狀態由 " + oldStatus + " 變更為 " + request.status());

        return TicketDetailResponse.from(ticket, findComments(ticketNo));
    }

    @Transactional
    public TicketDetailResponse assign(String ticketNo, UpdateTicketAssigneeRequest request){
        Ticket ticket = findTicketOrThrow(ticketNo);
        Agent assignedAgent = agentRepository.findById(request.assignId())
                .orElseThrow(() -> ApiException.notFound("查無此客服"));
        ticket.setAssignee(assignedAgent);

        addComment(ticket, currentOperator(), "由"+currentOperator()+" 轉接給 " + request.assignId());
        return  TicketDetailResponse.from(ticket, findComments(ticketNo));
    }


    // ==========================================================================================
    //  內部工具
    // ==========================================================================================
    /** 依單號撈該工單的處理記錄（舊到新），轉成 TicketCommentResponse 列表 */
    private List<TicketCommentResponse> findComments(String ticketNo) {
        return ticketCommentRepository.findByTicket_TicketNoOrderByCreatedAtAsc(ticketNo)
                .stream()
                .map(TicketCommentResponse::from)
                .toList();
    }

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

    /** 替工單留一筆處理記錄。comment 是 new 出來的，JPA 還不認識它，要 save() 才會 INSERT */
    private void addComment(Ticket ticket, Agent agent, String content) {
        ticketCommentRepository.save(TicketComment.builder()
                .ticket(ticket)
                .agent(agent)
                .content(content)
                .build());
    }
}

