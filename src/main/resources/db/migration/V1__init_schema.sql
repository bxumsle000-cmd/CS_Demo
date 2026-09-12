SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- ---------------------------------------------------------------------
-- 1. 客服人員
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[agents](
    [agent_id]      NVARCHAR(10)  NOT NULL,   -- 客服代號，例如 CSC00001
    [name]          NVARCHAR(50)  NOT NULL,   -- 客服姓名
    [password_hash] NVARCHAR(255) NOT NULL,   -- 登入密碼雜湊值（BCrypt）
    [status]        NVARCHAR(20)  NOT NULL,   -- 目前工作狀態，見下方 CHECK
    [created_at]    DATETIME2(0)  NOT NULL,   -- 帳號建立時間
    [updated_at]    DATETIME2(0)  NOT NULL,   -- 最後更新時間，由 JPA @PreUpdate 維護
    CONSTRAINT [PK_agents] PRIMARY KEY CLUSTERED ([agent_id] ASC)
)
GO

ALTER TABLE [dbo].[agents] ADD CONSTRAINT [DF_agents_status]
    DEFAULT (N'ONLINE') FOR [status]
GO
ALTER TABLE [dbo].[agents] ADD CONSTRAINT [DF_agents_created_at]
    DEFAULT (SYSDATETIME()) FOR [created_at]
GO
-- 客服狀態（ONLINE / LUNCH / ...）會一直變，這裡只留「最後更新時間」，不留狀態歷史。
-- 要做工時統計（每個狀態累計多久）得另開 agent_status_logs 一列一列記，那是另一個題目。
ALTER TABLE [dbo].[agents] ADD CONSTRAINT [DF_agents_updated_at]
    DEFAULT (SYSDATETIME()) FOR [updated_at]
GO

-- 狀態白名單。ON_CALL 由系統在通話事件時設定，其餘為客服手動選擇。
ALTER TABLE [dbo].[agents] ADD CONSTRAINT [CK_agents_status]
    CHECK ([status] IN (N'ONLINE', N'ON_CALL', N'BREAK', N'RESTROOM', N'LUNCH', N'MEETING'))
GO
-- ---------------------------------------------------------------------
-- 2. 工單
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[tickets](
    [ticket_id]     INT IDENTITY(1,1) NOT NULL,  -- 工單流水號（內部主鍵，不對外）
    [ticket_no]     AS (N'TK-' + CASE
                            WHEN [ticket_id] <= 999999
                            THEN RIGHT(N'000000' + CONVERT(NVARCHAR(10), [ticket_id]), 6)
                            ELSE CONVERT(NVARCHAR(10), [ticket_id])
                        END) PERSISTED NOT NULL,  -- 對外顯示的工單編號，格式 TK-000001
    [customer_name] NVARCHAR(255)     NULL,      -- 通話中向客戶確認的姓名，未提供可為 NULL
    [contact_phone] NVARCHAR(50)      NULL,      -- 客戶提供的聯絡電話，用來查歷史紀錄
    [title]         NVARCHAR(50)  NOT NULL,      -- 工單主旨，長度與 CreateTicketRequest 的 @Size(max = 50) 一致
    [description]   NVARCHAR(MAX)     NULL,      -- 問題描述內容
    [status]        NVARCHAR(20)  NOT NULL,      -- 處理狀態，見下方 CHECK
    [category]      NVARCHAR(255) NOT NULL,      -- 問題分類，例如 帳號問題 / 付款、發票
    [channel]       NVARCHAR(10)  NOT NULL,      -- 派單來源，見下方 CHECK
    [assignee_id]   NVARCHAR(10)  NOT NULL,      -- 負責處理的客服代號
    [created_at]    DATETIME2(0)  NOT NULL,      -- 建立時間
    [updated_at]    DATETIME2(0)  NOT NULL,      -- 最後更新時間，由 JPA @PreUpdate 維護
    CONSTRAINT [PK_tickets] PRIMARY KEY CLUSTERED ([ticket_id] ASC)
)
GO

-- ticket_no 由 ticket_id 推導、已經不可能重複，這條唯一約束是最後一道防線：
-- 萬一哪天有人改了運算式而算出重複值，會在寫入當下就爆，而不是等到出貨才發現。
ALTER TABLE [dbo].[tickets] ADD CONSTRAINT [UQ_tickets_ticket_no]
    UNIQUE NONCLUSTERED ([ticket_no] ASC)
GO

ALTER TABLE [dbo].[tickets] ADD CONSTRAINT [DF_tickets_created_at]
    DEFAULT (SYSDATETIME()) FOR [created_at]
GO
ALTER TABLE [dbo].[tickets] ADD CONSTRAINT [DF_tickets_updated_at]
    DEFAULT (SYSDATETIME()) FOR [updated_at]
GO

ALTER TABLE [dbo].[tickets]  ADD CONSTRAINT [FK_tickets_agents]
    FOREIGN KEY ([assignee_id]) REFERENCES [dbo].[agents] ([agent_id])
GO

ALTER TABLE [dbo].[tickets]  ADD CONSTRAINT [CK_tickets_status]
    CHECK ([status] IN (N'IN_PROGRESS', N'PENDING', N'RESOLVED'))
GO

-- 派單來源：PHONE = 通話工作台在通話中建立，Agent = 客服從「＋ 新增派件」手動建立。
-- 值寫成 Agent（首字大寫）是照需求原文。
-- 提醒：SQL Server 預設定序不分大小寫，所以程式送 AGENT、agent 也會通過這條 CHECK，
--       資料庫不會幫你統一大小寫，要一致得由後端自己保證。
ALTER TABLE [dbo].[tickets]  ADD CONSTRAINT [CK_tickets_channel]
    CHECK ([channel] IN (N'PHONE', N'AGENT'))
GO

CREATE NONCLUSTERED INDEX [IX_tickets_assignee_status_created]
    ON [dbo].[tickets] ([assignee_id] ASC, [status] ASC, [created_at] DESC)
GO

CREATE NONCLUSTERED INDEX [IX_tickets_contact_phone]
    ON [dbo].[tickets] ([contact_phone] ASC)
GO

-- ---------------------------------------------------------------------
-- 3. 工單處理記錄 / 留言
-- ---------------------------------------------------------------------
CREATE TABLE [dbo].[ticket_comments](
    [comment_id] INT IDENTITY(1,1) NOT NULL,  -- 留言／紀錄流水號
    [ticket_id]  INT           NOT NULL,      -- 所屬工單
    [agent_id]   NVARCHAR(10)      NULL,      -- 留言的客服代號，系統事件為 NULL
    [content]    NVARCHAR(MAX) NOT NULL,      -- 留言內容或系統事件描述
    [created_at] DATETIME2(0)  NOT NULL,      -- 建立時間
    CONSTRAINT [PK_ticket_comments] PRIMARY KEY CLUSTERED ([comment_id] ASC)
)
GO

ALTER TABLE [dbo].[ticket_comments] ADD CONSTRAINT [DF_ticket_comments_created_at]
    DEFAULT (SYSDATETIME()) FOR [created_at]
GO

-- 工單刪掉時，底下的處理記錄一起刪
ALTER TABLE [dbo].[ticket_comments] ADD CONSTRAINT [FK_ticket_comments_tickets]
    FOREIGN KEY ([ticket_id]) REFERENCES [dbo].[tickets] ([ticket_id])
    ON DELETE CASCADE
GO

-- 客服不可隨意刪除（留言要留著當稽核紀錄），所以這條不設 CASCADE
ALTER TABLE [dbo].[ticket_comments]  ADD CONSTRAINT [FK_ticket_comments_agents]
    FOREIGN KEY ([agent_id]) REFERENCES [dbo].[agents] ([agent_id])
GO

-- 工單詳情頁要撈整串 timeline，依時間排序
CREATE NONCLUSTERED INDEX [IX_ticket_comments_ticket]
    ON [dbo].[ticket_comments] ([ticket_id] ASC, [created_at] ASC)
GO

-- ---------------------------------------------------------------------
-- 4. 行事曆回電安排
-- ---------------------------------------------------------------------

CREATE TABLE [dbo].[follow_ups](
    [follow_up_id] INT IDENTITY(1,1) NOT NULL,  -- 流水號，同時是對外露出的識別碼（單號不再唯一指向一筆安排）
    [agent_id]     NVARCHAR(10)  NOT NULL,      -- 這筆安排的主人，也就是「誰的行事曆」
    [ticket_id]    INT           NOT NULL,      -- 要跟進的工單（內部 id，不是 TK-000001）
    [follow_up_at] DATETIME2(0)  NOT NULL,      -- 排定的回電時間，精度到秒
    [note]         NVARCHAR(200)     NULL,      -- 個人備註，只有主人看得到
    CONSTRAINT [PK_follow_ups] PRIMARY KEY CLUSTERED ([follow_up_id] ASC)
)
GO

ALTER TABLE [dbo].[follow_ups]  ADD CONSTRAINT [FK_follow_ups_tickets]
    FOREIGN KEY ([ticket_id]) REFERENCES [dbo].[tickets] ([ticket_id])
    ON DELETE CASCADE
GO


ALTER TABLE [dbo].[follow_ups] WITH CHECK ADD CONSTRAINT [FK_follow_ups_agents]
    FOREIGN KEY ([agent_id]) REFERENCES [dbo].[agents] ([agent_id])
GO


ALTER TABLE [dbo].[follow_ups] ADD CONSTRAINT [UQ_follow_ups_agent_ticket_time]
    UNIQUE NONCLUSTERED ([agent_id] ASC, [ticket_id] ASC, [follow_up_at] ASC)
GO

CREATE NONCLUSTERED INDEX [IX_follow_ups_agent_time]
    ON [dbo].[follow_ups] ([agent_id] ASC, [follow_up_at] ASC)
GO

-- ---------------------------------------------------------------------
-- 5. 種子資料：三個開發用客服帳號
-- ---------------------------------------------------------------------
--
-- 【開發用密碼：pass1234】三個帳號都一樣。
--   password_hash 是用 BCryptPasswordEncoder（strength 10）算出來的，
--   BCrypt 每次加鹽都不同，所以三行的雜湊值長得不一樣是正常的。
--
-- ⚠️ 這是開發／demo 用的假帳號，正式環境請務必改密碼或拿掉這一節。
--
-- status 不指定，讓它吃 DF_agents_status 的預設值 ONLINE。
INSERT INTO [dbo].[agents] ([agent_id], [name], [password_hash]) VALUES
    (N'CSC00001', N'林曉明', N'$2a$10$jAbfr7dQChMTXY9PMHZIT.r4qNz1ye8FB7xdEbHmTJHDczGYI4W.e'),
    (N'CSC00002', N'陳美芳', N'$2a$10$WQLaWHKTPR69yqT8S9SgkOHyuLu49ezfXNYwlDUQZWcZQcSo5FnOq'),
    (N'CSC00003', N'黃志豪', N'$2a$10$gQ5/bFNZ0vNwCl956c6dne8UvPdJqx8KSVoRPYmJrmSjbTrNf2nZu')
GO
