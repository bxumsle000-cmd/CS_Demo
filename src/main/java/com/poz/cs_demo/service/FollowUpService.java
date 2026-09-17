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

/**
 * 行事曆回電安排的業務邏輯，只操作「目前登入者自己」的行事曆。
 * <p>
 * 提供的方法：
 * <ul>
 *   <li>{@link #getList(FollowUpGetListRequest)}：查月曆範圍內的回電安排</li>
 *   <li>{@link #getNonFinishTickets()}：「加入案件到這天」下拉選單可選的工單</li>
 *   <li>{@link #createFollowUp(CreateFollowUpRequest)}：把工單排進某一天</li>
 *   <li>{@link #deleteFollowUp(Integer)}：刪除一筆回電安排</li>
 * </ul>
 * <p>
 * 主人（agent）一律從 {@link CurrentAgent} 取得，不從請求帶入：
 * 查詢只撈自己的、刪除也只能刪自己的，別人的安排一概當作不存在（404）。
 */
@Service
@RequiredArgsConstructor
public class FollowUpService {
    private final CurrentAgent currentAgent;
    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;
    private final FollowUpRepository followUpRepository;

    /**
     * 查目前登入者在指定日期範圍內的回電安排，依時間由早到晚。
     * <p>
     * 回傳扁平清單、不依日期分組，月曆格子上的件數由前端自己 group by 日期。
     * FollowUp.ticket 是 LAZY，Repository 已用 JOIN FETCH 一次撈出，這裡轉 Response 不會再多查。
     *
     * @param request from / to：月曆實際畫出來的第一格與最後一格（含鄰月格子）
     * @return 範圍內的回電安排，含單號、標題、姓名、電話、狀態與備註
     */
    @Transactional(readOnly = true)
    public List<FollowUpGetListResponse> getList(FollowUpGetListRequest request){
        return followUpRepository.findRangeByAgent(
                currentAgent.currentAgentId(),request.fromInclusive(),request.toExclusive())
                .stream()
                .map(FollowUpGetListResponse::from)
                .toList();
    }

    /**
     * 「加入案件到這天」下拉選單的選項：目前登入者名下、還在處理（處理中 / 等待客戶回覆）的工單。
     * <p>
     * 狀態條件寫死在 Repository 的 JPQL 裡（IN_PROGRESS, PENDING），
     * 這裡只負責帶入登入者並轉成 Response；下拉選單一次全列，沒有分頁。
     *
     * @return 可排進行事曆的工單，依更新時間新到舊；只含單號、標題、狀態
     */
    @Transactional(readOnly = true)
    public List<AssignableTicketResponse> getNonFinishTickets(){
        return ticketRepository.findOpenByAssignee(currentAgent.currentAgentId())
                .stream()
                .map(AssignableTicketResponse::from)
                .toList();
    }

    /**
     * 把一張工單排進目前登入者行事曆的某個時間。
     * <p>
     * 同一張工單可以排任意多筆、時間重複也不限制（V2 已拿掉唯一鍵），所以這裡不做「已存在就覆蓋」的判斷。
     * followUp 是 new 出來的，JPA 還不認識它，要 save() 才會 INSERT。
     *
     * @param request ticketNo（單號；不存在回 404）、followUpAt（回電時間）、note（備註，可為 null）
     */
    @Transactional
    public void createFollowUp(CreateFollowUpRequest request){
        Agent agent = currentOperator();
        Ticket ticket = ticketRepository.findByTicketNo(request.ticketNo())
                .orElseThrow(() -> ApiException.notFound("沒有這個單號：" + request.ticketNo()));

        FollowUp followUp = FollowUp.builder()
                .agent(agent)
                .ticket(ticket)
                .followUpAt(request.followUpAt())
                .note(request.note())
                .build();
        followUpRepository.save(followUp);
    }

    /**
     * 刪除一筆回電安排，只能刪自己的。
     * <p>
     * 先查出來再刪、而不是直接 deleteById：一來 deleteById 找不到時會靜悄悄跳過、前端以為成功；
     * 二來查出來的物件剛好拿來比對主人。followUp.agent 是 LAZY，比對一定要在交易內做。
     *
     * @param followUpId 安排的識別碼，來自 GET 列表回傳的 followUpId；不存在或不是自己的都回 404
     */
    @Transactional
    public void deleteFollowUp(Integer followUpId){
        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> ApiException.notFound("沒有這個回電安排"));

        // 不是自己的安排也回 404 而非 403：不讓外人靠回應碼猜出這個 id 存在
        if (!followUp.getAgent().getAgentId().equals(currentAgent.currentAgentId())) {
            throw ApiException.notFound("沒有這個回電安排");
        }
        followUpRepository.delete(followUp);
    }

    // ==========================================================================================
    //  內部工具：行事曆 API 共用的小步驟
    // ==========================================================================================

    /** 目前登入的客服；查不到代表登入已失效，丟 401 */
    private Agent currentOperator() {
        return agentRepository.findById(currentAgent.currentAgentId())
                .orElseThrow(() -> ApiException.unauthorized("登入已失效"));
    }

}
