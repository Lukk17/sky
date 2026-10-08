// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  localDev: false,

  apiBaseUrl: 'http://localhost:5777',
  allOfferPath: '/api/v1/offers',
  ownedOffersPath: '/api/v1/owner/offers',
  addOfferPath: '/api/v1/owner/offers',
  editOfferPath: '/api/v1/owner/offers',
  deleteOfferPath: '/api/v1/owner/offers/',
  searchOfferPath: '/api/v1/search',

  bookings: '/api/v1/user/bookings',
  addBooking: '/api/v1/bookings',
  deleteBooking: '/api/v1/bookings/',

  receivedMessages: '/api/v1/messages/received',
  sentMessages: '/api/v1/messages/sent',
  sendMessage: '/api/v1/messages',
  deleteMessage: '/api/v1/messages/',

  notifySocketPath: '/notifyWebsocket',
};
