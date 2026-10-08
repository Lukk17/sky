import {Injectable, NgZone, OnDestroy} from '@angular/core';
import {Client, IMessage} from '@stomp/stompjs';
import {BehaviorSubject, Observable, Subscription} from 'rxjs';
import {AppConfigService} from './app-config.service';
import {SkyAuthService} from './sky-auth.service';

function buildSocketUrl(config: AppConfigService): string {
  const origin = `${config.get().apiBaseUrl}`;
  const path = `${config.get().notifySocketPath}`;
  if (origin.startsWith('https')) {
    return 'wss' + origin.substring(5) + path;
  }
  if (origin.startsWith('http')) {
    return 'ws' + origin.substring(4) + path;
  }
  return origin + path;
}

export interface NotifyEvent {
  raw: string;
  senderEmail?: string;
  text?: string;
}

function parseNotifyEvent(raw: string): NotifyEvent {
  try {
    const parsed = JSON.parse(raw);
    const payload = parsed?.payload ?? parsed;
    const senderEmail = payload?.senderEmail ?? payload?.sender ?? undefined;
    const text = payload?.text ?? payload?.message ?? undefined;
    return {raw, senderEmail, text};
  } catch {
    return {raw};
  }
}

@Injectable({
  providedIn: 'root'
})
export class StompService implements OnDestroy {
  private client: Client | null = null;
  private authSub: Subscription;
  private connectedEmail: string | null = null;
  private notifications: BehaviorSubject<NotifyEvent | null> = new BehaviorSubject<NotifyEvent | null>(null);

  constructor(private skyAuth: SkyAuthService, private zone: NgZone, private config: AppConfigService) {
    this.authSub = this.skyAuth.currentUser$.subscribe((email) => {
      if (email) {
        this.ensureConnected(email);
      } else {
        this.disconnect();
      }
    });
  }

  ngOnDestroy(): void {
    this.authSub.unsubscribe();
    this.disconnect();
  }

  private ensureConnected(email: string): void {
    if (this.client?.active && this.connectedEmail === email) {
      return;
    }
    this.disconnect();
    this.connectedEmail = email;
    const client = new Client({
      webSocketFactory: () => new WebSocket(buildSocketUrl(this.config)),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });
    client.onConnect = () => {
      client.subscribe('/user/queue/notify', (message: IMessage) => {
        this.zone.run(() => {
          this.notifications.next(parseNotifyEvent(message.body));
        });
      });
    };
    client.onStompError = () => {
      this.zone.run(() => {
        this.notifications.next(null);
      });
    };
    client.onWebSocketClose = () => {
      this.zone.run(() => undefined);
    };
    this.client = client;
    client.activate();
  }

  private disconnect(): void {
    this.connectedEmail = null;
    const client = this.client;
    this.client = null;
    if (client?.active) {
      client.deactivate().then().catch(() => undefined);
    }
  }

  getMessages(): Observable<NotifyEvent | null> {
    return this.notifications.asObservable();
  }
}
