# JWT / Spring Security 完整流程圖

> 圖都用純文字畫，不需要安裝任何外掛。
> 圖上的 `[編號]` 代表先後順序，`檔名:行號` 代表程式碼位置，`──▶` 代表「呼叫 / 交給」。

---

## 總覽：三個階段

```
 階段 0：編譯時 ──────▶ 階段 1：啟動時（只跑一次）──────▶ 階段 2：每個請求（跑很多次）
 確認型別正確              把 Filter 清單組好、接好線              照清單一關一關執行
 （圖一）                  （圖二）                                （圖三、圖四）
```

---

## 圖一：編譯時

```
[0-1] JwtAuthFilter.java:42
      public class JwtAuthFilter extends OncePerRequestFilter
        │
        │ 繼承關係：
        │   JwtAuthFilter
        │     └▶ OncePerRequestFilter
        │          └▶ GenericFilterBean
        │               └▶ implements jakarta.servlet.Filter
        ▼
[0-2] SecurityConfig.java:81
      addFilterBefore(new JwtAuthFilter(jwtService), ...)
        │
        │ addFilterBefore 的參數型別規定是 Filter
        │ JwtAuthFilter 是 Filter → 編譯通過
        │ （如果傳入的不是 Filter → 編譯失敗，程式根本跑不起來）
        ▼
      進入階段 1
```

---

## 圖二：啟動時（按下 Run，只跑一次）

```
[1-1] pom.xml:72  spring-boot-starter-security
        │
        │ Spring Boot 看到這個套件 → 自動設定
        ▼
[1-2] SecurityFilterAutoConfiguration（Spring Boot 內建，不在你的專案裡）
        │
        │ 在 Tomcat 門口登記一個 Filter
        │   名稱：springSecurityFilterChain
        │   範圍：/*（所有網址）
        │   作用：有請求進來 → 轉交給 FilterChainProxy（在 [1-8] 準備好）
        ▼
[1-3] application.properties:48-49
      jwt.secret / jwt.expiration-ms
        │
        │ 綁定設定值（因為 CsDemoApplication 有 @ConfigurationPropertiesScan）
        ▼
[1-4] JwtProperties
        │ 檢查：secret ≥ 32 bytes、expiration-ms > 0
        │ 不符合 → 啟動失敗
        ▼
[1-5] JwtService(JwtProperties)
        │ JwtService.java:34  把 secret 轉成簽章用的 key
        ▼
[1-6] SecurityConfig（建構子注入 JwtService）
        │
        │ SecurityConfig.java:49  @Bean
        │ Spring 自動呼叫 securityFilterChain(HttpSecurity http)
        │ （http 是 Spring 準備好、自動傳進來的設定工具）
        │
        │   .csrf(disable)                   → 清單裡不放 CsrfFilter
        │   .sessionManagement(STATELESS)    → 不使用 Session
        │   .formLogin(disable)              → 不放 UsernamePasswordAuthenticationFilter
        │   .httpBasic(disable)              → 不放 BasicAuthenticationFilter
        │   .exceptionHandling(HttpStatusEntryPoint(401))
        │                                    → 第 11 關擋人時交給 Spring 內建的 HttpStatusEntryPoint
        │   .authorizeHttpRequests(規則)      → 第 12 關用的規則
        │   .addFilterBefore(new JwtAuthFilter(jwtService), ...)
        │                                    → JwtAuthFilter 被 new 出來、排進清單
        │                                      （只是排進去，還沒執行）
        ▼
[1-7] SecurityConfig.java:83  return http.build();
        │ 照上面的設定，組出 SecurityFilterChain：
        │
        │    1. DisableEncodeUrlFilter
        │    2. WebAsyncManagerIntegrationFilter
        │    3. SecurityContextHolderFilter
        │    4. HeaderWriterFilter
        │    5. LogoutFilter
        │    6. JwtAuthFilter                ← 你的
        │    7. RequestCacheAwareFilter
        │    8. SecurityContextHolderAwareRequestFilter
        │    9. AnonymousAuthenticationFilter
        │   10. SessionManagementFilter
        │   11. ExceptionTranslationFilter
        │   12. AuthorizationFilter
        │
        │ （這份清單是實際開 DEBUG log 印出來的）
        ▼
[1-8] FilterChainProxy（Spring Security 的總入口，在 spring-security-web jar 裡）
        │ 依「型別」收走所有 SecurityFilterChain Bean
        │ → [1-2] 門口的 Filter 就是把請求轉交到這裡
        ▼
[1-9] Tomcat 開始監聽 8080 port
      log：Started CsDemoApplication ...
        │
        ▼
      之後 securityFilterChain() 不會再被呼叫，進入階段 2
```

> 說明：[1-1]～[1-8] 是依「誰需要誰」排列的。Spring 內部實際建立物件的先後順序，可能跟圖上不完全相同，但依賴關係是對的。

---

## 圖三：一個請求走過全部關卡（核心）

以 `GET /api/auth/me` 為例：

```
[2-1] 前端 api.js
        │ api.js:37  const token = getToken();       ← 從 localStorage 取出 token
        │ api.js:38  headers['Authorization'] = 'Bearer ' + token;
        │ fetch('/api/auth/me', { method, headers })
        │
        │ 瀏覽器送出：
        │   GET /api/auth/me
        │   Authorization: Bearer eyJhbGci...
        ▼
[2-2] Tomcat 收到，包成 HttpServletRequest request
        │ 門口有 springSecurityFilterChain（[1-2] 登記的）
        ▼
[2-3] FilterChainProxy
        │ 「這個請求用哪條清單？」→ 你的 SecurityFilterChain（適用所有請求）
        ▼
[2-4] 第 1～5 關（Spring 內建）
        │ 第 3 關 SecurityContextHolderFilter：準備一個空的 SecurityContext
        ▼
[2-5] 第 6 關 JwtAuthFilter.doFilterInternal(request, response, filterChain)
        │
        ├─[a] JwtAuthFilter.java:69-70  extractToken(request)
        │      request.getHeader("Authorization")
        │      │
        │      ├─ 沒有 header，或開頭不是 "Bearer " → token = null → 直接跳到 [d]
        │      └─ 有 → 去掉 "Bearer "，得到 token → 往下到 [b]
        │
        ├─[b] JwtAuthFilter.java:55  jwtService.parseAgentId(token)
        │      JwtService.java:66  用 key 驗證簽章、檢查過期
        │      │
        │      ├─ 成功 → agentId = "CSC00001" → 往下到 [c]
        │      └─ 失敗（竄改 / 過期 / 別的 secret）→ 丟 JwtException
        │           JwtAuthFilter.java:57  catch → 只記 log、不貼名牌 → 跳到 [d]
        │
        ├─[c] JwtAuthFilter.java:78  setAuthenticated(agentId, request)
        │      建立 Authentication（principal=CSC00001, ROLE_AGENT）
        │      → SecurityContextHolder.setContext(...)   ← 貼上名牌
        │
        ▼
      [d] JwtAuthFilter.java:64  filterChain.doFilter(request, response)
          不管成功或失敗，一律放行到下一關（Filter 只辨識身分、不擋人）
        │
        ▼
[2-6] 第 7～8 關（Spring 內建）
        ▼
[2-7] 第 9 關 AnonymousAuthenticationFilter
        │ 置物櫃裡有名牌？
        │   有 → 不動
        │   沒有 → 貼上 anonymousUser（匿名身分）
        ▼
[2-8] 第 10 關 SessionManagementFilter
        ▼
[2-9] 第 11 關 ExceptionTranslationFilter
        │ 先待命：如果第 12 關丟出「拒絕存取」，由它接手 → 跳到 [2-11]
        ▼
[2-10] 第 12 關 AuthorizationFilter（照 SecurityConfig.java:72-75 的規則，由上往下比對）
        │
        ├─ POST /api/auth/login → permitAll → 放行 → 到 [2-12]
        ├─ PUBLIC_PATHS → permitAll → 放行 → 到 [2-12]
        └─ 其他路徑 → authenticated()
              │
              ├─ 有真正的身分（CSC00001）→ 放行 → 到 [2-12]
              └─ 只有 anonymousUser → 丟出「拒絕存取」→ 到 [2-11]

[2-11] 擋下 → 回 401
        │ ExceptionTranslationFilter 接到 → 判斷是「還沒登入」
        ▼
        HttpStatusEntryPoint（Spring 內建，SecurityConfig.java:69 設定）
        │ 寫入 response：只有狀態碼 401，body 是空的
        ▼
        回到前端 api.js:63-65
          status === 401 且原本有帶 token → clearSession() → 跳回 #/login
        （結束，不會到 Controller）

[2-12] DispatcherServlet → Controller
        │ AuthController.java:31  @GetMapping("/me") → authService.me()
        ▼
[2-13] Service 需要知道「現在是誰」
        │ CurrentAgent.java:29  currentAgentId()
        │   → SecurityContextHolder.getContext().getAuthentication()   ← 讀 [2-5c] 貼的名牌
        │   → CurrentAgent.java:39  auth.getName() → "CSC00001"
        ▼
[2-14] 查資料庫、組回應 → 回到前端
        │
        ▼
      請求結束，Spring Security 清空這個請求的 SecurityContext
```

---

## 圖四：從打開網站到登入後，實際的請求順序

每一個請求都會完整走一次圖三。

```
[A] 使用者打開 http://localhost:8080/
      │
      ├─ GET /              ─┐
      ├─ GET /css/app.css    │ 沒 token → [2-5] 不做事
      ├─ GET /js/api.js      │ → [2-10] 在 PUBLIC_PATHS → permitAll → 放行
      └─ GET /favicon.svg   ─┘ → 回傳檔案
      ▼
[B] 使用者輸入帳密，按登入
      │ login.js:49  api.login(agentId, password)
      │ POST /api/auth/login  （沒有 Authorization header）
      │
      │ [2-5] 沒 token → 不做事
      │ [2-10] POST /api/auth/login → permitAll → 放行
      ▼
      AuthController.java:19 → AuthService.java:45  login()
      │ AuthService.java:49  passwordEncoder.matches(明文, 資料庫雜湊)   ← PasswordConfig 提供 BCrypt
      │ AuthService.java:55  jwtService.generateToken("CSC00001")
      │                      JwtService.java:44  用 key 簽名，設定 8 小時後過期
      ▼
      回傳 {"agentId":"CSC00001","token":"eyJ..."}
      │
      │ login.js:50  setSession({ agentId, token })
      │ state.js:22  存進 localStorage
      ▼
[C] 登入後馬上取個人資料
      │ login.js:53  api.me()
      │ GET /api/auth/me  + Authorization: Bearer eyJ...    ← api.js:38 自動加上
      │
      │ 走完整的圖三：[2-5] 驗證成功、貼名牌 → [2-10] 放行 → [2-13] CurrentAgent 取得 CSC00001
      ▼
[D] 之後所有 API（查工單、建立追蹤…）都跟 [C] 一樣
      │
      ▼
[E] 8 小時後 token 過期
      │ 任何 API → [2-5b] 解析失敗（過期）→ 不貼名牌
      │ → [2-10] 沒身分 → [2-11] 401 → 前端清掉 session，跳回登入頁
      ▼
      回到 [B]
```

---

## 圖五：檔案關聯圖（誰用到誰、什麼時候）

```
【啟動時建立的關聯】
application.properties
  └─▶ JwtProperties
        └─▶ JwtService
              └─▶ SecurityConfig（注入 JwtService）
                    ├─▶ new JwtAuthFilter(jwtService) → 排進 Filter 清單第 6 關
                    └─▶ new HttpStatusEntryPoint(401) → 交給第 11 關使用

【登入請求時】
AuthService.login()
  ├─▶ PasswordConfig 的 BCrypt：matches() 比對密碼
  └─▶ JwtService.generateToken()：產生 token

【每個請求時】
JwtAuthFilter（第 6 關）
  ├─▶ JwtService.parseAgentId()：驗證 token
  └─▶ 成功 → 寫入 SecurityContextHolder（置物櫃）
                     │
                     ├─▶ AuthorizationFilter（第 12 關）讀取：有身分才放行
                     │       └─ 沒身分 → ExceptionTranslationFilter（第 11 關）
                     │                    └─▶ HttpStatusEntryPoint：回 401（空 body）
                     │
                     └─▶ CurrentAgent.currentAgentId() 讀取：getName()
                             ▲
                             └─ AuthService、AgentService、TicketService、
                                FollowUpService、TicketDetailService 呼叫
```

| 檔案 | 什麼時候被用到 | 被誰用 |
|---|---|---|
| `JwtProperties` | 啟動時 | `JwtService` |
| `JwtService` | 啟動時建立；登入時 `generateToken`；每個請求 `parseAgentId` | `SecurityConfig`、`JwtAuthFilter`、`AuthService` |
| `SecurityConfig` | 啟動時，只跑一次 | Spring（因為 `@Bean`） |
| `JwtAuthFilter` | 啟動時被 `new`；每個請求執行 `doFilterInternal` | `SecurityConfig` 登記，框架呼叫 |
| `CurrentAgent` | Service 需要知道「現在是誰」時 | 各個 Service |
| `PasswordConfig` | 登入、註冊、改密碼時 | `AuthService`、`AgentService` |
