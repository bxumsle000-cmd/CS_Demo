package com.poz.cs_demo.controller;

import com.poz.cs_demo.dto.followup.AssignableTicketResponse;
import com.poz.cs_demo.dto.followup.CreateFollowUpRequest;
import com.poz.cs_demo.dto.followup.FollowUpGetListRequest;
import com.poz.cs_demo.dto.followup.FollowUpGetListResponse;
import com.poz.cs_demo.service.FollowUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行事曆回電安排相關 API，只操作目前登入者自己的安排。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follow-ups")
public class FollowUpController {
    private final FollowUpService followUpService ;

    /** 查詢區間內的回電安排（from、to 為 query 參數） */
    @GetMapping
    public List<FollowUpGetListResponse> list(FollowUpGetListRequest request) {
        return followUpService.getList(request);
    }

    @GetMapping("/tickets")
    public List<AssignableTicketResponse> getNonFinishTickets(){
        return followUpService.getNonFinishTickets();
    }

    /** 加入案件到某一天（同一張工單已有安排則覆蓋） */
    @PostMapping
    public void create(@RequestBody CreateFollowUpRequest request) {
        followUpService.createFollowUp(request);
    }

    /** 刪除回電安排 */
    @DeleteMapping("/{followUpId}")
    public void delete(@PathVariable Integer followUpId) {
        followUpService.deleteFollowUp(followUpId);
    }
}
