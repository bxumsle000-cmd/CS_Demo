package com.poz.cs_demo.entity;

import com.poz.cs_demo.enums.AgentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 客服人員，對應資料表 agents。
 */
@Entity
@Table(name = "agents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    /** 客服代號，例如 CSC00001。由人工指定，不是自動流水號 */
    @Id
    @Column(name = "agent_id", length = 10, nullable = false)
    private String agentId;

    /** 客服姓名 */
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    /** 登入密碼雜湊值（BCrypt）。排除在 toString 之外，避免印到 log */
    @ToString.Exclude
    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    /**
     * 目前工作狀態。
     * JPA 每次 INSERT 都會把所有欄位送出去，資料庫的 DEFAULT 不會生效，
     * 所以預設值在 Java 這邊給（Builder 也要用 @Builder.Default 才會套用）。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private AgentStatus status = AgentStatus.ONLINE;

    /** 帳號建立時間，建立後不再更動 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 最後更新時間，由 @PreUpdate 維護 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================================================================
    // 時間設定
    // ==================================================================
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
