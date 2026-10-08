import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
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
  private readonly destroyRef = inject(DestroyRef);

  constructor(private messageService: MessageService, private router: Router, private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (params) => {
        this.receiver = params['receiver'] ?? '';
      },
      error: () => {
        this.error = 'Could not read the route. Please try again.';
      }});
  }

  onSubmit(message: NgForm) {
    this.error = null;
    if (message.invalid) {
      this.error = 'Please fill in all required fields.';
      return;
    }
    const v = message.value as Record<string, string>;
    this.messageService.sendMessage({text: String(v['text'] ?? ''), receiver: String(v['receiver'] ?? this.receiver ?? '')}).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.router.navigate(['/messages']).then(),
      error: () => {
        this.error = 'Could not send the message. Please try again.';
      }});
  }
}
