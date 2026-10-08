import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Router} from '@angular/router';
import {SkyAuthService, isSafePostLoginPath} from '../services/sky-auth.service';

@Component({
    selector: 'app-auth',
    templateUrl: './auth.component.html',
    styleUrls: ['./auth.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class AuthComponent implements OnInit {
  error: string | null = null;

  constructor(private skyAuthService: SkyAuthService, private router: Router) {
  }

  private readonly destroyRef = inject(DestroyRef);

  ngOnInit() {
    this.skyAuthService.currentUser$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((email) => {
      if (email != null) {
        const returnPath = this.skyAuthService.consumePostLoginPath();
        this.router.navigateByUrl(isSafePostLoginPath(returnPath) ? (returnPath as string) : '/home').then();
      }
    });
  }

  login() {
    this.skyAuthService.login('/home');
  }
}
