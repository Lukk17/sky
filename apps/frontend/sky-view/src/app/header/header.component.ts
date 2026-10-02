import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router} from '@angular/router';
import {SkyAuthService} from '../services/sky-auth.service';

import {NgForm} from '@angular/forms';
import {interval, switchMap} from 'rxjs';
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
  banner: { sender: string; text: string } | null = null;
  private bannerTimer: ReturnType<typeof setTimeout> | null = null;

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
        this.dismissBanner();
      }
    });
    this.stompService.getMessages().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((event) => {
      if (this.isAuth) {
        this.refreshUnread();
      }
      if (event && this.isAuth) {
        this.showBanner(event.senderEmail ?? 'Sky', event.text ?? 'You have a new message');
      }
    });
    // Chat messages publish no push event (sky-message has no Kafka/outbound),
    // so poll while logged in; STOMP still covers offer/booking events instantly.
    interval(5000).pipe(
      takeUntilDestroyed(this.destroyRef),
      switchMap(() => this.isAuth ? this.messageService.getReceived() : []),
    ).subscribe((messages) => {
      if (this.isAuth && messages) {
        this.applyUnread(messages);
      }
    });
  }

  private showBanner(sender: string, text: string) {
    this.banner = {sender, text};
    if (this.bannerTimer) {
      clearTimeout(this.bannerTimer);
    }
    this.bannerTimer = setTimeout(() => {
      this.banner = null;
    }, 6000);
  }

  dismissBanner() {
    this.banner = null;
    if (this.bannerTimer) {
      clearTimeout(this.bannerTimer);
      this.bannerTimer = null;
    }
  }

  private refreshUnread() {
    this.messageService.getReceived().subscribe((messages) => {
      this.applyUnread(messages ?? []);
    });
  }

  private applyUnread(messages: Message[]) {
    const previousIds = new Set(this.unread.map((m) => m.id));
    this.unread = [...(messages ?? [])]
      .filter((m) => !this.readStore.isRead(m))
      .sort((a, b) => new Date(b.createdTime).getTime() - new Date(a.createdTime).getTime());
    const fresh = this.unread.filter((m) => !previousIds.has(m.id));
    if (previousIds.size > 0 && fresh.length > 0) {
      const newest = fresh[0];
      this.showBanner(newest.senderEmail ?? 'Sky', newest.text ?? 'You have a new message');
    }
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
