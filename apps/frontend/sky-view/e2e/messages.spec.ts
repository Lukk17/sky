import { test, expect } from '@playwright/test';

const received = [
  { id: 1, text: 'older message', senderEmail: 'a@test.local', receiverEmail: 'me@test.local', createdTime: '2026-01-01T10:00:00', read: true },
  { id: 2, text: 'newer message', senderEmail: 'b@test.local', receiverEmail: 'me@test.local', createdTime: '2026-02-01T10:00:00', read: false },
];

test('threads render newest first with unread badge', async ({ page }) => {
  await page.route('**/api/v1/messages/received', (route) => route.fulfill({ json: received }));
  await page.route('**/api/v1/messages/sent', (route) => route.fulfill({ json: [] }));

  await page.goto('/messages');
  const threads = page.getByTestId('thread-list').locator('[data-testid^="thread-"]');
  await expect(threads.first()).toContainText('b@test.local');
  await expect(page.getByTestId('unread-b@test.local')).toBeVisible();
  await expect(page.getByTestId('threads-unread')).toContainText('1 unread');
});

test('deep link selects the right thread and reply sends', async ({ page }) => {
  await page.route('**/api/v1/messages/received', (route) => route.fulfill({ json: received }));
  await page.route('**/api/v1/messages/sent', (route) => route.fulfill({ json: [] }));
  await page.route('**/api/v1/messages', (route) => route.fulfill({
    json: { id: 3, text: 'reply here', senderEmail: 'me@test.local', receiverEmail: 'a@test.local', createdTime: '2026-03-01T10:00:00', read: true },
  }));

  await page.goto('/messages?with=a@test.local');
  await expect(page.getByTestId('conversation')).toContainText('a@test.local');
  await expect(page.getByTestId('bubble-1')).toContainText('older message');

  await page.getByTestId('reply-input').fill('reply here');
  await page.route('**/api/v1/messages/sent', (route) => route.fulfill({
    json: [{ id: 3, text: 'reply here', senderEmail: 'me@test.local', receiverEmail: 'a@test.local', createdTime: '2026-03-01T10:00:00', read: true }],
  }));
  await page.getByTestId('reply-send').click();
  await expect(page.getByTestId('bubble-3')).toContainText('reply here');
});

test('header bell badge opens thread and marks read', async ({ page }) => {
  const inbox = [
    { id: 7, text: 'hello bob', senderEmail: 'a@test.local', receiverEmail: 'b@test.local', createdTime: '2026-04-01T10:00:00', read: false },
  ];
  await page.route('**/api/v1/messages/received', (route) => route.fulfill({ json: inbox }));
  await page.route('**/api/v1/messages/sent', (route) => route.fulfill({ json: [] }));
  await page.route('**/api/session', (route) => route.fulfill({ json: { email: 'b@test.local' } }));

  await page.goto('/messages');
  await expect(page.getByTestId('thread-a@test.local')).toContainText('a@test.local');
  await expect(page.getByTestId('notify-badge')).toContainText('1');
  await page.getByTestId('notify-bell').click();
  await expect(page.getByTestId('notify-dropdown')).toContainText('hello bob');
  await page.getByTestId('notify-7').click();
  await expect(page).toHaveURL(/with=a%40test\.local|with=a@test\.local/);
  await expect(page.getByTestId('bubble-7')).toContainText('hello bob');
});
