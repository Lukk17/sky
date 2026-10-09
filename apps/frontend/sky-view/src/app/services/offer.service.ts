import {Injectable} from '@angular/core';
import {catchError, map, of} from 'rxjs';
import {switchMap} from 'rxjs/operators';

import {ResponseHandlerService} from './responseHandler.service';
import {OffersService} from '../api-generated/api/offers.service';
import {OwnerOffersService} from '../api-generated/api/ownerOffers.service';
import {OfferDTO} from '../api-generated/model/offerDTO';
import {Pageable} from '../api-generated/model/pageable';
import {Page} from '../api-generated/model/page';
import {PhotoDTO} from '../api-generated/model/photoDTO';
import {PhotoPositionDTO} from '../api-generated/model/photoPositionDTO';

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

  private static mapPageToOffers(page: Page): Offer[] {
    const content = (page as any)?.content ?? [];
    return content.map((dto: any) => OfferService.mapOfferDtoToOffer(dto));
  }

  private static mapOfferDtoToOffer(dto: OfferDTO | any): Offer {
    const offer = new Offer(
      (dto as any).hotelName ?? (dto as any).name ?? '',
      (dto as any).description ?? '',
      typeof (dto as any).price === 'number' ? (dto as any).price : ((dto as any).price ? parseFloat((dto as any).price) : 0),
      (dto as any).roomCapacity ?? 1,
      (dto as any).city ?? '',
      (dto as any).country ?? '',
      (dto as any).photoPath ?? (dto as any).photoUrl ?? '',
    );
    offer.id = (dto as any).id ?? '';
    offer.ownerEmail = (dto as any).ownerEmail ?? '';
    offer.comment = (dto as any).comment ?? '';
    offer.photoUrl = (dto as any).photoUrl ?? null;
    offer.externalPhotoUrl = (dto as any).externalPhotoUrl ?? null;
    offer.coverPhotoUrl = (dto as any).coverPhotoUrl ?? (dto as any).photoUrl ?? null;
    if ((dto as any).gallery) {
      offer.gallery = (dto as any).gallery.map((p: any) => ({
        id: String(p.id ?? ''),
        position: Number(p.position ?? 0),
        url: p.url ?? '',
        main: p.main === true || p.cover === true,
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
    const offer = OfferService.buildOffer(draft);
    offer.id = id;
    const offerDTO: OfferDTO = {
      id: offer.id,
      name: offer.hotelName,
      price: String(offer.price),
    } as OfferDTO;
    return this.ownerOffersService.addOffer(offerDTO, 'body', false).pipe(
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
    return this.offersService.searchOffers(pageable, searched, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'searchOffer()')),
      map((resp: Page) => ResponseHandlerService.handleGetOffersResponse(OfferService.mapPageToOffers(resp)))
    );
  }

  public addOffer(draft: OfferDraft) {
    const offer = OfferService.buildOffer(draft);
    const offerDTO: OfferDTO = {
      name: offer.hotelName,
      price: String(offer.price),
    } as OfferDTO;
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
    return this.ownerOffersService.addPhoto(offerId, file as Blob, 'body', false).pipe(
      map((resp: PhotoDTO) => {
        const offer: Offer = new Offer('', '', 0, 0, '', '');
        offer.id = offerId;
        // Map PhotoDTO to offer's gallery - but service returns PhotoDTO; components expect updated offer structure
        // Keep behavior identical - just pass through as best as possible
        const anyResp: any = resp;
        if (anyResp && anyResp.gallery) {
          offer.gallery = anyResp.gallery;
        }
        if (anyResp && anyResp.coverPhotoUrl) {
          offer.coverPhotoUrl = anyResp.coverPhotoUrl;
        }
        return offer;
      })
    );
  }

  public deleteGalleryPhoto(offerId: string, photoId: string) {
    return this.ownerOffersService.deletePhotoById(offerId, photoId, 'body', false).pipe(
      map((resp: any) => {
        if (!resp) return new Offer('', '', 0, 0, '', '');
        return OfferService.mapOfferDtoToOffer(resp);
      })
    );
  }

  public setGalleryCover(offerId: string, photoId: string) {
    return this.ownerOffersService.setPhotoAsCover(offerId, photoId, 'body', false).pipe(
      map((resp: any) => {
        if (!resp) return new Offer('', '', 0, 0, '', '');
        return OfferService.mapOfferDtoToOffer(resp);
      })
    );
  }

  public reorderGalleryPhoto(offerId: string, photoId: string, position: number) {
    const dto: PhotoPositionDTO = { position } as PhotoPositionDTO;
    return this.ownerOffersService.updatePhotoPosition(offerId, photoId, dto, 'body', false).pipe(
      map((resp: any) => {
        if (!resp) return new Offer('', '', 0, 0, '', '');
        return OfferService.mapOfferDtoToOffer(resp);
      })
    );
  }
}
