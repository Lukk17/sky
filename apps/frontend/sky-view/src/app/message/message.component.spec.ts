import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { MessageComponent } from './message.component';
import { MessageService } from '../services/message.service';
import { StompService } from '../services/StompService';

describe('MessageComponent session socket', () => {
  it('refreshes received messages when the session socket pushes', () => {
    const socketMessages = new Subject<string | null>();
    const messageService = {
      getReceived: () => of([]),
      getSent: () => of([]),
    } as unknown as MessageService;
    TestBed.configureTestingModule({
      declarations: [MessageComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: MessageService, useValue: messageService },
        { provide: StompService, useValue: { getMessages: () => socketMessages.asObservable() } },
      ],
    });
    const fixture = TestBed.createComponent(MessageComponent);
    const service = TestBed.inject(MessageService);
    spyOn(service, 'getReceived').and.callThrough();

    fixture.detectChanges();
    socketMessages.next('new-message');

    expect(service.getReceived).toHaveBeenCalledTimes(2);
  });
});
