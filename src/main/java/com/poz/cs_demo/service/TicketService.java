package com.poz.cs_demo.service;


import com.poz.cs_demo.dto.ticket.AssigneeResponse;
import com.poz.cs_demo.dto.ticket.CreateTicketRequest;
import com.poz.cs_demo.dto.ticket.SearchTicketRequest;
import com.poz.cs_demo.dto.ticket.SearchTicketResponse;
import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.entity.Ticket;
import com.poz.cs_demo.entity.TicketComment;
import com.poz.cs_demo.enums.TicketChannel;
import com.poz.cs_demo.exception.ApiException;
import com.poz.cs_demo.repository.AgentRepository;
import com.poz.cs_demo.repository.TicketCommentRepository;
import com.poz.cs_demo.repository.TicketRepository;
import com.poz.cs_demo.security.CurrentAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 工單建立與列表查詢的業務邏輯。
 * <p>
 * 提供的方法：
 * <ul>
 *   <li>{@link #createTicket(CreateTicketRequest, TicketChannel)}：建立工單並留下第一筆處理記錄</li>
 *   <li>{@link #search(SearchTicketRequest)}：工單列表搜尋（分頁）</li>
 *   <li>{@link #getAssignees()}：「轉派給其他客服」下拉選單的客服清單</li>
 * </ul>
 * <p>
 * 單張工單的詳情、改狀態、轉接在 {@link TicketDetailService}；這裡只管「開單」與「找單」。
 */
@Service
@RequiredArgsConstructor
public class TicketService {
    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final CurrentAgent currentAgent;

    /**
     * 建立工單。
     * <p>
     * 兩個入口共用這支方法，差別只在 channel：
     * 通話工作台「建立工單並結束通話」傳 PHONE、首頁「＋ 新增派件」傳 AGENT，
     * 由 Controller 依呼叫的 API 決定，不從 request body 帶入。
     *
     * @param request 表單內容；assigneeId 為 null 或空字串時，負責人為目前登入者
     * @param channel 派單來源，PHONE 或 AGENT
     */
    @Transactional
    public void createTicket(CreateTicketRequest request, TicketChannel channel){
        // 建立工單的人 = 目前登入者，處理記錄要記在他名下
        Agent operator = agentRepository.findById(currentAgent.currentAgentId())
                .orElseThrow(() -> ApiException.unauthorized("登入已失效"));

        // 沒勾「轉派給其他客服」時 assigneeId 不會帶，負責人就是自己；有勾才另外查
        Agent assignee = operator;
        if (request.assigneeId() != null && !request.assigneeId().isBlank()) {
            assignee = agentRepository.findById(request.assigneeId())
                    .orElseThrow(() -> ApiException.notFound("查無此客服"));
        }

        Ticket ticket = Ticket.builder()
                .title(request.title())
                .customerName(request.customerName())
                .contactPhone(request.contactPhone())
                .category(request.category())
                .channel(channel)
                .assignee(assignee)
                .description(request.description())
                .status(request.status())
                .build();
        ticketRepository.save(ticket);

        String content = channel == TicketChannel.PHONE
                ? "工單經電話進線建立"
                : "工單由客服手動建立";

        TicketComment ticketComment = TicketComment.builder()
                .ticket(ticket)
                .agent(operator)
                .content(content)
                .build();
        ticketCommentRepository.save(ticketComment);
    }

    /** 工單列表搜尋（分頁），條件整理與分頁設定都在 SearchTicketRequest 裡做完了 */
    @Transactional(readOnly = true)
    public Page<SearchTicketResponse> search(SearchTicketRequest request) {
        return ticketRepository.search(
                request.ticketNo(),
                request.customerName(),
                request.contactPhone(),
                request.status(),
                request.assigneeId(),
                request.updatedFromInclusive(),
                request.updatedToExclusive(),
                request.toPageable()
        ).map(SearchTicketResponse::from);
    }

    /**
     * 「轉派給其他客服」下拉選單的選項：所有客服的代號與姓名。
     * <p>
     * 建單與詳情頁轉派都會用到，所以放在這裡而不是 TicketDetailService。
     * 一次全列、沒有分頁；要不要把「自己」或「目前負責人」濾掉交給前端決定。
     *
     * @return 所有客服，只含代號與姓名，依代號排序
     */
    @Transactional(readOnly = true)
    public List<AssigneeResponse> getAssignees() {
        return agentRepository.findAll(Sort.by("agentId"))
                .stream()
                .map(AssigneeResponse::from)
                .toList();
    }


}
