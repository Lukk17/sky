import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
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

  ngOnInit() {
    this.skyAuthService.currentUser$.subscribe((email) => {
      if (email != null) {
        const returnPath = this.skyAuthService.consumePostLoginPath();
        this.router.navigate([isSafePostLoginPath(returnPath) ? returnPath as string : '/home']).then();
      }
    });
  }

  login() {
    this.skyAuthService.login('/home');
  }
}
