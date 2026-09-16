package com.poz.cs_demo.service;

import com.poz.cs_demo.dto.followup.CreateFollowUpRequest;
import com.poz.cs_demo.dto.followup.CreateFollowUpResponse;
import com.poz.cs_demo.dto.followup.FollowUpGetListRequest;
import com.poz.cs_demo.dto.followup.FollowUpGetListResponse;
import com.poz.cs_demo.entity.FollowUp;
import com.poz.cs_demo.repository.FollowUpRepository;
import com.poz.cs_demo.security.CurrentAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowUpService {
    private final CurrentAgent currentAgent;
    private final FollowUpRepository followUpRepository;

    @Transactional
    public List<FollowUpGetListResponse> getList(FollowUpGetListRequest request){
        return followUpRepository.findRangeByAgent(
                currentAgent.currentAgentId(),request.fromInclusive(),request.toExclusive())
                .stream()
                .map(FollowUpGetListResponse::from)
                .toList();
    }


    @Transactional
    public CreateFollowUpResponse createFollowUp(CreateFollowUpRequest request){
        return null;
    }

}
