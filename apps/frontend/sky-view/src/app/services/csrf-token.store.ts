import {Injectable} from '@angular/core';

@Injectable({providedIn: 'root'})
export class CsrfTokenStore {
  private token: string | null = null;

  setToken(token: string | null): void {
    this.token = token;
  }

  getToken(): string | null {
    return this.token;
  }
}
