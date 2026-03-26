import { spawn } from 'node:child_process';
import { readFile, readdir, rm, stat } from 'node:fs/promises';
import path from 'node:path';
import Redis from 'ioredis';

const WAIT_TARGETS = [
  process.env.TARGET_API_URL,
  process.env.PLAYWRIGHT_BASE_URL,
].filter(Boolean);

const QA_TARGET = process.env.QA_TARGET;
const PLAYWRIGHT_SPEC = process.env.PLAYWRIGHT_SPEC;
const REDIS_HOST = process.env.REDIS_HOST;

function getPlaywrightArtifactsPath() {
  if (!QA_TARGET) {
    throw new Error('QA_TARGET is required');
  }

  return path.join('test-results', QA_TARGET, '.playwright-artifacts');
}

function getReadableResultsPath() {
  if (!QA_TARGET) {
    throw new Error('QA_TARGET is required');
  }

  return path.join('test-results', QA_TARGET);
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForHttpTarget(target) {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    try {
      const response = await fetch(target);
      if (response.ok) {
        return;
      }
    } catch {
      // Retry until the timeout budget is exhausted.
    }

    await sleep(1_000);
  }

  throw new Error(`Timed out waiting for ${target}`);
}

async function waitForTargets() {
  for (const target of WAIT_TARGETS) {
    await waitForHttpTarget(target);
  }
}

async function flushRedisIfNeeded() {
  if (!REDIS_HOST || REDIS_HOST === 'none') {
    return;
  }

  const redis = new Redis({
    host: REDIS_HOST,
    lazyConnect: true,
    maxRetriesPerRequest: 1,
  });

  try {
    await redis.connect();
    await redis.flushall();
  } finally {
    redis.disconnect();
  }
}

function runPlaywright() {
  if (!PLAYWRIGHT_SPEC) {
    throw new Error('PLAYWRIGHT_SPEC is required');
  }

  return new Promise((resolve, reject) => {
    const child = spawn(
      'npx',
      ['cross-env', 'PLAYWRIGHT_BROWSERS_PATH=/ms-playwright', 'playwright', 'test', PLAYWRIGHT_SPEC],
      {
        stdio: 'inherit',
        shell: true,
        env: process.env,
      },
    );

    child.on('error', reject);
    child.on('exit', (code) => {
      resolve(code ?? 1);
    });
  });
}

async function loadFailureSummaries() {
  const readableResultsPath = getReadableResultsPath();
  const entries = await readdir(readableResultsPath, { withFileTypes: true }).catch(() => []);
  const failures = [];

  for (const entry of entries) {
    if (!entry.isDirectory() || !entry.name.startsWith('tb-')) {
      continue;
    }

    const failurePath = path.join(readableResultsPath, entry.name, 'failure.json');
    const exists = await stat(failurePath).then(() => true).catch(() => false);
    if (!exists) {
      continue;
    }

    const failure = JSON.parse(await readFile(failurePath, 'utf8'));
    failures.push({
      title: failure['제목'] ?? entry.name,
      failureType: failure['실패유형'] ?? failure.failureType ?? 'unknown',
      summary: failure['요약'] ?? failure['QA비고'] ?? '',
      errorMessage: failure['오류']?.['메시지'] ?? failure['오류메시지'] ?? '',
      path: failurePath,
    });
  }

  return failures;
}

function printFailureSummaries(failures) {
  if (failures.length === 0) {
    return;
  }

  console.error(`QA failed for ${failures.length} test case(s):`);
  for (const failure of failures) {
    console.error(`- ${failure.title}`);
    console.error(`  type: ${failure.failureType}`);
    if (failure.summary) {
      console.error(`  summary: ${failure.summary}`);
    }
    if (failure.errorMessage) {
      console.error(`  error: ${failure.errorMessage}`);
    }
    console.error(`  artifact: ${failure.path}`);
  }
}

async function cleanupPlaywrightArtifacts() {
  await rm(getPlaywrightArtifactsPath(), { recursive: true, force: true });
}

async function cleanupReadableResults() {
  await rm(getReadableResultsPath(), { recursive: true, force: true });
}

async function main() {
  await waitForTargets();
  await flushRedisIfNeeded();

  await cleanupReadableResults();

  try {
    const playwrightExitCode = await runPlaywright();
    const failures = await loadFailureSummaries();

    if (failures.length > 0) {
      printFailureSummaries(failures);
    }

    if (playwrightExitCode !== 0) {
      throw new Error(`Playwright exited with code ${playwrightExitCode}`);
    }

    if (failures.length > 0) {
      throw new Error('QA verdict contains FAIL entries.');
    }
  } finally {
    await cleanupPlaywrightArtifacts();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
