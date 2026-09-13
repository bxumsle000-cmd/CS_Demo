package com.poz.cs_demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 行事曆回電安排，對應資料表 follow_ups。
 */
@Entity
@Table(
        name = "follow_ups",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_follow_ups_agent_ticket_time",
                columnNames = {"agent_id", "ticket_id", "follow_up_at"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUp {
    /** 流水號，同時是對外露出的識別碼，由資料庫 IDENTITY 產生 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_up_id", nullable = false)
    private Integer followUpId;

    /** 這筆安排的主人，也就是「誰的行事曆」 */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    /** 要跟進的工單 */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    /** 排定的回電時間，精度到秒 */
    @Column(name = "follow_up_at", nullable = false)
    private LocalDateTime followUpAt;

    /** 個人備註，只有主人看得到 */
    @Column(name = "note", length = 200)
    private String note;
}
