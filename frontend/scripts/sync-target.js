// ============================================================
// build 完之後，把 static 同步複製到 target/classes/static。
//
// 為什麼需要：IntelliJ 執行 Spring Boot 時，讀的是 target/classes 裡的檔案，
// 不是 src/main/resources。只更新 src 的話，8080 會一直拿到舊版前端，
// 要 Rebuild Project 才會換。同步過去之後，重新整理瀏覽器就能看到新版。
// ============================================================
import { cpSync, existsSync, rmSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const src = fileURLToPath(new URL('../../src/main/resources/static', import.meta.url));
const classes = fileURLToPath(new URL('../../target/classes', import.meta.url));
const dest = `${classes}/static`;

if (!existsSync(classes)) {
  console.log('[sync-target] 還沒有 target/classes（後端沒編譯過），略過');
} else {
  rmSync(dest, { recursive: true, force: true }); // 先刪掉，避免舊的 js 檔殘留
  cpSync(src, dest, { recursive: true });
  console.log('[sync-target] 已同步到 target/classes/static');
}
