SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- ---------------------------------------------------------------------
-- 客服狀態新增 OFFLINE（離線）
-- ---------------------------------------------------------------------
--
-- 登出時把狀態設為 OFFLINE，讓其他人能分辨「在線上」和「已下班」。
-- OFFLINE 只由登出設定、登入時自動回到 ONLINE，不開放手動選擇（規則同 ON_CALL）。
-- SQL Server 的 CHECK 不能直接修改，只能先 DROP 再重建。
ALTER TABLE [dbo].[agents] DROP CONSTRAINT [CK_agents_status]
GO
ALTER TABLE [dbo].[agents] ADD CONSTRAINT [CK_agents_status]
    CHECK ([status] IN (N'ONLINE', N'ON_CALL', N'BREAK', N'RESTROOM', N'LUNCH', N'MEETING', N'OFFLINE'))
GO
