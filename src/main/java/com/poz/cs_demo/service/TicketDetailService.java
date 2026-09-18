package com.poz.cs_demo.service;

import com.poz.cs_demo.dto.detail.*;
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

/**
 * 工單詳情頁相關的業務邏輯，一律以單號（ticketNo，例如 TK-000001）定位工單。
 * <p>
 * 提供的方法：
 * <ul>
 *   <li>{@link #getDetail(String)}：取得工單內容與處理記錄</li>
 *   <li>{@link #updateStatus(String, UpdateTicketStatusRequest)}：變更工單狀態</li>
 *   <li>{@link #assign(String, UpdateTicketAssigneeRequest)}：轉接給其他客服</li>
 * </ul>
 * <p>
 * 回傳的 {@link TicketDetailResponse} 都會包含最新的處理記錄列表，
 * 前端做完任何操作後不必再另外打一次 GET /comments。
 */
@Service
@RequiredArgsConstructor
public class TicketDetailService {
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final AgentRepository agentRepository;
    private final CurrentAgent currentAgent;

    /**
     * 取得工單內容（主旨、描述、狀態、客服、客戶等）與處理記錄列表。
     *
     * @param ticketNo 單號，例如 TK-000001；不存在回 404
     * @return 工單內容與處理記錄
     */
    @Transactional
    public TicketDetailResponse getDetail(String ticketNo) {
        Ticket ticket = findTicketOrThrow(ticketNo);
        return TicketDetailResponse.from(ticket, findComments(ticketNo));
    }

    /**
     * 變更工單狀態，並以目前登入者的名義留下一筆「狀態由 A 變更為 B」的處理記錄。
     * <p>
     * 改狀態與寫處理記錄放在同一個交易，要嘛一起成功、要嘛一起失敗。
     * Ticket 是在交易內查出來的，改完欄位後 JPA 會自動 UPDATE（dirty checking），
     * @PreUpdate 也會順便更新 updatedAt。
     *
     * @param ticketNo 單號；不存在回 404
     * @param request  status（新狀態：PENDING / RESOLVED / IN_PROGRESS）
     * @return 更新後的工單內容與處理記錄
     */
    @Transactional
    public TicketDetailResponse updateStatus(String ticketNo, UpdateTicketStatusRequest request) {
        Agent agent = currentOperator();
        Ticket ticket = findTicketOrThrow(ticketNo);

        TicketStatus oldStatus = ticket.getStatus();   // 先記下舊狀態，setStatus 之後就拿不到了
        ticket.setStatus(request.status());            // ticket 是查出來的，JPA 會自動 UPDATE

        addComment(agent, ticket, "狀態由 " + oldStatus + " 變更為 " + request.status());

        return TicketDetailResponse.from(ticket, findComments(ticketNo));
    }

    /**
     * 把工單轉接給其他客服，並留下一筆「由 X 轉接給 Y」的處理記錄。
     *
     * @param ticketNo 單號；不存在回 404
     * @param request  assignId（要轉接給的客服代號）；查無此客服回 404
     * @return 更新後的工單內容與處理記錄
     */
    @Transactional
    public TicketDetailResponse assign(String ticketNo, UpdateTicketAssigneeRequest request){
        Agent agent = currentOperator();
        Ticket ticket = findTicketOrThrow(ticketNo);
        Agent assignedAgent = agentRepository.findById(request.assignId())
                .orElseThrow(() -> ApiException.notFound("查無此客服"));
        ticket.setAssignee(assignedAgent);

        addComment(agent ,ticket,  "由"+currentOperator().getAgentId()+" 轉接給 " + request.assignId());
        return  TicketDetailResponse.from(ticket, findComments(ticketNo));
    }

    @Transactional
    public  TicketDetailResponse addComment(String ticketNo, AddTicketCommentRequest request){
        Agent agent = currentOperator();
        Ticket ticket = findTicketOrThrow(ticketNo);
        addComment(agent , ticket, request.content());
        return  TicketDetailResponse.from(ticket , findComments(ticketNo));
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

    /** 依單號撈該工單的處理記錄（舊到新），轉成 TicketCommentResponse 列表 */
    private List<TicketCommentResponse> findComments(String ticketNo) {
        return ticketCommentRepository.findByTicket_TicketNoOrderByCreatedAtAsc(ticketNo)
                .stream()
                .map(TicketCommentResponse::from)
                .toList();
    }

    /**
     * 替工單留一筆處理記錄。
     * comment 是 new 出來的，JPA 還不認識它，要 save() 才會 INSERT（不像查出來的 ticket 會自動 UPDATE）。
     *
     * @param ticket  所屬工單
     * @param agent   留言的客服；系統事件可傳 null
     * @param content 留言內容或系統事件描述
     */
    private void addComment( Agent agent,Ticket ticket, String content) {
        ticketCommentRepository.save(TicketComment.builder()
                .ticket(ticket)
                .agent(agent)
                .content(content)
                .build());
    }
}

