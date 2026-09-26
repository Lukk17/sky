import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {NgForm} from '@angular/forms';
import {MessageService} from '../../services/message.service';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
    selector: 'app-new-message',
    templateUrl: './new-message.component.html',
    styleUrls: ['./new-message.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class NewMessageComponent implements OnInit {
  error: string | null = null;
  receiver = '';

  constructor(private messageService: MessageService, private router: Router, private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      console.log(params['receiver']);
      this.receiver = params['receiver'];
    });
  }

  onSubmit(message: NgForm) {

    this.messageService.sendMessage(message).subscribe(sentMessage => {
      console.log(sentMessage);
      this.router.navigate(['/messages']).then();
      return sentMessage;
    });
  }
}
