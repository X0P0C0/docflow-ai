import { defineConfig } from "vitest/config";
import { resolve } from "path";

export default defineConfig({
  test: {
    environment: "happy-dom",
    globals: true,
    include: ["src/**/*.test.{ts,tsx}"],
    exclude: ["node_modules", "dist", "src/**/*.vue"]
  },
  resolve: {
    alias: {
      "@": resolve(__dirname, "src")
    }
  }
});
