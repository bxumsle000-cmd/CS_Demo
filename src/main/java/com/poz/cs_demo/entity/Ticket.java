package com.poz.cs_demo.entity;

import com.poz.cs_demo.enums.TicketChannel;
import com.poz.cs_demo.enums.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Generated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 工單，對應資料表 tickets。
 */
@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    /** 工單流水號（內部主鍵，不對外），由資料庫 IDENTITY 產生 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id", nullable = false)
    private Integer ticketId;

    /**
     * 對外顯示的工單編號，格式 TK-000001。
     * 這是資料庫的計算欄位（由 ticket_id 推導），Java 這邊絕對不能寫入：
     * - insertable/updatable = false：INSERT / UPDATE 都不送這個欄位
     * - @Generated：告訴 Hibernate 存檔後要回頭把資料庫算好的值讀回來
     */
    @Generated
    @Column(name = "ticket_no", insertable = false, updatable = false)
    private String ticketNo;

    /** 通話中向客戶確認的姓名，未提供可為 NULL */
    @Column(name = "customer_name", length = 255)
    private String customerName;

    /** 客戶提供的聯絡電話，用來查歷史紀錄 */
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    /** 工單主旨 */
    @Column(name = "title", length = 50, nullable = false)
    private String title;

    /** 問題描述內容，欄位型別是 NVARCHAR(MAX) */
    @Column(name = "description")
    private String description;

    /** 處理狀態 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TicketStatus status;

    /** 問題分類，例如 帳號問題 / 付款、發票 */
    @Column(name = "category", length = 255, nullable = false)
    private String category;

    /** 派單來源 */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 10, nullable = false)
    private TicketChannel channel;

    /**
     * 負責處理的客服。
     * LAZY：查工單時不會自動把客服一起撈出來，真的呼叫 getAssignee() 才查。
     * 排除在 toString 之外，否則印 log 時會觸發延遲載入。
     */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignee_id", nullable = false)
    private Agent assignee;

    /** 建立時間，建立後不再更動 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 最後更新時間，由 @PreUpdate 維護 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 欄位是 DATETIME2(0)，精度到秒，這裡先截掉毫秒讓 Java 與資料庫的值一致 */
    private static LocalDateTime nowToSecond() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = nowToSecond();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = nowToSecond();
    }
}
