package com.poz.cs_demo.service;


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
import org.springframework.stereotype.Service;

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
    public void createTicket(CreateTicketRequest request, TicketChannel channel){
        // 沒勾「轉派給其他客服」時 assigneeId 不會帶，預設由自己負責
        String assigneeId = request.assigneeId();
        if (assigneeId == null || assigneeId.isBlank()) {
            assigneeId = currentAgent.currentAgentId();
        }

        Agent agent = agentRepository.findById(assigneeId)
                .orElseThrow( ()-> ApiException.unauthorized("查無此客服"));

        Ticket ticket = Ticket.builder()
                .title(request.title())
                .customerName(request.customerName())
                .contactPhone(request.contactPhone())
                .category(request.category())
                .channel(channel)
                .assignee(agent)
                .description(request.description())
                .status(request.status())
                .build();
        ticketRepository.save(ticket);

        TicketComment ticketComment = TicketComment.builder()
                .ticket(ticket)
                .agent(agent)
                .content("系統 · 工單經電話進線建立")
                .build();
        ticketCommentRepository.save(ticketComment);

    }

    /** 工單列表搜尋（分頁），條件整理與分頁設定都在 SearchTicketRequest 裡做完了 */
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


}
