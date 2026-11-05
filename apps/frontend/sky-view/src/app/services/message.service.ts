import {Injectable} from '@angular/core';
import {catchError, map} from 'rxjs/operators';
import {shareReplay, switchMap, timer} from 'rxjs';
import {ResponseHandlerService} from './responseHandler.service';
import {MessagesService} from '../api-generated/api/messages.service';
import {MessageDTO} from '../api-generated/model/messageDTO';
import {MessagePage} from '../api-generated/model/messagePage';
import {Pageable} from '../api-generated/model/pageable';

@Injectable({
  providedIn: 'root'
})
export class MessageReadStore {
  private readIds = new Set<string>();

  markRead(id: string): void {
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

  // Threads are built client-side from the whole inbox, so ask for one big
  // page: the default size 20 hides older threads and any message past it.
  // PAGE_SIZE is the inbox page size used by getReceived/getSent; increase only with backend pagination support.
  private static readonly PAGE_SIZE = 500;
  public readonly unread$ = timer(0, 5000).pipe(
    switchMap(() => this.getReceived()),
    shareReplay({bufferSize: 1, refCount: true}),
  );

  constructor(private messagesService: MessagesService) {
  }

  private static mapMessageDtoToMessage(dto: MessageDTO): Message {
    const message = new Message(dto.text, dto.receiverEmail);
    message.id = dto.id ?? '';
    message.createdTime = dto.createdTime ?? '';
    message.read = dto.read ?? false;
    message.senderEmail = dto.senderEmail ?? '';
    return message;
  }

  private static buildMessage(draft: MessageDraft) {
    return new Message(
      draft.text,
      draft.receiver,
    );
  }

  getReceived() {
    const pageable: Pageable = {size: MessageService.PAGE_SIZE};
    return this.messagesService.getReceivedMessages(pageable, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'getReceived()')),
      map((resp: MessagePage) => ResponseHandlerService.handleMessageResponse(
        resp.content.map((dto) => MessageService.mapMessageDtoToMessage(dto))))
    );
  }

  getSent() {
    const pageable: Pageable = {size: MessageService.PAGE_SIZE};
    return this.messagesService.getSentMessages(pageable, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'getSent()')),
      map((resp: MessagePage) => ResponseHandlerService.handleMessageResponse(
        resp.content.map((dto) => MessageService.mapMessageDtoToMessage(dto))))
    );
  }

  sendMessage(draft: MessageDraft) {
    const message = MessageService.buildMessage(draft);
    return this.sendDirect(message.receiverEmail, message.text);
  }

  sendDirect(receiverEmail: string, text: string) {
    return this.messagesService.sendMessage({receiverEmail, text}, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'sendMessage()')),
      map((resp: MessageDTO) => MessageService.mapMessageDtoToMessage(resp))
    );
  }

  deleteMessage(id: string) {
    return this.messagesService.deleteMessage(id, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'deleteMessage()')),
      map(() => undefined)
    );
  }
}

export class Message {

  'id': string;
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
