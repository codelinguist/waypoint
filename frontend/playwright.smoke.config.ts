import { defineConfig, devices } from '@playwright/test';

// Config for e2e/real-backend-smoke.spec.ts only. Unlike playwright.config.ts
// (which mocks the API against a Vite preview server it starts itself), this
// targets the isolated, disposable stack real-backend-smoke.sh already built
// and started — no webServer of its own, and baseURL comes from the
// SMOKE_BASE_URL that script sets. See README.md "Frontend E2E tests".
const baseURL = process.env.SMOKE_BASE_URL;
if (!baseURL) {
  throw new Error('playwright.smoke.config.ts requires SMOKE_BASE_URL (set by e2e/real-backend-smoke.sh)');
}

export default defineConfig({
  testDir: './e2e',
  testMatch: 'real-backend-smoke.spec.ts',
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL,
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
