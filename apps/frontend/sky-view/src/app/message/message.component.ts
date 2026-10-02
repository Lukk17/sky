import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router} from '@angular/router';
import {forkJoin} from 'rxjs';
import {Message, MessageReadStore, MessageService} from '../services/message.service';
import {StompService} from '../services/StompService';
import {SkyAuthService} from '../services/sky-auth.service';

export interface Thread {
  email: string;
  messages: Message[];
  latest: Message;
  unread: number;
}

@Component({
    selector: 'app-message',
    templateUrl: './message.component.html',
    styleUrls: ['./message.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class MessageComponent implements OnInit {
  threads: Thread[] = [];
  selectedEmail: string | null = null;
  replyText = '';
  ownEmail: string | null = null;
  private selectedFromUrl = false;
  private sentIds = new Set<number>();

  get selected(): Thread | null {
    return this.threads.find((t) => t.email === this.selectedEmail) ?? null;
  }

  get unreadCount(): number {
    return this.threads.reduce((n, t) => n + t.unread, 0);
  }

  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private messageService: MessageService,
    private readStore: MessageReadStore,
    private stompService: StompService,
    private skyAuth: SkyAuthService,
    private route: ActivatedRoute,
    private router: Router,
  ) {
  }

  ngOnInit(): void {
    this.skyAuth.currentUser$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((email) => {
      this.ownEmail = email;
      this.refresh();
    });

    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const withEmail = params['with'] ?? null;
      this.selectedEmail = withEmail;
      this.selectedFromUrl = withEmail != null;
      if (withEmail) {
        this.markThreadRead(withEmail);
      }
    });

    this.stompService.getMessages().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.refresh();
    });
  }

  private static newestFirst(messages: Message[]): Message[] {
    return [...(messages ?? [])].sort((a, b) =>
      new Date(b.createdTime).getTime() - new Date(a.createdTime).getTime());
  }

  private static oldestFirst(messages: Message[]): Message[] {
    return [...(messages ?? [])].sort((a, b) =>
      new Date(a.createdTime).getTime() - new Date(b.createdTime).getTime());
  }

  private refresh() {
    forkJoin({
      received: this.messageService.getReceived(),
      sent: this.messageService.getSent(),
    }).subscribe(({received, sent}) => {
      this.sentIds = new Set((sent ?? []).map((m) => m.id));
      this.threads = this.buildThreads(received ?? [], sent ?? []);
      if (this.selectedEmail && !this.threads.some((t) => t.email === this.selectedEmail)) {
        this.selectedEmail = null;
      }
      if (!this.selectedEmail && this.threads.length > 0) {
        this.selectThread(this.threads[0].email, false, false);
      } else if (this.selectedEmail && this.selectedFromUrl) {
        this.markThreadRead(this.selectedEmail);
      }
    });
  }

  private buildThreads(received: Message[], sent: Message[]): Thread[] {
    const byOther = new Map<string, Message[]>();
    const receivedIds = new Set(received.map((m) => m.id));
    for (const m of MessageComponent.newestFirst([...received, ...sent])) {
      const other = receivedIds.has(m.id)
        ? (m.senderEmail ?? this.otherParty(m))
        : (m.receiverEmail ?? this.otherParty(m));
      if (!other) {
        continue;
      }
      if (!byOther.has(other)) {
        byOther.set(other, []);
      }
      byOther.get(other)?.push(m);
    }
    const threads: Thread[] = [];
    for (const [email, msgs] of byOther) {
      const ordered = MessageComponent.oldestFirst(msgs);
      const latest = MessageComponent.newestFirst(msgs)[0];
      const unread = msgs.filter((m) => receivedIds.has(m.id) && !this.readStore.isRead(m)).length;
      threads.push({email, messages: ordered, latest, unread});
    }
    return threads.sort((a, b) =>
      new Date(b.latest.createdTime).getTime() - new Date(a.latest.createdTime).getTime());
  }

  private otherParty(m: Message): string | null {
    if (this.ownEmail) {
      if (m.senderEmail === this.ownEmail) {
        return m.receiverEmail;
      }
      if (m.receiverEmail === this.ownEmail) {
        return m.senderEmail;
      }
    }
    return m.senderEmail ?? m.receiverEmail ?? null;
  }

  isOwn(m: Message): boolean {
    if (this.ownEmail != null) {
      return m.senderEmail === this.ownEmail;
    }
    return this.sentIds.has(m.id);
  }

  selectThread(email: string, updateUrl = true, markRead = true) {
    this.selectedEmail = email;
    this.selectedFromUrl = false;
    if (markRead) {
      this.markThreadRead(email);
    }
    if (updateUrl) {
      this.router.navigate([], {relativeTo: this.route, queryParams: {with: email}}).then();
    }
  }

  private markThreadRead(email: string) {
    const thread = this.threads.find((t) => t.email === email);
    thread?.messages.forEach((m) => {
      if (!this.isOwn(m) && !m.read) {
        this.readStore.markRead(m.id);
      }
    });
    if (thread) {
      thread.unread = 0;
    }
  }

  sendReply() {
    const text = this.replyText.trim();
    if (!text || !this.selectedEmail) {
      return;
    }
    this.messageService.sendDirect(this.selectedEmail, text).subscribe(() => {
      this.replyText = '';
      this.refresh();
    });
  }

  deleteMessage(id: number) {
    this.messageService.deleteMessage(id).subscribe(value => {
      console.log(value);
      this.refresh();
      return value;
    });
  }
}
