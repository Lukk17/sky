import { enableProdMode, provideZoneChangeDetection } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app/app.module';
import { environment } from './environments/environment';
import {providePrimeNG} from 'primeng/config';
import Aura from '@primeuix/themes/aura';

if (environment.production) {
  enableProdMode();
}

platformBrowserDynamic().bootstrapModule(AppModule, { applicationProviders: [provideZoneChangeDetection(), providePrimeNG({theme: {preset: Aura, options: {darkModeSelector: '.sky-dark', cssLayer: false}}})], })
  .catch(err => console.error(err));
