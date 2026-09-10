import { defineConfig, devices } from '@playwright/test';

// Browser-level test suite (real Chromium, not jsdom) covering the states in
// the approved design (agent/ui/financial-position/design-brief.md) via
// mocked API routes, plus one smoke spec that talks to the real backend
// (see e2e/real-backend.smoke.spec.ts and README.md "Frontend E2E tests").
// Not part of the required `verify` CI gate — see README.md for why and how
// to run this suite locally.
export default defineConfig({
  testDir: './e2e',
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
