/**
 * ULID (Universally Unique Lexicographically Sortable Identifier) 생성기.
 *
 * 구조: 48-bit timestamp (ms) + 80-bit randomness = 128-bit
 * 인코딩: Crockford's Base32 → 26자 문자열
 *
 * - 시간순 정렬 가능 (앞 10자가 타임스탬프)
 * - 클라이언트 측 상태 관리 불필요 (시스템 시계 + 난수만 사용)
 * - UUID v4 대비 하이픈 없이 깔끔, 동일 128비트 엔트로피
 */

const CROCKFORD_BASE32 = '0123456789ABCDEFGHJKMNPQRSTVWXYZ';

/**
 * 밀리초 타임스탬프(48비트)를 Crockford Base32 10자로 인코딩한다.
 */
function encodeTime(now: number): string {
  let remaining = now;
  const chars: string[] = new Array(10);

  for (let i = 9; i >= 0; i--) {
    chars[i] = CROCKFORD_BASE32[remaining % 32];
    remaining = Math.floor(remaining / 32);
  }

  return chars.join('');
}

/**
 * 80비트 랜덤을 Crockford Base32 16자로 인코딩한다.
 */
function encodeRandom(): string {
  const chars: string[] = new Array(16);
  const randomBytes = new Uint8Array(16);
  globalThis.crypto.getRandomValues(randomBytes);

  for (let i = 0; i < 16; i++) {
    chars[i] = CROCKFORD_BASE32[randomBytes[i] % 32];
  }

  return chars.join('');
}

/**
 * ULID를 생성한다.
 *
 * @returns 26자 Crockford Base32 문자열 (예: 01HZXK5P3RQJC8V7DWMN4T6EYS)
 */
export function ulid(): string {
  return encodeTime(Date.now()) + encodeRandom();
}
