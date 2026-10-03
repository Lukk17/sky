import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject, BehaviorSubject } from 'rxjs';
import { MessageComponent } from './message.component';
import { Message, MessageReadStore, MessageService } from '../services/message.service';
import { StompService } from '../services/StompService';
import { SkyAuthService } from '../services/sky-auth.service';

function setup(received: Message[] = [], sent: Message[] = []) {
  const socketMessages = new Subject<string | null>();
  const unreadSubject = new Subject<Message[]>();
  const messageService = {
    getReceived: () => of(received),
    getSent: () => of(sent),
    unread$: unreadSubject.asObservable(),
  } as unknown as MessageService;
  TestBed.configureTestingModule({
    declarations: [MessageComponent],
    schemas: [NO_ERRORS_SCHEMA],
    providers: [
      { provide: MessageService, useValue: messageService },
      MessageReadStore,
      { provide: StompService, useValue: { getMessages: () => socketMessages.asObservable() } },
      { provide: SkyAuthService, useValue: { currentUser$: new BehaviorSubject('me@test.local') } },
      { provide: ActivatedRoute, useValue: { queryParams: of({}) } },
      { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
    ],
  });
  const fixture = TestBed.createComponent(MessageComponent);
  return { fixture, service: TestBed.inject(MessageService), socketMessages };
}

describe('MessageComponent session socket', () => {
  it('refreshes received messages when the session socket pushes', () => {
    const { fixture, service, socketMessages } = setup();
    spyOn(service, 'getReceived').and.callThrough();

    fixture.detectChanges();
    socketMessages.next('new-message');

    expect(service.getReceived).toHaveBeenCalledTimes(2);
  });

  it('groups sent plus received by other party with newest thread first', () => {
    const { fixture } = setup(
      [
        { id: 1, text: 'hi', senderEmail: 'a@test.local', receiverEmail: 'me@test.local', createdTime: '2026-01-01T10:00:00', read: true },
        { id: 2, text: 'hey', senderEmail: 'b@test.local', receiverEmail: 'me@test.local', createdTime: '2026-02-01T10:00:00', read: false },
      ] as unknown as Message[],
      [
        { id: 3, text: 'yo', senderEmail: 'me@test.local', receiverEmail: 'a@test.local', createdTime: '2026-03-01T10:00:00', read: true },
      ] as unknown as Message[],
    );
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    expect(cmp.threads.map((t) => t.email)).toEqual(['a@test.local', 'b@test.local']);
    expect(cmp.threads[0].messages.map((m) => m.id)).toEqual([1, 3]);
    expect(cmp.unreadCount).toBe(1);
  });
});
