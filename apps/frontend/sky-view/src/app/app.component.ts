import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {Router} from '@angular/router';
import {environment} from '../environments/environment';
import {SkyAuthService, isSafePostLoginPath} from './services/sky-auth.service';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class AppComponent implements OnInit {

  constructor(private skyAuth: SkyAuthService, private router: Router) {
  }

  ngOnInit(): void {

    void environment.localDev;
    void environment.production;

    this.skyAuth.currentUser$.subscribe((email) => {
      if (email != null) {
        const returnPath = this.skyAuth.consumePostLoginPath();
        if (isSafePostLoginPath(returnPath)) {
          this.router.navigateByUrl(returnPath as string).then();
        }
      }
    });
  }
}
