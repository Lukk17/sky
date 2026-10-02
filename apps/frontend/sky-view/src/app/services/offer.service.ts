import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {catchError, map} from 'rxjs/operators';
import {NgForm} from '@angular/forms';
import {environment} from '../../environments/environment';
import {ResponseHandlerService} from './responseHandler.service';


@Injectable({
  providedIn: 'root'
})
export class OfferService {

  searched!: Offer[];
  editedOffer!: Offer;
  detailedOffer!: Offer;
  private BASE_ADDRESS = `${environment.apiBaseUrl}`;
  private ALL_OFFERS_URL = this.BASE_ADDRESS + `${environment.allOfferPath}`;
  private OWNED_OFFERS_URL = this.BASE_ADDRESS + `${environment.ownedOffersPath}`;
  private ADD_OFFER_URL = this.BASE_ADDRESS + `${environment.addOfferPath}`;
  private EDIT_OFFER_URL = this.BASE_ADDRESS + `${environment.editOfferPath}`;
  private DELETE_OFFER_URL = this.BASE_ADDRESS + `${environment.deleteOfferPath}`;
  private SEARCH_OFFER_URL = this.BASE_ADDRESS + `${environment.searchOfferPath}`;

  constructor(private http: HttpClient) {
  }

  private static buildOffer(offerForm: NgForm) {
    return new Offer(
      offerForm.value.hotelName,
      offerForm.value.description,
      offerForm.value.price,
      offerForm.value.roomCapacity,
      offerForm.value.city,
      offerForm.value.country,
      offerForm.value.photoPath,
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

  public editOffer(offerForm: NgForm) {
    const offer = OfferService.buildOffer(offerForm);
    offer.id = this.editedOffer.id;

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

  public addOffer(offerForm: NgForm) {
    const offer = OfferService.buildOffer(offerForm);

    return this.http.post<Offer>(this.ADD_OFFER_URL,
      offer);
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
  'photoPath': string;
  'photoUrl'?: string | null;
  'externalPhotoUrl'?: string | null;
  'gallery'?: OfferPhoto[];
  'coverPhotoUrl'?: string | null;

  constructor(hotelName: string, description: string, price: number, roomCapacity: number, city: string,
              country: string, photoPath: string) {
    this.hotelName = hotelName;
    this.description = description;
    this.price = price;
    this.roomCapacity = roomCapacity;
    this.city = city;
    this.country = country;
    this.photoPath = photoPath;
  }
}
