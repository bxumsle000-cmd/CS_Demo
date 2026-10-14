// ============================================================
// Vite 設定
//   npm run dev   → http://localhost:5173，/api 轉給 8080 的 Spring Boot
//   npm run build → 輸出到 Spring Boot 的 static 資料夾，啟動 8080 就能直接用
// ============================================================
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        // 檔案放進 js/、css/：SecurityConfig 的 PUBLIC_PATHS 已經放行這兩個路徑，後端不用改
        entryFileNames: 'js/[name]-[hash].js',
        chunkFileNames: 'js/[name]-[hash].js',
        assetFileNames: (info) =>
          (info.names?.[0] ?? '').endsWith('.css') ? 'css/[name]-[hash][extname]' : 'js/[name]-[hash][extname]',
      },
    },
  },
});
