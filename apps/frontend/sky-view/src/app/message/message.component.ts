import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Message, MessageService} from '../services/message.service';
import {StompService} from '../services/StompService';

@Component({
    selector: 'app-message',
    templateUrl: './message.component.html',
    styleUrls: ['./message.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class MessageComponent implements OnInit {
  RECEIVED_PAGE = 'received';
  SENT_PAGE = 'sent';

  sent!: Message[];
  received!: Message[];
  currentPage!: string;

  private readonly destroyRef = inject(DestroyRef);

  constructor(private messageService: MessageService, private stompService: StompService) {
  }

  ngOnInit(): void {

    this.messageService.getReceived().subscribe(messages => {
      this.received = messages;
    });

    this.messageService.getSent().subscribe(messages => {
      this.sent = messages;
    });

    this.currentPage = this.RECEIVED_PAGE;

    this.stompService.getMessages().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.messageService.getReceived().subscribe(messages => {
        this.received = messages;
      });
    });
  }

  goToSent() {
    this.currentPage = this.SENT_PAGE;
    this.messageService.getSent().subscribe(messages => {
      this.sent = messages;
    });
    console.log(`Sent: ${this.currentPage}`);
  }

  goToReceived() {
    this.currentPage = this.RECEIVED_PAGE;
    this.messageService.getReceived().subscribe(messages => {
      this.received = messages;
    });
    console.log(`Received: ${this.currentPage}`);
  }

  deleteMessage(id: number) {
    this.messageService.deleteMessage(id).subscribe(value => {
      console.log(value);
      return value;
    });
  }
}

