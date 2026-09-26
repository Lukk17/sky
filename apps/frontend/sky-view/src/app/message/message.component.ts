import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {Message, MessageService} from '../services/message.service';

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

  constructor(private messageService: MessageService) {
  }

  ngOnInit(): void {

    this.messageService.getReceived().subscribe(messages => {
      this.received = messages;
    });

    this.messageService.getSent().subscribe(messages => {
      this.sent = messages;
    });

    this.currentPage = this.RECEIVED_PAGE;
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

