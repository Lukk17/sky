import {of} from 'rxjs';
import {OffersService} from '../api-generated/api/offers.service';
import {OwnerOffersService} from '../api-generated/api/ownerOffers.service';
import {OfferDTO} from '../api-generated/model/offerDTO';
import {OfferPage} from '../api-generated/model/offerPage';
import {Offer, OfferService} from './offer.service';

function offerPage(content: OfferDTO[]): OfferPage {
  return {
    content,
    empty: content.length === 0,
    first: true,
    last: true,
    number: 0,
    numberOfElements: content.length,
    pageable: {
      offset: 0,
      pageNumber: 0,
      pageSize: 20,
      paged: true,
      sort: {empty: true, sorted: false, unsorted: true},
      unpaged: false,
    },
    size: 20,
    sort: {empty: true, sorted: false, unsorted: true},
    totalElements: content.length,
    totalPages: 1,
  };
}

const harborDto: OfferDTO = {
  id: '11111111-1111-1111-1111-111111111111',
  hotelName: 'Harbor',
  description: 'Sea view',
  comment: null,
  price: 120.5,
  ownerEmail: 'owner@test.local',
  roomCapacity: 2,
  city: 'Split',
  country: 'Croatia',
  externalPhotoUrl: null,
  photoUrl: null,
  gallery: [
    {id: 'photo-b', main: false, position: 1, url: 'https://cdn.test/b.jpg'},
    {id: 'photo-a', main: true, position: 0, url: 'https://cdn.test/a.jpg'},
  ],
  coverPhotoUrl: null,
};

describe('OfferService', () => {
  it('searches through the generated GET with term and pageable and maps typed content', () => {
    const search = jasmine.createSpy('search').and.returnValue(of(offerPage([harborDto])));
    const offersService = {search} as unknown as OffersService;
    const service = new OfferService(offersService, {} as OwnerOffersService);

    let offers: Offer[] = [];
    service.searchOffer('harbor').subscribe((result) => (offers = result));

    expect(search).toHaveBeenCalledOnceWith('harbor', jasmine.any(Object), 'body', false);
    expect(offers.length).toBe(1);
    expect(offers[0].hotelName).toBe('Harbor');
    expect(offers[0].price).toBe(120.5);
    expect(offers[0].gallery!.map((p) => p.id)).toEqual(['photo-a', 'photo-b']);
    expect(offers[0].coverPhotoUrl).toBe('https://cdn.test/a.jpg');
  });

  it('maps the getAllOffers page content to offers', () => {
    const getAllOffers = jasmine.createSpy('getAllOffers').and.returnValue(of(offerPage([harborDto])));
    const offersService = {getAllOffers} as unknown as OffersService;
    const service = new OfferService(offersService, {} as OwnerOffersService);

    let offers: Offer[] = [];
    service.getAllOffers().subscribe((result) => (offers = result));

    expect(getAllOffers).toHaveBeenCalledOnceWith(jasmine.any(Object), 'body', false);
    expect(offers.map((o) => o.hotelName)).toEqual(['Harbor']);
  });
});
