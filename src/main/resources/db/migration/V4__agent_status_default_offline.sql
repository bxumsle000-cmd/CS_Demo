SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- ---------------------------------------------------------------------
-- 客服狀態的預設值由 ONLINE 改為 OFFLINE
-- ---------------------------------------------------------------------
--
-- 新建立的帳號還沒登入過，不應該顯示成「在線上」；登入時才會改成 ONLINE。
-- SQL Server 的 DEFAULT 約束不能直接修改，只能先 DROP 再重建。
-- 只影響之後新增的資料列，既有客服的狀態不會被改動。
ALTER TABLE [dbo].[agents] DROP CONSTRAINT [DF_agents_status]
GO
ALTER TABLE [dbo].[agents] ADD CONSTRAINT [DF_agents_status]
    DEFAULT (N'OFFLINE') FOR [status]
GO
