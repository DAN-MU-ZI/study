import { expect, getCurrentOrder, getOrder, getPayments, humanClick, observe, test, waitForOrderPaid } from './fixtures';
import { writeReadableArtifacts } from './readableArtifacts';

test.describe('고급 멱등성 검증', () => {
  test.afterEach(async ({ page, qaDebug }, testInfo) => {
    await writeReadableArtifacts(page, testInfo, qaDebug.snapshot());
  });

  test.beforeEach(async ({ page }) => {
    const nextOrderResponse = await page.request.post('/api/orders/next');
    const nextOrder = await nextOrderResponse.json();

    await page.goto('/');
    await expect(page.getByTestId('page-title')).toBeVisible();
    await expect(page.getByTestId('loading-indicator')).toBeHidden();
    await expect(page.getByTestId('order-id')).toHaveText(nextOrder.orderId);
  });

  test('TB-000: 현재 주문이 pending으로 보인다', async ({ page }) => {
    const currentOrder = await getCurrentOrder(page);

    await expect(page.getByTestId('order-card')).toBeVisible();
    await expect(page.getByTestId('order-id')).toHaveText(currentOrder.orderId);
    await expect(page.getByTestId('order-status')).toHaveText('pending');
    await expect(page.getByTestId('payment-history')).toBeVisible();
    await expect(page.getByTestId('request-log')).toBeVisible();
    await expect(page.getByTestId('pay-button')).toBeEnabled();
    await expect(page.getByTestId('new-order-button')).toBeDisabled();
  });

  test('TB-001: 결제 버튼을 한 번 클릭하면 정상 승인된다', async ({ page }) => {
    const currentOrder = await getCurrentOrder(page);

    await page.getByTestId('pay-button').click();
    await waitForOrderPaid(page, currentOrder.orderId);

    await expect(page.getByTestId('request-state')).toContainText('idle');
    await expect(page.getByTestId('order-status')).toHaveText('paid');
    await expect(page.getByTestId('payment-row')).toHaveCount(1);
    await expect(page.getByTestId('duplicate-warning')).toHaveCount(0);

    const payments = await getPayments(page, currentOrder.orderId);
    expect(payments).toHaveLength(1);
    expect(payments[0].orderId).toBe(currentOrder.orderId);
  });

  test('TB-002: 같은 주문을 빠르게 반복 클릭했을 때의 결과를 확인한다', async ({ page }) => {
    const currentOrder = await getCurrentOrder(page);

    await page.getByTestId('pay-button').dblclick();
    await waitForOrderPaid(page, currentOrder.orderId);

    await expect(page.getByTestId('request-state')).toContainText('idle');
    await expect(page.getByTestId('duplicate-warning')).toHaveCount(0);
    await expect(page.getByTestId('payment-row')).toHaveCount(1);

    const payments = await getPayments(page, currentOrder.orderId);
    expect(payments).toHaveLength(1);
    expect(new Set(payments.map((payment) => payment.pgTransactionId)).size).toBe(1);
  });

  test('TB-003: 처리 중 버튼 상태를 확인한다', async ({ page }) => {
    await humanClick(page, page.getByTestId('pay-button'));
    await expect(page.getByTestId('request-state')).toContainText('submitting');
    await expect(page.getByTestId('pay-button')).toBeDisabled();
    await expect(page.getByTestId('new-order-button')).toBeDisabled();
    await observe(page, 150);
  });

  test('TB-004: 결제 후 주문 상태와 결제 이력이 일치한다', async ({ page }) => {
    const currentOrder = await getCurrentOrder(page);

    await page.getByTestId('pay-button').click();
    await waitForOrderPaid(page, currentOrder.orderId);

    const order = await getOrder(page, currentOrder.orderId);
    const payments = await getPayments(page, currentOrder.orderId);

    expect(order.status).toBe('PAID');
    expect(payments).toHaveLength(1);
    await expect(page.getByTestId('order-status')).toHaveText('paid');
    await expect(page.getByTestId('duplicate-warning')).toHaveCount(0);
  });

  test('TB-005: 다음 주문 시작 후 새 pending 주문으로 전환된다', async ({ page }) => {
    const paymentKeys: string[] = [];
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().includes('/api/payments')) {
        const key = request.headers()['idempotency-key'];
        if (key) {
          paymentKeys.push(key);
        }
      }
    });

    const firstOrder = await getCurrentOrder(page);
    await page.getByTestId('pay-button').click();
    await waitForOrderPaid(page, firstOrder.orderId);

    await expect(page.getByTestId('new-order-button')).toBeEnabled();
    await humanClick(page, page.getByTestId('new-order-button'));

    const secondOrder = await getCurrentOrder(page);
    expect(secondOrder.orderId).not.toBe(firstOrder.orderId);
    await expect(page.getByTestId('order-status')).toHaveText('pending');

    await page.getByTestId('pay-button').click();
    await waitForOrderPaid(page, secondOrder.orderId);

    expect(paymentKeys).toHaveLength(2);
    expect(paymentKeys[0]).not.toBe(paymentKeys[1]);
  });
});
