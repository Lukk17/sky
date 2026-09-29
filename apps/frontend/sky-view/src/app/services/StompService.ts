import {Injectable} from '@angular/core';
import {Client, IMessage, Stomp} from '@stomp/stompjs';
import {BehaviorSubject, Observable} from 'rxjs';
import {environment} from '../../environments/environment';

function buildSocketUrl(): string {
  const origin = `${environment.apiBaseUrl}`;
  const path = `${environment.notifySocketPath}`;
  if (origin.startsWith('https')) {
    return 'wss' + origin.substring(5) + path;
  }
  if (origin.startsWith('http')) {
    return 'ws' + origin.substring(4) + path;
  }
  return origin + path;
}

@Injectable({
  providedIn: 'root'
})
export class StompService {
  private client: Client;
  private messages: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

  constructor() {
    this.client = Stomp.over(new WebSocket(buildSocketUrl()));

    this.client.onConnect = () => {
      this.client.subscribe('/sky/notify', (message: IMessage) => {
        this.messages.next(message.body);
      });
    };

    this.client.onStompError = (frame) => {
      console.error(`Error: ${frame.headers['message']}`);
    };

    this.client.activate();
  }

  sendMessage(message: string): void {
    this.client.publish({destination: '/sky/notify', body: message});
  }

  getMessages(): Observable<string | null> {
    return this.messages.asObservable();
  }
}
