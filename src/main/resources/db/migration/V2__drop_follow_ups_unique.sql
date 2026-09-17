SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- ---------------------------------------------------------------------
-- 拿掉回電安排的唯一鍵
-- ---------------------------------------------------------------------
--
-- V1 建的 UQ_follow_ups_agent_ticket_time 限制「同一客服 + 同一工單 + 同一時間」只能有一筆。
-- 設計改成同一張工單可以排任意多筆安排、時間重複也不限制，所以把這條唯一鍵拿掉。
-- 查詢用的索引 IX_follow_ups_agent_time（agent_id, follow_up_at）不受影響，保留。
ALTER TABLE [dbo].[follow_ups] DROP CONSTRAINT [UQ_follow_ups_agent_ticket_time]
GO
