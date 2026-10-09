export * from './bookings.service';
import { BookingsService } from './bookings.service';
export * from './messages.service';
import { MessagesService } from './messages.service';
export * from './offers.service';
import { OffersService } from './offers.service';
export * from './ownerOffers.service';
import { OwnerOffersService } from './ownerOffers.service';
export const APIS = [BookingsService, MessagesService, OffersService, OwnerOffersService];
