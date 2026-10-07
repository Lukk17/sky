import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';

export interface AppConfig {
  apiBaseUrl: string;
  allOfferPath: string;
  ownedOffersPath: string;
  addOfferPath: string;
  editOfferPath: string;
  deleteOfferPath: string;
  searchOfferPath: string;
  bookings: string;
  addBooking: string;
  deleteBooking: string;
  receivedMessages: string;
  sentMessages: string;
  sendMessage: string;
  deleteMessage: string;
  notifySocketPath: string;
}

const CONFIG_URL = '/assets/config/app-config.json';

function defaults(): AppConfig {
  return {
    apiBaseUrl: environment.apiBaseUrl,
    allOfferPath: environment.allOfferPath,
    ownedOffersPath: environment.ownedOffersPath,
    addOfferPath: environment.addOfferPath,
    editOfferPath: environment.editOfferPath,
    deleteOfferPath: environment.deleteOfferPath,
    searchOfferPath: environment.searchOfferPath,
    bookings: environment.bookings,
    addBooking: environment.addBooking,
    deleteBooking: environment.deleteBooking,
    receivedMessages: environment.receivedMessages,
    sentMessages: environment.sentMessages,
    sendMessage: environment.sendMessage,
    deleteMessage: environment.deleteMessage,
    notifySocketPath: environment.notifySocketPath,
  };
}

@Injectable({providedIn: 'root'})
export class AppConfigService {
  private config: AppConfig = defaults();

  load(): Promise<void> {
    return fetch(CONFIG_URL, {cache: 'no-store'})
      .then((res) => (res.ok ? res.json() : {}))
      .then((json) => {
        this.config = {...defaults(), ...(json as Partial<AppConfig>)};
      })
      .catch(() => undefined);
  }

  get(): AppConfig {
    return this.config;
  }
}
