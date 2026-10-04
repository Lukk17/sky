import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router} from '@angular/router';
import {forkJoin} from 'rxjs';
import {groupThreads} from '../utils/message-threads.util';
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

    this.stompService.getMessages()?.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((event) => {
      if (event) {
        this.refresh();
      }
    });

    this.messageService.unread$?.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        if (this.ownEmail) {
          this.refresh();
        }
      },
      error: () => undefined,
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

  loadError: string | null = null;

  private refresh() {
    forkJoin({
      received: this.messageService.getReceived(),
      sent: this.messageService.getSent(),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next: ({received, sent}) => {
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
      },
      error: () => {
        this.loadError = 'Could not load messages. Please try again.';
      }});
  }

  private buildThreads(received: Message[], sent: Message[]): Thread[] {
    return groupThreads(received, sent, this.ownEmail, (m) => this.readStore.isRead(m));
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
    this.messageService.sendDirect(this.selectedEmail, text).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.replyText = '';
        this.refresh();
      },
      error: () => {
        this.loadError = 'Could not send the reply. Please try again.';
      }});
  }

  deleteMessage(id: number) {
    this.messageService.deleteMessage(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.refresh(),
      error: () => {
        this.loadError = 'Could not delete the message. Please try again.';
      }});
  }
}
