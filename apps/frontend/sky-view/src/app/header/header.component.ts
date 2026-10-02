import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router} from '@angular/router';
import {SkyAuthService} from '../services/sky-auth.service';

import {NgForm} from '@angular/forms';
import {OfferService} from '../services/offer.service';
import {Message, MessageReadStore, MessageService} from '../services/message.service';
import {StompService} from '../services/StompService';

@Component({
    selector: 'app-header',
    templateUrl: './header.component.html',
    styleUrls: ['./header.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class HeaderComponent implements OnInit {
  isAuth = false;
  userEmail: string | null = null;
  unread: Message[] = [];
  navOpen = false;

  get unreadCount(): number {
    return this.unread.length;
  }

  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private offerService: OfferService,
    private skyAuth: SkyAuthService,
    private messageService: MessageService,
    private readStore: MessageReadStore,
    private stompService: StompService,
  ) {
  }

  ngOnInit() {
    this.skyAuth.currentUser$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((email) => {
      this.userEmail = email;
      this.isAuth = email != null;
      if (this.isAuth) {
        this.refreshUnread();
      } else {
        this.unread = [];
      }
    });
    this.stompService.getMessages().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.isAuth) {
        this.refreshUnread();
      }
    });
  }

  private refreshUnread() {
    this.messageService.getReceived().subscribe((messages) => {
      this.unread = [...(messages ?? [])]
        .filter((m) => !this.readStore.isRead(m))
        .sort((a, b) => new Date(b.createdTime).getTime() - new Date(a.createdTime).getTime());
    });
  }

  openThread(message: Message) {
    this.readStore.markRead(message.id);
    this.unread = this.unread.filter((m) => m.id !== message.id);
    this.router.navigate(['/messages'], {queryParams: {with: message.senderEmail}}).then();
  }

  routeToHome() {
    //  instead of using routerLink in html file router can be used in method here
    //  if relative path is needed it can be injected to constructor and used in method
    // now it will add this to previous path
    // this.router.navigate(['/'], {relativeTo: this.route}).then(r => this.logger.log("Route to home"))
    this.router.navigate(['/']).then();
  }

  login() {
    this.skyAuth.login(this.router.url);
  }

  logout() {
    this.isAuth = false;
    this.skyAuth.logout();
  }

  search(searchForm: NgForm) {
    this.offerService.searchOffer(searchForm.value.search).subscribe(offers => {
      this.offerService.searched = offers;
      // mid navigating to "/home" required to reload "/offerSearch" items [workaround]
      this.router.navigate(['/home']).then(() => {
        this.router.navigate(['/offerSearch']).then();
      });
    });
  }
}
