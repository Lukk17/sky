import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {catchError, map} from 'rxjs/operators';
import {shareReplay, switchMap, timer} from 'rxjs';
import {ResponseHandlerService} from './responseHandler.service';
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class MessageReadStore {
  private readIds = new Set<number>();

  markRead(id: number): void {
    this.readIds.add(id);
  }

  isRead(m: Message): boolean {
    return m.read || this.readIds.has(m.id);
  }
}

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private BASE_ADDRESS = `${environment.apiBaseUrl}`;

  private RECEIVED_MESSAGES_URL = this.BASE_ADDRESS + `${environment.receivedMessages}`;
  private SENT_MESSAGES_URL = this.BASE_ADDRESS + `${environment.sentMessages}`;
  private SEND_MESSAGE_URL = this.BASE_ADDRESS + `${environment.sendMessage}`;
  private DELETE_MESSAGE_URL = this.BASE_ADDRESS + `${environment.deleteMessage}`;

  constructor(private http: HttpClient) {
  }

  private static buildMessage(draft: MessageDraft) {
    return new Message(
      draft.text,
      draft.receiver,
    );
  }

  // Threads are built client-side from the whole inbox, so ask for one big
  // page: the default size 20 hides older threads and any message past it.
  // PAGE is the inbox page size used by getReceived/getSent; increase only with backend pagination support.
  private static readonly PAGE = '?size=500';
  public readonly unread$ = timer(0, 5000).pipe(
    switchMap(() => this.getReceived()),
    shareReplay({bufferSize: 1, refCount: true}),
  );

  getReceived() {
    return this.http.get<Message[]>(`${this.RECEIVED_MESSAGES_URL}${MessageService.PAGE}`).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'getReceived()')),
      map((resp) => ResponseHandlerService.handleMessageResponse(resp))
    );
  }

  getSent() {
    return this.http.get<Message[]>(`${this.SENT_MESSAGES_URL}${MessageService.PAGE}`).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'getSent()')),
      map((resp) => ResponseHandlerService.handleMessageResponse(resp))
    );
  }

  sendMessage(draft: MessageDraft) {
    const message = MessageService.buildMessage(draft);
    return this.sendDirect(message.receiverEmail, message.text);
  }

  sendDirect(receiverEmail: string, text: string) {
    return this.http.post<Message>(this.SEND_MESSAGE_URL,
      {receiverEmail, text}).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'sendMessage()'))
    );
  }

  deleteMessage(id: number) {
    return this.http.delete<void>(this.DELETE_MESSAGE_URL + id).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'deleteMessage()'))
    );
  }
}

export class Message {

  'id': number;
  'text': string;
  'receiverEmail': string;
  'senderEmail': string;
  'createdTime': string;
  'read': boolean;

  constructor(text: string, receiverEmail: string) {
    this.text = text;
    this.receiverEmail = receiverEmail;
  }
}

export interface MessageDraft {
  text: string;
  receiver: string;
}
