import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {AuthComponent} from './auth/auth.component';
import {HeaderComponent} from './header/header.component';
import { provideHttpClient, withInterceptors, withInterceptorsFromDi, withXhr } from '@angular/common/http';
import {FormsModule} from '@angular/forms';
import {HelloComponent} from './hello/hello.component';
import {MatDialogModule} from '@angular/material/dialog';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {OffersComponent} from './offer/offers/offers.component';
import {AddOfferComponent} from './offer/add-offer/add-offer.component';
import {UserDetailsComponent} from './user/user-details/user-details.component';
import {OfferSearchComponent} from './offer/offer-search/offer-search.component';
import {EditOfferComponent} from './offer/edit-offer/edit-offer.component';
import {OfferDetailsComponent} from './offer/offer-details/offer-details.component';
import {MessageComponent} from './message/message.component';
import {NewMessageComponent} from './message/new-message/new-message.component';
import {CalendarModule, DateAdapter} from 'angular-calendar';
import {adapterFactory} from 'angular-calendar/date-adapters/date-fns';
import {NgOptimizedImage} from '@angular/common';
import {OffersOwnedComponent} from './offer/offers-owned/offers-owned.component';
import {PageNotFoundComponent} from './page-not-found/page-not-found.component';
import {sessionCookieInterceptor} from './services/session-cookie.interceptor';

@NgModule({ declarations: [
        AppComponent,
        HeaderComponent,
        AuthComponent,
        HelloComponent,
        OffersComponent,
        AddOfferComponent,
        UserDetailsComponent,
        OfferSearchComponent,
        EditOfferComponent,
        OfferDetailsComponent,
        MessageComponent,
        NewMessageComponent,
        OffersOwnedComponent,
        PageNotFoundComponent,
    ],
    bootstrap: [AppComponent], imports: [BrowserModule,
        AppRoutingModule,
        FormsModule,
        MatDialogModule,
        BrowserAnimationsModule,
        CalendarModule.forRoot({ provide: DateAdapter, useFactory: adapterFactory }),
        NgOptimizedImage,
    ], providers: [provideHttpClient(withXhr(), withInterceptorsFromDi(), withInterceptors([sessionCookieInterceptor]))] })
export class AppModule {
}
