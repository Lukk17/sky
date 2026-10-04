import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {catchError, map, of} from 'rxjs';
import {switchMap} from 'rxjs/operators';

export interface OfferDraft {
  hotelName: string;
  description: string;
  price: number;
  roomCapacity: number;
  city: string;
  country: string;
}

import {environment} from '../../environments/environment';
import {ResponseHandlerService} from './responseHandler.service';


@Injectable({
  providedIn: 'root'
})
export class OfferService {

  searched: Offer[] = [];
  private BASE_ADDRESS = `${environment.apiBaseUrl}`;
  private ALL_OFFERS_URL = this.BASE_ADDRESS + `${environment.allOfferPath}`;
  private OWNED_OFFERS_URL = this.BASE_ADDRESS + `${environment.ownedOffersPath}`;
  private ADD_OFFER_URL = this.BASE_ADDRESS + `${environment.addOfferPath}`;
  private EDIT_OFFER_URL = this.BASE_ADDRESS + `${environment.editOfferPath}`;
  private DELETE_OFFER_URL = this.BASE_ADDRESS + `${environment.deleteOfferPath}`;
  private SEARCH_OFFER_URL = this.BASE_ADDRESS + `${environment.searchOfferPath}`;

  constructor(private http: HttpClient) {
  }

  private static buildOffer(draft: OfferDraft) {
    return new Offer(
      draft.hotelName,
      draft.description,
      draft.price,
      draft.roomCapacity,
      draft.city,
      draft.country,
    );
  }

  public getAllOffers() {
    return this.http
      .get<Offer[]>(this.ALL_OFFERS_URL)
      .pipe(
        catchError((err) => ResponseHandlerService.handleError(err, 'getAllOffers()')),
        map((resp) => ResponseHandlerService.handleGetOffersResponse(resp))
      );
  }

  public getUserOffers() {
    return this.http
      .get<Offer[]>(this.OWNED_OFFERS_URL)
      .pipe(
        catchError((err) => ResponseHandlerService.handleError(err, 'getUserOffers()')),
        map((resp) => ResponseHandlerService.handleGetOffersResponse(resp))
      );
  }

  public editOffer(id: string, draft: OfferDraft) {
    const offer = OfferService.buildOffer(draft);
    offer.id = id;

    return this.http.put<Offer>(this.EDIT_OFFER_URL,
      offer).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'deleteOffer()')),
    );
  }

  public deleteOffer(id: string) {
    return this.http.delete<void>(this.DELETE_OFFER_URL + id)
      .pipe(
        catchError((err) => ResponseHandlerService.handleError(err, 'deleteOffer()'))
      );
  }

  public searchOffer(searched: string) {
    return this.http
      .post<Offer[]>(this.SEARCH_OFFER_URL,
        searched
      )
      .pipe(
        catchError((err) => ResponseHandlerService.handleError(err, 'searchOffer()')),
        map((resp) => ResponseHandlerService.handleGetOffersResponse(resp))
      );
  }

  public addOffer(draft: OfferDraft) {
    const offer = OfferService.buildOffer(draft);

    return this.http.post<Offer>(this.ADD_OFFER_URL,
      offer).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'addOffer()'))
    );
  }

  public getOfferById(id: string) {
    return this.http.get<Offer>(`${this.ALL_OFFERS_URL}/${id}`).pipe(
      catchError(() => of(null)),
      switchMap((one) => {
        if (one) return of(ResponseHandlerService.handleGetOfferResponse(one));
        return this.getAllOffers().pipe(map((offers) => (offers ?? []).find((o) => o.id === id) ?? null));
      }),
    );
  }

  public uploadPhoto(offerId: string, file: File) {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<Offer>(`${this.ADD_OFFER_URL}/${offerId}/photo`, formData);
  }
}

export interface OfferPhoto {
  id: string;
  position: number;
  url: string;
  main: boolean;
}

export class Offer {

  'id': string;
  'hotelName': string;
  'description': string;
  'comment': string;
  'price': number;
  'ownerEmail': string;
  'roomCapacity': number;
  'city': string;
  'country': string;
  'photoPath'?: string;
  'photoUrl'?: string | null;
  'externalPhotoUrl'?: string | null;
  'gallery'?: OfferPhoto[];
  'coverPhotoUrl'?: string | null;

  constructor(hotelName: string, description: string, price: number, roomCapacity: number, city: string,
              country: string, photoPath?: string) {
    this.hotelName = hotelName;
    this.description = description;
    this.price = price;
    this.roomCapacity = roomCapacity;
    this.city = city;
    this.country = country;
    this.photoPath = photoPath;
  }
}
