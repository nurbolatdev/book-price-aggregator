import { defineConfig, devices } from '@playwright/test'

// Manual suite against the running stack (docker compose up, demo profile). Never runs in CI.
export default defineConfig({
  testDir: './live',
  testMatch: '*.live.ts',
  outputDir: '../test-results/live',
  retries: 0,
  reporter: 'list',
  use: { baseURL: process.env.E2E_BASE_URL ?? 'http://localhost', trace: 'retain-on-failure' },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
