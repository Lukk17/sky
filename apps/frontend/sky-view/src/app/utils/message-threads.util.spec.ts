import {groupThreads} from './message-threads.util';

describe('groupThreads', () => {
  it('groups by counterpart and counts unread', () => {
    const received = [{id: 1, senderEmail: 'a@x.io', receiverEmail: 'me@x.io', createdTime: '2026-01-02T10:00:00', text: 'hi'}];
    const sent = [{id: 2, senderEmail: 'me@x.io', receiverEmail: 'a@x.io', createdTime: '2026-01-03T10:00:00', text: 'yo'}];
    const threads = groupThreads(received, sent, 'me@x.io', () => false);
    expect(threads.length).toBe(1);
    expect(threads[0].email).toBe('a@x.io');
    expect(threads[0].unread).toBe(1);
  });
});
