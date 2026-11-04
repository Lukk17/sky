import {Injectable} from '@angular/core';
import {catchError, map, of} from 'rxjs';
import {switchMap} from 'rxjs/operators';

import {ResponseHandlerService} from './responseHandler.service';
import {OffersService} from '../api-generated/api/offers.service';
import {OwnerOffersService} from '../api-generated/api/ownerOffers.service';
import {OfferDTO} from '../api-generated/model/offerDTO';
import {OfferEditDTO} from '../api-generated/model/offerEditDTO';
import {Pageable} from '../api-generated/model/pageable';
import {Page} from '../api-generated/model/page';

export interface OfferDraft {
  hotelName: string;
  description: string;
  price: number;
  roomCapacity: number;
  city: string;
  country: string;
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

@Injectable({
  providedIn: 'root'
})
export class OfferService {
  searched: Offer[] = [];

  constructor(private offersService: OffersService, private ownerOffersService: OwnerOffersService) {
  }

  private static mapPageToOffers(page: Page): Offer[] {
    const content = (page as any)?.content ?? [];
    return content.map((dto: any) => OfferService.mapOfferDtoToOffer(dto));
  }

  private static mapOfferDtoToOffer(dto: OfferDTO): Offer {
    const offer = new Offer(
      dto.hotelName ?? '',
      dto.description ?? '',
      dto.price ?? 0,
      dto.roomCapacity ?? 1,
      dto.city ?? '',
      dto.country ?? '',
      dto.photoUrl ?? '',
    );
    offer.id = dto.id ?? '';
    offer.ownerEmail = dto.ownerEmail ?? '';
    offer.comment = dto.comment ?? '';
    offer.photoUrl = dto.photoUrl ?? null;
    offer.externalPhotoUrl = dto.externalPhotoUrl ?? null;
    offer.coverPhotoUrl = dto.coverPhotoUrl ?? null;
    if (dto.gallery) {
      offer.gallery = dto.gallery.map((p) => ({
        id: String(p.id ?? ''),
        position: Number(p.position ?? 0),
        url: p.url ?? '',
        main: p.main === true,
      }));
    }
    return offer;
  }

  public getAllOffers() {
    const pageable: Pageable = {} as Pageable;
    return this.offersService.getAllOffers(pageable, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'getAllOffers()')),
      map((resp: Page) => ResponseHandlerService.handleGetOffersResponse(OfferService.mapPageToOffers(resp)))
    );
  }

  public getUserOffers() {
    const pageable: Pageable = {} as Pageable;
    return this.ownerOffersService.getOwnedOffers(pageable, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'getUserOffers()')),
      map((resp: Page) => ResponseHandlerService.handleGetOffersResponse(OfferService.mapPageToOffers(resp)))
    );
  }

  public editOffer(id: string, draft: OfferDraft) {
    const offerEditDTO: OfferEditDTO = {
      id,
      hotelName: draft.hotelName,
      description: draft.description,
      price: draft.price,
      roomCapacity: draft.roomCapacity,
      city: draft.city,
      country: draft.country,
    };
    return this.ownerOffersService.edit(offerEditDTO, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'editOffer()')),
      map((resp: OfferDTO) => OfferService.mapOfferDtoToOffer(resp))
    );
  }

  public deleteOffer(id: string) {
    return this.ownerOffersService.deleteOffer(id, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'deleteOffer()'))
    );
  }

  public searchOffer(searched: string) {
    const pageable: Pageable = {} as Pageable;
    return this.offersService.search(pageable, searched, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'searchOffer()')),
      map((resp: Page) => ResponseHandlerService.handleGetOffersResponse(OfferService.mapPageToOffers(resp)))
    );
  }

  public addOffer(draft: OfferDraft) {
    const offerDTO: OfferDTO = {
      hotelName: draft.hotelName,
      description: draft.description,
      price: draft.price,
      roomCapacity: draft.roomCapacity,
      city: draft.city,
      country: draft.country,
    };
    return this.ownerOffersService.addOffer(offerDTO, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'addOffer()')),
      map((resp: OfferDTO) => OfferService.mapOfferDtoToOffer(resp))
    );
  }

  public getOfferById(id: string) {
    return this.offersService.getOfferById(id, 'body', false).pipe(
      catchError(() => of(null)),
      switchMap((one: OfferDTO | null) => {
        if (one) {
          const mapped = OfferService.mapOfferDtoToOffer(one);
          return of(ResponseHandlerService.handleGetOfferResponse(mapped));
        }
        return this.getAllOffers().pipe(map((offers) => (offers ?? []).find((o) => o.id === id) ?? null));
      }),
    );
  }

  public uploadPhoto(offerId: string, file: File) {
    return this.ownerOffersService.uploadPhoto(offerId, file as Blob, 'body', false).pipe(
      map((resp: OfferDTO) => OfferService.mapOfferDtoToOffer(resp))
    );
  }

  public uploadGalleryPhoto(offerId: string, file: File) {
    return this.ownerOffersService.uploadGalleryPhoto(offerId, file as Blob, 'body', false).pipe(
      map((resp: OfferDTO) => OfferService.mapOfferDtoToOffer(resp))
    );
  }

  public deleteGalleryPhoto(offerId: string, photoId: string) {
    return this.ownerOffersService.deleteGalleryPhoto(offerId, photoId, 'body', false).pipe(
      map((resp: OfferDTO) => OfferService.mapOfferDtoToOffer(resp))
    );
  }

  public setGalleryCover(offerId: string, photoId: string) {
    return this.ownerOffersService.setGalleryCover(offerId, photoId, 'body', false).pipe(
      map((resp: OfferDTO) => OfferService.mapOfferDtoToOffer(resp))
    );
  }

  public reorderGalleryPhoto(offerId: string, photoId: string, position: number) {
    return this.ownerOffersService.reorderGalleryPhoto(offerId, photoId, position, 'body', false).pipe(
      map((resp: OfferDTO) => OfferService.mapOfferDtoToOffer(resp))
    );
  }
}
