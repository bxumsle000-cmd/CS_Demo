package com.poz.cs_demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI（springdoc）的設定。
 * <p>
 * 主要目的是讓 Swagger UI 右上角出現「Authorize」按鈕：
 * 先用 /api/auth/login 拿到 token，貼進去之後，
 * Swagger 之後送出的每個請求都會自動帶 {@code Authorization: Bearer <token>}，
 * 不必每個 API 手動填 header。
 */
@Configuration
public class OpenApiConfig {

    /** 在 OpenAPI 文件裡這組認證方式的名稱，components 與 security 兩邊要對得上 */
    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        // 描述「怎麼認證」：HTTP bearer，格式是 JWT（bearerFormat 只是給人看的提示）
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("CS_Demo 客服工單系統 API")
                        .description("先呼叫 POST /api/auth/login 取得 token，再點右上角 Authorize 貼上。")
                        .version("1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearerScheme))
                // 全域套用：所有 API 預設都標示為需要 bearerAuth（Swagger 會在每個端點顯示鎖頭圖示）
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
