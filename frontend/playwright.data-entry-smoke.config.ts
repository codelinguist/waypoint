import { defineConfig, devices } from '@playwright/test';

// Config for e2e/data-entry-smoke.spec.ts only. Same shape as
// playwright.smoke.config.ts (see that file's comment) — targets the
// isolated, disposable stack e2e/real-backend-data-entry-smoke.sh already
// built and started, with no webServer of its own.
const baseURL = process.env.SMOKE_BASE_URL;
if (!baseURL) {
  throw new Error('playwright.data-entry-smoke.config.ts requires SMOKE_BASE_URL (set by e2e/real-backend-data-entry-smoke.sh)');
}

export default defineConfig({
  testDir: './e2e',
  testMatch: 'data-entry-smoke.spec.ts',
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL,
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
