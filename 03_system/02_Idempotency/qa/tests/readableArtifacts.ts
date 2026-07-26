import { existsSync, readFileSync } from 'node:fs';
import { copyFile, mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import type { Page, TestInfo } from '@playwright/test';
import type { QaDebugSnapshot } from './fixtures';

const qaTarget = process.env.QA_TARGET ?? '01_advanced';

function stripBom(content: string): string {
  return content.charCodeAt(0) === 0xfeff ? content.slice(1) : content;
}

function parseFirstTwoCsvFields(line: string): [string, string] | null {
  const fields: string[] = [];
  let current = '';
  let inQuotes = false;

  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];

    if (char === '"') {
      const nextChar = line[index + 1];
      if (inQuotes && nextChar === '"') {
        current += '"';
        index += 1;
        continue;
      }

      inQuotes = !inQuotes;
      continue;
    }

    if (char === ',' && !inQuotes) {
      fields.push(current.trim());
      current = '';
      if (fields.length === 2) {
        return [fields[0], fields[1]];
      }
      continue;
    }

    if (char !== '\r') {
      current += char;
    }
  }

  fields.push(current.trim());
  if (fields.length < 2) {
    return null;
  }

  return [fields[0], fields[1]];
}

function resolveTestCasesPath(): string {
  const candidates = [
    path.resolve(process.cwd(), 'test-cases.csv'),
    path.resolve(process.cwd(), '../test-cases.csv'),
  ];

  for (const candidate of candidates) {
    if (existsSync(candidate)) {
      return candidate;
    }
  }

  throw new Error(`테스트 케이스 CSV를 찾을 수 없다. 확인한 경로: ${candidates.join(', ')}`);
}

function loadTestCaseNames(): Map<string, string> {
  const content = stripBom(readFileSync(resolveTestCasesPath(), 'utf8'));
  const lines = content.split('\n');
  const cases = new Map<string, string>();
  let headerSkipped = false;

  for (const line of lines) {
    if (!line.trim()) {
      continue;
    }

    const parsed = parseFirstTwoCsvFields(line);
    if (!parsed) {
      continue;
    }

    if (!headerSkipped) {
      headerSkipped = true;
      continue;
    }

    const [id, name] = parsed;
    cases.set(id, name);
  }

  return cases;
}

function isAsciiLetterOrDigit(char: string): boolean {
  const code = char.charCodeAt(0);
  return (code >= 48 && code <= 57) || (code >= 97 && code <= 122);
}

function isHangul(char: string): boolean {
  const codePoint = char.codePointAt(0) ?? 0;
  return (
    (codePoint >= 0x1100 && codePoint <= 0x11ff) ||
    (codePoint >= 0x3130 && codePoint <= 0x318f) ||
    (codePoint >= 0xac00 && codePoint <= 0xd7a3)
  );
}

function isDelimiter(char: string): boolean {
  return (
    char === ' ' ||
    char === '\t' ||
    char === '\n' ||
    char === '\r' ||
    char === '-' ||
    char === '_' ||
    char === ':' ||
    char === '/' ||
    char === '\\' ||
    char === ',' ||
    char === '.' ||
    char === '(' ||
    char === ')' ||
    char === '[' ||
    char === ']' ||
    char === '{' ||
    char === '}' ||
    char === '<' ||
    char === '>' ||
    char === '`' ||
    char === '\'' ||
    char === '"' ||
    char === '|' ||
    char === '&' ||
    char === '!' ||
    char === '?' ||
    char === '+' ||
    char === '=' ||
    char === '~' ||
    char === '@' ||
    char === '#' ||
    char === '$' ||
    char === '%' ||
    char === '^' ||
    char === '*' ||
    char === ';'
  );
}

function toSlugPart(value: string): string {
  let slug = '';
  let previousWasDash = false;

  for (const originalChar of value.trim().toLowerCase()) {
    if (isAsciiLetterOrDigit(originalChar) || isHangul(originalChar)) {
      slug += originalChar;
      previousWasDash = false;
      continue;
    }

    if (isDelimiter(originalChar) && slug && !previousWasDash) {
      slug += '-';
      previousWasDash = true;
    }
  }

  if (slug.endsWith('-')) {
    slug = slug.slice(0, -1);
  }

  return slug.slice(0, 80);
}

function getTestCaseId(title: string): string {
  const separatorIndex = title.indexOf(':');
  if (separatorIndex === -1) {
    return title.trim();
  }

  return title.slice(0, separatorIndex).trim();
}

function getTestCaseTitleBody(title: string): string {
  const separatorIndex = title.indexOf(':');
  if (separatorIndex === -1) {
    return title.trim();
  }

  return title.slice(separatorIndex + 1).trim();
}

const testCaseNames = loadTestCaseNames();

function getReadableArtifactName(title: string): string {
  const testCaseId = getTestCaseId(title);
  const testCaseName = testCaseNames.get(testCaseId) ?? getTestCaseTitleBody(title);
  const idPart = toSlugPart(testCaseId);
  const namePart = toSlugPart(testCaseName);

  return namePart ? `${idPart}-${namePart}` : idPart;
}

function getQaVerdict(title: string, executionStatus: TestInfo['status']) {
  if (executionStatus !== 'passed') {
    return {
      qaVerdict: 'FAIL',
      qaNote: '자동화 실행 자체가 실패했다.',
    };
  }

  if (qaTarget !== '00_baseline') {
    return {
      qaVerdict: 'PASS',
      qaNote: 'advanced 기준 안정성 요구사항으로 기대 결과를 충족했다.',
    };
  }

  const baselineFailureCases = new Set(['TB-002', 'TB-003']);
  if (baselineFailureCases.has(getTestCaseId(title))) {
    return {
      qaVerdict: 'FAIL',
      qaNote: '취약점은 재현됐지만 advanced 기준 안정성 요구사항은 충족하지 못했다.',
    };
  }

  return {
    qaVerdict: 'PASS',
    qaNote: '이 케이스 자체는 advanced 기준 안정성 요구사항과 직접 충돌하지 않는다.',
  };
}

function getReadableArtifactRoot(testInfo: Pick<TestInfo, 'outputDir'>): string {
  const rawRoot = path.dirname(testInfo.outputDir);
  return path.basename(rawRoot).startsWith('.playwright') ? path.dirname(rawRoot) : rawRoot;
}

export function getReadableArtifactDir(testInfo: Pick<TestInfo, 'outputDir' | 'title'>): string {
  return path.join(getReadableArtifactRoot(testInfo), getReadableArtifactName(testInfo.title));
}

function getFailureType(
  executionStatus: TestInfo['status'],
  qaVerdict: 'PASS' | 'FAIL',
): 'execution-failure' | 'qa-verdict-failure' | null {
  if (executionStatus !== 'passed') {
    return 'execution-failure';
  }

  if (qaVerdict === 'FAIL') {
    return 'qa-verdict-failure';
  }

  return null;
}

type FailureType = NonNullable<ReturnType<typeof getFailureType>>;

function compactRecord<T extends Record<string, unknown>>(record: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(record).filter(([, value]) => {
      if (value === null || value === undefined) {
        return false;
      }

      if (typeof value === 'string') {
        return value.length > 0;
      }

      if (Array.isArray(value)) {
        return value.length > 0;
      }

      if (typeof value === 'object') {
        return Object.keys(value).length > 0;
      }

      return true;
    }),
  ) as Partial<T>;
}

async function safely<T>(page: Page, action: () => Promise<T>, fallback: T): Promise<T> {
  if (page.isClosed()) {
    return fallback;
  }

  try {
    return await action();
  } catch {
    return fallback;
  }
}

async function getTestIdText(page: Page, testId: string): Promise<string | null> {
  return safely(
    page,
    async () => {
      const text = await page.getByTestId(testId).first().textContent();
      return text?.trim() ?? null;
    },
    null,
  );
}

async function getTestIdCount(page: Page, testId: string): Promise<number | null> {
  return safely(page, async () => page.getByTestId(testId).count(), null);
}

async function isTestIdVisible(page: Page, testId: string): Promise<boolean | null> {
  return safely(page, async () => page.getByTestId(testId).first().isVisible(), null);
}

async function isTestIdEnabled(page: Page, testId: string): Promise<boolean | null> {
  return safely(page, async () => page.getByTestId(testId).first().isEnabled(), null);
}

async function getAllTestIdTexts(page: Page, testId: string): Promise<string[]> {
  return safely(
    page,
    async () =>
      (await page.getByTestId(testId).allInnerTexts())
        .map((text) => text.trim())
        .filter(Boolean),
    [],
  );
}

function getBaselineQaFailureSummary(title: string): string {
  switch (getTestCaseId(title)) {
    case 'TB-002':
      return '같은 주문에서 중복 승인 방지가 적용되지 않았다.';
    case 'TB-003':
      return '처리 중 상태에서 결제 버튼 비활성화가 적용되지 않았다.';
    default:
      return 'advanced 기준 안정성 요구사항을 충족하지 못했다.';
  }
}

async function buildTb002QaFailureDetails(page: Page) {
  const [requestState, paymentCount, duplicateWarningVisible, pgTransactionIds] = await Promise.all([
    getTestIdText(page, 'request-state'),
    getTestIdCount(page, 'payment-row'),
    isTestIdVisible(page, 'duplicate-warning'),
    getAllTestIdTexts(page, 'payment-row-pg-transaction-id'),
  ]);

  const observed = compactRecord({
    요청상태: requestState,
    결제이력수: paymentCount,
    고유PG거래수: pgTransactionIds.length > 0 ? new Set(pgTransactionIds).size : null,
    중복경고노출: duplicateWarningVisible,
  });

  return compactRecord({
    요약:
      paymentCount === 2 && duplicateWarningVisible === true
        ? '같은 주문에서 결제 이력 2건과 중복 경고가 함께 확인됐다.'
        : '같은 주문에서 단일 승인으로 수렴하지 않았다.',
    기대값: {
      결제이력수: 1,
      고유PG거래수: 1,
      중복경고노출: false,
    },
    관측값: observed,
  });
}

async function buildTb003QaFailureDetails(page: Page) {
  const [requestState, payButtonEnabled, newOrderButtonEnabled] = await Promise.all([
    getTestIdText(page, 'request-state'),
    isTestIdEnabled(page, 'pay-button'),
    isTestIdEnabled(page, 'new-order-button'),
  ]);

  const observed = compactRecord({
    요청상태: requestState,
    결제버튼활성화: payButtonEnabled,
    다음주문버튼활성화: newOrderButtonEnabled,
  });

  return compactRecord({
    요약:
      requestState?.includes('submitting') && payButtonEnabled === true
        ? '요청 상태가 submitting인데도 결제 버튼이 활성화되어 있었다.'
        : '처리 중 버튼 비활성화 조건이 충족되지 않았다.',
    기대값: {
      요청상태: 'submitting',
      결제버튼활성화: false,
      다음주문버튼활성화: false,
    },
    관측값: observed,
  });
}

async function buildQaVerdictFailureDetails(page: Page, title: string) {
  switch (getTestCaseId(title)) {
    case 'TB-002':
      return buildTb002QaFailureDetails(page);
    case 'TB-003':
      return buildTb003QaFailureDetails(page);
    default:
      return {
        요약: getBaselineQaFailureSummary(title),
      };
  }
}

function buildExecutionFailureDetails(testInfo: TestInfo, qaDebug: QaDebugSnapshot) {
  const primaryError = testInfo.error ?? testInfo.errors[0];

  return compactRecord({
    요약: '자동화 실행 자체가 실패했다.',
    오류: compactRecord({
      메시지: primaryError?.message ?? null,
      스택: primaryError?.stack ?? null,
    }),
    디버그: compactRecord({
      콘솔로그: qaDebug.consoleLogs.slice(-10),
      페이지오류: qaDebug.pageErrors.slice(-10),
      요청실패: qaDebug.requestFailures.slice(-10),
    }),
  });
}

async function buildFailureArtifact(
  page: Page,
  testInfo: TestInfo,
  qaDebug: QaDebugSnapshot,
  failureType: FailureType,
) {
  const details =
    failureType === 'execution-failure'
      ? buildExecutionFailureDetails(testInfo, qaDebug)
      : await buildQaVerdictFailureDetails(page, testInfo.title);

  return compactRecord({
    제목: testInfo.title,
    실패유형: failureType,
    ...details,
  });
}

export async function writeReadableArtifacts(
  page: Page,
  testInfo: TestInfo,
  qaDebug: QaDebugSnapshot,
): Promise<void> {
  const readableOutputDir = getReadableArtifactDir(testInfo);
  await mkdir(readableOutputDir, { recursive: true });
  const qaVerdict = getQaVerdict(testInfo.title, testInfo.status);
  const failureType = getFailureType(testInfo.status, qaVerdict.qaVerdict);

  const summary = JSON.stringify(
    {
      제목: testInfo.title,
      실행상태: testInfo.status,
      QA판정: qaVerdict.qaVerdict,
      실행시간ms: testInfo.duration,
    },
    null,
    2,
  );

  await writeFile(path.join(readableOutputDir, 'result.json'), summary, 'utf8');

  if (failureType) {
    const failureArtifact = await buildFailureArtifact(page, testInfo, qaDebug, failureType);
    const failureSummary = JSON.stringify(failureArtifact, null, 2);

    await writeFile(path.join(readableOutputDir, 'failure.json'), failureSummary, 'utf8');
  }

  const pageVideo = page.video();
  if (pageVideo) {
    await page.close({ runBeforeUnload: true }).catch(() => {});

    try {
      await pageVideo.saveAs(path.join(readableOutputDir, 'video.webm'));
    } catch {
      // 원본 Playwright 산출물은 숨김 디렉터리에 유지된다.
    }
  }

  for (const attachment of testInfo.attachments) {
    if (!attachment.path || attachment.name === 'video') {
      continue;
    }

    const extension = path.extname(attachment.path) || '.bin';
    const attachmentName = attachment.name === 'screenshot' ? 'screenshot' : attachment.name;
    const readableTargetPath = path.join(readableOutputDir, `${attachmentName}${extension}`);

    try {
      await copyFile(attachment.path, readableTargetPath);
    } catch {
      // 원본 Playwright 산출물은 숨김 디렉터리에 유지된다.
    }
  }
}
