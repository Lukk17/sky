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

    if (`${environment.localDev}` === 'true') {
      console.log('Local Dev');
      console.log(`Is localDev: ${environment.localDev}`);
      console.log(`Base url: ${environment.apiBaseUrl}`);
    } else {
      console.log(`Base url: ${environment.apiBaseUrl}`);
    }

    if (`${environment.production}` === 'true') {
      console.log('Production build');
      console.log(`Is prod: ${environment.production}`);
    }

    this.skyAuth.currentUser$.subscribe((email) => {
      if (email != null) {
        const returnPath = this.skyAuth.consumePostLoginPath();
        if (isSafePostLoginPath(returnPath)) {
          this.router.navigate([returnPath as string]).then();
        }
      }
    });
  }
}
