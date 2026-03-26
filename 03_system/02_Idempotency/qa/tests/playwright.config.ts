import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://frontend:4173';
const qaTarget = process.env.QA_TARGET ?? '01_advanced';

export default defineConfig({
  testDir: '.',
  outputDir: `test-results/${qaTarget}/.playwright-artifacts`,
  preserveOutput: 'always',
  timeout: 30_000,
  expect: {
    timeout: 5_000,
  },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['./csvReporter.ts', { outputFile: `test-results/${qaTarget}/qa-run-results.csv` }]
  ],
  use: {
    baseURL,
    trace: 'on-first-retry',
    video: 'on',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
