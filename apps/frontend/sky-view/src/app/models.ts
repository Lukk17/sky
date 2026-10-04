export interface OfferPhoto {
  id: string;
  position: number;
  url: string;
  main: boolean;
}

export interface OfferModel {
  id: string;
  hotelName: string;
  description: string;
  comment: string;
  price: number;
  ownerEmail: string;
  roomCapacity: number;
  city: string;
  country: string;
  photoUrl?: string | null;
  externalPhotoUrl?: string | null;
  gallery?: OfferPhoto[];
  coverPhotoUrl?: string | null;
}

export interface BookingModel {
  id: number;
  offerId: string;
  bookedDate: string;
  bookingUser: string;
  ownerEmail: string;
}

export interface PersonalBookingModel {
  hotelName: string;
  id: number;
  offerId: string;
  date: string;
}

export interface MessageModel {
  id: number;
  text: string;
  receiverEmail: string;
  senderEmail: string;
  createdTime: string;
  read: boolean;
}

export interface ThreadModel {
  email: string;
  messages: MessageModel[];
  latest: MessageModel;
  unread: number;
}
