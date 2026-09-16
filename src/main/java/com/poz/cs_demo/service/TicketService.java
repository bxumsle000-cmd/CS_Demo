package com.poz.cs_demo.service;


import com.poz.cs_demo.dto.ticket.CreateTicketRequest;
import com.poz.cs_demo.dto.ticket.SearchTicketRequest;
import com.poz.cs_demo.dto.ticket.SearchTicketResponse;
import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.entity.Ticket;
import com.poz.cs_demo.exception.ApiException;
import com.poz.cs_demo.repository.AgentRepository;
import com.poz.cs_demo.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;

    public void createTicket(CreateTicketRequest request){
        Agent agent = agentRepository.findById(request.assigneeId())
                .orElseThrow( ()-> ApiException.unauthorized("查無此客服"));

        Ticket ticket = Ticket.builder()
                .title(request.title())
                .customerName(request.customerName())
                .category(request.category())
                .assignee(agent)
                .description(request.description())
                .status(request.status())
                .build();
        ticketRepository.save(ticket);
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
