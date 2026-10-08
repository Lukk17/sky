import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {AuthComponent} from './auth/auth.component';
import {HelloComponent} from './hello/hello.component';
import {AddOfferComponent} from './offer/add-offer/add-offer.component';
import {UserDetailsComponent} from './user/user-details/user-details.component';
import {OfferSearchComponent} from './offer/offer-search/offer-search.component';
import {EditOfferComponent} from './offer/edit-offer/edit-offer.component';
import {OfferDetailsComponent} from './offer/offer-details/offer-details.component';
import {MessageComponent} from './message/message.component';
import {NewMessageComponent} from './message/new-message/new-message.component';
import {OffersOwnedComponent} from './offer/offers-owned/offers-owned.component';
import {PageNotFoundComponent} from './page-not-found/page-not-found.component';

const routes: Routes = [
  {path: '', component: HelloComponent},
  {path: 'home', redirectTo: '', pathMatch: 'full' },
  {path: 'auth', component: AuthComponent},
  {path: 'my-offers', component: OffersOwnedComponent},
  {path: 'myOffers', redirectTo: 'my-offers', pathMatch: 'full'},
  {path: 'add-offer', component: AddOfferComponent},
  {path: 'addOffer', redirectTo: 'add-offer', pathMatch: 'full'},
  {path: 'user-details', component: UserDetailsComponent},
  {path: 'userDetails', redirectTo: 'user-details', pathMatch: 'full'},
  {path: 'offer-search', component: OfferSearchComponent},
  {path: 'offerSearch', redirectTo: 'offer-search', pathMatch: 'full'},
  {path: 'edit-offer', component: EditOfferComponent},
  {path: 'editOffer', redirectTo: 'edit-offer', pathMatch: 'full'},
  {path: 'offer-details', component: OfferDetailsComponent},
  {path: 'offerDetails', redirectTo: 'offer-details', pathMatch: 'full'},
  {path: 'messages', component: MessageComponent},
  {path: 'new-message', component: NewMessageComponent},
  {path: 'newMessage', redirectTo: 'new-message', pathMatch: 'full'},
  { path: '**', pathMatch: 'full',
    component: PageNotFoundComponent },

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
