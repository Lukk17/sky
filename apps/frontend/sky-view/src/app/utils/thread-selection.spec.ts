import { groupThreads } from './message-threads.util';

describe('thread selection', () => {
  it('selects latest thread first and counts unread', () => {
    const received = [
      { id: 1, senderEmail: 'a@x.io', receiverEmail: 'me@x.io', createdTime: '2026-01-01T10:00:00', text: 'hi', read: false },
      { id: 2, senderEmail: 'b@x.io', receiverEmail: 'me@x.io', createdTime: '2026-01-02T10:00:00', text: 'hey', read: true },
    ];
    const threads = groupThreads(received, [], 'me@x.io', (m) => !!m.read);
    expect(threads[0].email).toBe('b@x.io');
    expect(threads.find((t) => t.email === 'a@x.io')?.unread).toBe(1);
  });
});
