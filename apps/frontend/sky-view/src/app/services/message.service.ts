import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {catchError, tap} from 'rxjs/operators';
import {NgForm} from '@angular/forms';
import {ResponseHandlerService} from './responseHandler.service';
import {environment} from '../../environments/environment';

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

  private static buildMessage(messageForm: NgForm) {
    return new Message(
      messageForm.value.text,
      messageForm.value.receiver,
    );
  }

  getReceived() {
    return this.http.get<Message[]>(this.RECEIVED_MESSAGES_URL).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'getReceived()')),
      tap(ResponseHandlerService.handleMessageResponse)
    );
  }

  getSent() {
    return this.http.get<Message[]>(this.SENT_MESSAGES_URL).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'getSent()')),
      tap(ResponseHandlerService.handleMessageResponse)
    );
  }

  sendMessage(messageForm: NgForm) {
    const message = MessageService.buildMessage(messageForm);
    return this.http.post<Message>(this.SEND_MESSAGE_URL,
      message).pipe(
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
