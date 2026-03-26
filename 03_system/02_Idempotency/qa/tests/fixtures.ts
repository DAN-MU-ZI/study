import { expect, test as base, type APIResponse, type Locator, type Page, type Request } from '@playwright/test';

type ConsoleEntry = {
  type: string;
  text: string;
};

type PageErrorEntry = {
  message: string;
  stack: string | null;
};

type RequestFailureEntry = {
  method: string;
  url: string;
  errorText: string | null;
};

export type QaDebugSnapshot = {
  consoleLogs: ConsoleEntry[];
  pageErrors: PageErrorEntry[];
  requestFailures: RequestFailureEntry[];
};

type QaDebugCollector = {
  snapshot: () => QaDebugSnapshot;
};

function pushLimited<T>(items: T[], value: T, limit = 50): void {
  items.push(value);
  if (items.length > limit) {
    items.shift();
  }
}

export async function observe(page: Page, ms = 1200): Promise<void> {
  await page.waitForTimeout(ms);
}

async function glideTo(page: Page, locator: Locator): Promise<void> {
  try {
    await locator.scrollIntoViewIfNeeded({ timeout: 2_000 });
    const box = await locator.boundingBox({ timeout: 2_000 });
    if (box) {
      await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2, { steps: 10 });
      await page.waitForTimeout(20);
    }
  } catch {
    // Best effort only.
  }
}

export async function humanClick(page: Page, locator: Locator): Promise<void> {
  await glideTo(page, locator);
  await locator.click();
}

export async function humanFill(page: Page, locator: Locator, value: string): Promise<void> {
  await glideTo(page, locator);
  await locator.click();
  await locator.fill('');
  await locator.pressSequentially(value, { delay: 10 });
}

export async function humanSelect(page: Page, locator: Locator, value: string): Promise<void> {
  await glideTo(page, locator);
  await locator.selectOption(value);
}

function patchLocatorPrototype(page: Page): () => void {
  const sampleLocator = page.locator('body');
  const proto = Object.getPrototypeOf(sampleLocator);

  const origClick = proto.click;
  const origDblclick = proto.dblclick;
  const origFill = proto.fill;
  const origSelectOption = proto.selectOption;
  const origHover = proto.hover;
  const origCheck = proto.check;
  const origUncheck = proto.uncheck;
  const origDragTo = proto.dragTo;

  proto.click = async function (this: Locator, options?: unknown) {
    await glideTo(this.page(), this);
    return origClick.call(this, options);
  };

  proto.dblclick = async function (this: Locator, options?: unknown) {
    await glideTo(this.page(), this);
    return origDblclick.call(this, options);
  };

  proto.fill = async function (this: Locator, value: string, options?: unknown) {
    await glideTo(this.page(), this);

    const type = await this.getAttribute('type').catch(() => null);
    const isSpecial =
      type && ['date', 'month', 'week', 'time', 'datetime-local', 'color', 'range'].includes(type);

    if (isSpecial) {
      return origFill.call(this, value, options);
    }

    await origClick.call(this);
    await origFill.call(this, '');
    return this.pressSequentially(value, { delay: 10 });
  };

  proto.selectOption = async function (this: Locator, values: unknown, options?: unknown) {
    await glideTo(this.page(), this);
    return origSelectOption.call(this, values, options);
  };

  proto.hover = async function (this: Locator, options?: unknown) {
    await glideTo(this.page(), this);
    return origHover.call(this, options);
  };

  proto.check = async function (this: Locator, options?: unknown) {
    await glideTo(this.page(), this);
    return origCheck.call(this, options);
  };

  proto.uncheck = async function (this: Locator, options?: unknown) {
    await glideTo(this.page(), this);
    return origUncheck.call(this, options);
  };

  proto.dragTo = async function (this: Locator, target: Locator, options?: unknown) {
    await glideTo(this.page(), this);
    return origDragTo.call(this, target, options);
  };

  return () => {
    proto.click = origClick;
    proto.dblclick = origDblclick;
    proto.fill = origFill;
    proto.selectOption = origSelectOption;
    proto.hover = origHover;
    proto.check = origCheck;
    proto.uncheck = origUncheck;
    proto.dragTo = origDragTo;
  };
}

async function requestJson<T>(response: APIResponse): Promise<T> {
  const text = await response.text();
  if (!response.ok()) {
    throw new Error(text || response.statusText());
  }
  return text ? (JSON.parse(text) as T) : ({} as T);
}

export async function createNextOrder(page: Page) {
  const response = await page.request.post('/api/orders/next');
  return requestJson<{
    orderId: string;
    status: 'PENDING' | 'PAID';
    lastPaymentId: string | null;
    lastPgTransactionId: string | null;
  }>(response);
}

export async function getCurrentOrder(page: Page) {
  const response = await page.request.get('/api/orders/current');
  return requestJson<{
    orderId: string;
    status: 'PENDING' | 'PAID';
    lastPaymentId: string | null;
    lastPgTransactionId: string | null;
  }>(response);
}

export async function getPayments(page: Page, orderId?: string) {
  const query = orderId ? `?orderId=${encodeURIComponent(orderId)}` : '';
  const response = await page.request.get(`/api/payments${query}`);
  return requestJson<
    Array<{
      orderId: string;
      customerId: string;
      amount: number;
      paymentId: string;
      pgTransactionId: string;
      status: string;
      threadName: string;
      requestedAt: string;
      approvedAt: string;
    }>
  >(response);
}

export async function getOrder(page: Page, orderId: string) {
  const response = await page.request.get(`/api/orders/${encodeURIComponent(orderId)}`);
  return requestJson<{
    orderId: string;
    status: 'PENDING' | 'PAID';
    lastPaymentId: string | null;
    lastPgTransactionId: string | null;
  }>(response);
}

export async function waitForOrderPaid(page: Page, orderId: string) {
  await expect
    .poll(async () => {
      const order = await getOrder(page, orderId);
      const payments = await getPayments(page, orderId);
      return {
        status: order.status,
        paymentCount: payments.length,
      };
    }, {
      timeout: 10_000,
      intervals: [200, 400, 800],
    })
    .toEqual({
      status: 'PAID',
      paymentCount: 1,
    });
}

export const test = base.extend<{ qaDebug: QaDebugCollector }>({
  page: async ({ page }, use) => {
    const restore = patchLocatorPrototype(page);

    const injectCursor = async () => {
      await page.evaluate(() => {
        if (document.getElementById('pw-cursor')) {
          return;
        }

        const cursor = document.createElement('div');
        cursor.id = 'pw-cursor';
        Object.assign(cursor.style, {
          position: 'fixed',
          zIndex: '999999',
          pointerEvents: 'none',
          width: '20px',
          height: '20px',
          borderRadius: '50%',
          background: 'rgba(255, 68, 68, 0.7)',
          border: '2px solid rgba(255, 68, 68, 0.9)',
          boxShadow: '0 0 8px rgba(255, 68, 68, 0.4)',
          transform: 'translate(-50%, -50%)',
          transition: 'width 0.15s, height 0.15s, background 0.15s',
          top: '-100px',
          left: '-100px',
        });
        document.body.appendChild(cursor);

        const ring = document.createElement('div');
        ring.id = 'pw-cursor-ring';
        Object.assign(ring.style, {
          position: 'fixed',
          zIndex: '999998',
          pointerEvents: 'none',
          width: '40px',
          height: '40px',
          borderRadius: '50%',
          border: '2px solid rgba(255, 68, 68, 0.6)',
          transform: 'translate(-50%, -50%) scale(0)',
          top: '-100px',
          left: '-100px',
          opacity: '0',
        });
        document.body.appendChild(ring);

        document.addEventListener('mousemove', (event) => {
          cursor.style.top = `${event.clientY}px`;
          cursor.style.left = `${event.clientX}px`;
          ring.style.top = `${event.clientY}px`;
          ring.style.left = `${event.clientX}px`;
        });

        document.addEventListener('mousedown', () => {
          cursor.style.width = '14px';
          cursor.style.height = '14px';
          cursor.style.background = 'rgba(255, 30, 30, 0.9)';
          ring.style.transition = 'none';
          ring.style.transform = 'translate(-50%, -50%) scale(0)';
          ring.style.opacity = '1';

          requestAnimationFrame(() => {
            ring.style.transition = 'transform 0.4s ease-out, opacity 0.4s ease-out';
            ring.style.transform = 'translate(-50%, -50%) scale(1.5)';
            ring.style.opacity = '0';
          });
        });

        document.addEventListener('mouseup', () => {
          cursor.style.width = '20px';
          cursor.style.height = '20px';
          cursor.style.background = 'rgba(255, 68, 68, 0.7)';
        });
      });
    };

    page.on('load', injectCursor);
    page.on('domcontentloaded', injectCursor);

    await use(page);
    restore();
  },
  qaDebug: async ({ page }, use) => {
    const consoleLogs: ConsoleEntry[] = [];
    const pageErrors: PageErrorEntry[] = [];
    const requestFailures: RequestFailureEntry[] = [];

    const onConsole = (message: { type(): string; text(): string }) => {
      pushLimited(consoleLogs, {
        type: message.type(),
        text: message.text(),
      });
    };

    const onPageError = (error: Error) => {
      pushLimited(pageErrors, {
        message: error.message,
        stack: error.stack ?? null,
      });
    };

    const onRequestFailed = (request: Request) => {
      pushLimited(requestFailures, {
        method: request.method(),
        url: request.url(),
        errorText: request.failure()?.errorText ?? null,
      });
    };

    page.on('console', onConsole);
    page.on('pageerror', onPageError);
    page.on('requestfailed', onRequestFailed);

    await use({
      snapshot: () => ({
        consoleLogs: [...consoleLogs],
        pageErrors: [...pageErrors],
        requestFailures: [...requestFailures],
      }),
    });

    page.off('console', onConsole);
    page.off('pageerror', onPageError);
    page.off('requestfailed', onRequestFailed);
  },
});

export { expect };
