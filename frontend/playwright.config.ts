import { defineConfig, devices } from '@playwright/test';

// Browser-level test suite (real Chromium, not jsdom) covering the states in
// the approved design (agent/ui/financial-position/design-brief.md) via
// mocked API routes. e2e/real-backend-smoke.spec.ts is a separate real-backend
// counterpart run under playwright.smoke.config.ts by
// e2e/real-backend-smoke.sh, not this config (see README.md "Frontend E2E
// tests"). Not part of the required `verify` CI gate — see README.md for why
// and how to run this suite locally.
export default defineConfig({
  testDir: './e2e',
  // real-backend-smoke.spec.ts targets the isolated stack real-backend-smoke.sh
  // starts (see playwright.smoke.config.ts), not this config's own mocked
  // webServer — exclude it here so `npm run test:e2e` doesn't fail collecting
  // it for lack of SMOKE_* env vars.
  testIgnore: '**/real-backend-smoke.spec.ts',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:4173',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: {
    command: 'npm run preview -- --port 4173',
    url: 'http://localhost:4173',
    reuseExistingServer: !process.env.CI,
    timeout: 30_000,
  },
});
