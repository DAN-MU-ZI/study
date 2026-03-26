import { Reporter, TestCase, TestResult } from '@playwright/test/reporter';
import * as fs from 'fs';
import * as path from 'path';

const qaTarget = process.env.QA_TARGET ?? '01_advanced';

function getTestCaseId(title: string): string {
  const separatorIndex = title.indexOf(':');
  if (separatorIndex === -1) {
    return title.trim();
  }

  return title.slice(0, separatorIndex).trim();
}

function isBaselineQaFailure(test: TestCase): boolean {
  if (qaTarget !== '00_baseline') {
    return false;
  }

  const baselineFailureCases = new Set(['TB-002', 'TB-003']);
  return baselineFailureCases.has(getTestCaseId(test.title));
}

function getQaVerdict(test: TestCase, result: TestResult): 'PASS' | 'FAIL' {
  if (result.status !== 'passed') {
    return 'FAIL';
  }

  return isBaselineQaFailure(test) ? 'FAIL' : 'PASS';
}

function getQaNote(test: TestCase, result: TestResult): string {
  if (result.status !== 'passed') {
    return '자동화 실행 자체가 실패했다.';
  }

  if (isBaselineQaFailure(test)) {
    return '취약점은 재현됐지만 advanced 기준 안정성 요구사항은 충족하지 못했다.';
  }

  if (qaTarget === '00_baseline') {
    return '이 케이스 자체는 advanced 기준 안정성 요구사항과 직접 충돌하지 않는다.';
  }

  return 'advanced 기준 안정성 요구사항으로 기대 결과를 충족했다.';
}

export default class CsvReporter implements Reporter {
  private file!: fs.WriteStream;
  private filePath!: string;

  constructor(options: { outputFile?: string } = {}) {
    this.filePath = options.outputFile || 'test-results/results.csv';
  }

  onBegin() {
    const dir = path.dirname(this.filePath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }

    this.file = fs.createWriteStream(this.filePath, { encoding: 'utf8' });
    this.file.write('\uFEFF테스트제목,실행상태,QA판정,QA비고,실행시간(ms),오류\n');
  }

  onTestEnd(test: TestCase, result: TestResult) {
    const title = test.title.replace(/"/g, '""');
    const executionStatus = result.status;
    const qaVerdict = getQaVerdict(test, result);
    const qaNote = getQaNote(test, result).replace(/"/g, '""');
    const duration = result.duration;
    const errorMsg = result.error?.message?.replace(/\n/g, ' ').replace(/"/g, '""') || '';

    const row = `"${title}","${executionStatus}","${qaVerdict}","${qaNote}","${duration}","${errorMsg}"\n`;
    this.file.write(row);
  }

  onEnd() {
    this.file.end();
  }
}
