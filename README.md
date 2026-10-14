# CS_Demo

客服工單系統。客服接聽電話時可以建立工單、查詢歷史紀錄、轉派給其他客服，並在行事曆上安排回電。

## 功能

- JWT 登入，客服工作狀態切換（上線、休息、通話中……）
- 工單建立、篩選查詢、狀態管理、轉派
- 通話中自動顯示同一支電話過去的工單
- 工單處理記錄時間軸
- 回電行事曆

## 技術

- **後端**：Java 21、Spring Boot 4、Spring Security、JPA、Flyway
- **前端**：React 19、Vite
- **資料庫**：SQL Server 2022（Docker）

## 快速開始

```bash
# 1. 啟動資料庫
docker compose up -d

# 2. 啟動後端（第一次啟動會自動建表並寫入 demo 資料）
./mvnw spring-boot:run
```

打開 <http://localhost:8080>，API 文件在 <http://localhost:8080/swagger-ui.html>。

### Demo 帳號

`CSC00001` ～ `CSC00005`，密碼都是 `pass1234`。