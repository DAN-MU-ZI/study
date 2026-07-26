import { expect, getCart, getCartPayments, getCurrentCart, humanClick, observe, test, waitForCartPaid } from './fixtures';
import { writeReadableArtifacts } from './readableArtifacts';

test.describe('advanced cart idempotency verification', () => {
  test.afterEach(async ({ page, qaDebug }, testInfo) => {
    await writeReadableArtifacts(page, testInfo, qaDebug.snapshot());
  });

  test.beforeEach(async ({ page }) => {
    const nextCartResponse = await page.request.post('/api/carts/next');
    const nextCart = await nextCartResponse.json();

    await page.goto('/');
    await expect(page.getByTestId('page-title')).toBeVisible();
    await expect(page.getByTestId('loading-indicator')).toBeHidden();
    await expect(page.getByTestId('cart-id')).toHaveText(nextCart.cartId);
  });

  test('TB-000: current cart is shown as pending', async ({ page }) => {
    const currentCart = await getCurrentCart(page);

    await expect(page.getByTestId('cart-card')).toBeVisible();
    await expect(page.getByTestId('cart-id')).toHaveText(currentCart.cartId);
    await expect(page.getByTestId('cart-status')).toHaveText('pending');
    await expect(page.getByTestId('payment-history')).toBeVisible();
    await expect(page.getByTestId('request-log')).toBeVisible();
    await expect(page.getByTestId('pay-button')).toBeEnabled();
    await expect(page.getByTestId('new-cart-button')).toBeDisabled();
  });

  test('TB-001: a single pay action creates one approval', async ({ page }) => {
    const currentCart = await getCurrentCart(page);

    await page.getByTestId('pay-button').click();
    await waitForCartPaid(page, currentCart.cartId);

    await expect(page.getByTestId('request-state')).toContainText('idle');
    await expect(page.getByTestId('cart-status')).toHaveText('paid');
    await expect(page.getByTestId('payment-row')).toHaveCount(1);
    await expect(page.getByTestId('duplicate-warning')).toHaveCount(0);

    const payments = await getCartPayments(page, currentCart.cartId);
    expect(payments).toHaveLength(1);
    expect(payments[0].cartId).toBe(currentCart.cartId);
  });

  test('TB-002: double click still collapses into one payment', async ({ page }) => {
    const currentCart = await getCurrentCart(page);

    await page.getByTestId('pay-button').dblclick();
    await waitForCartPaid(page, currentCart.cartId);

    await expect(page.getByTestId('request-state')).toContainText('idle');
    await expect(page.getByTestId('duplicate-warning')).toHaveCount(0);
    await expect(page.getByTestId('payment-row')).toHaveCount(1);

    const payments = await getCartPayments(page, currentCart.cartId);
    expect(payments).toHaveLength(1);
    expect(new Set(payments.map((payment) => payment.pgTransactionId)).size).toBe(1);
  });

  test('TB-003: buttons reflect in-flight processing state', async ({ page }) => {
    await humanClick(page, page.getByTestId('pay-button'));
    await expect(page.getByTestId('request-state')).toContainText('submitting');
    await expect(page.getByTestId('pay-button')).toBeDisabled();
    await expect(page.getByTestId('new-cart-button')).toBeDisabled();
    await observe(page, 150);
  });

  test('TB-004: cart state and payment evidence stay aligned', async ({ page }) => {
    const currentCart = await getCurrentCart(page);

    await page.getByTestId('pay-button').click();
    await waitForCartPaid(page, currentCart.cartId);

    const cart = await getCart(page, currentCart.cartId);
    const payments = await getCartPayments(page, currentCart.cartId);

    expect(cart.status).toBe('PAID');
    expect(payments).toHaveLength(1);
    await expect(page.getByTestId('cart-status')).toHaveText('paid');
    await expect(page.getByTestId('duplicate-warning')).toHaveCount(0);
  });

  test('TB-005: next cart rotates idempotency scope', async ({ page }) => {
    const paymentKeys: string[] = [];
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().includes('/api/payments')) {
        const key = request.headers()['idempotency-key'];
        if (key) {
          paymentKeys.push(key);
        }
      }
    });

    const firstCart = await getCurrentCart(page);
    await page.getByTestId('pay-button').click();
    await waitForCartPaid(page, firstCart.cartId);

    await expect(page.getByTestId('new-cart-button')).toBeEnabled();
    await humanClick(page, page.getByTestId('new-cart-button'));

    const secondCart = await getCurrentCart(page);
    expect(secondCart.cartId).not.toBe(firstCart.cartId);
    await expect(page.getByTestId('cart-status')).toHaveText('pending');

    await page.getByTestId('pay-button').click();
    await waitForCartPaid(page, secondCart.cartId);

    expect(paymentKeys).toHaveLength(2);
    expect(paymentKeys[0]).not.toBe(paymentKeys[1]);
  });
});
