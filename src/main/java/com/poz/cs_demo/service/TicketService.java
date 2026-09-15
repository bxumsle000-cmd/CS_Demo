package com.poz.cs_demo.service;


import com.poz.cs_demo.dto.ticket.CreateTicketRequest;
import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.entity.Ticket;
import com.poz.cs_demo.enums.TicketStatus;
import com.poz.cs_demo.exception.ApiException;
import com.poz.cs_demo.repository.AgentRepository;
import com.poz.cs_demo.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {
    private AgentRepository agentRepository;
    private TicketRepository ticketRepository ;

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
}
