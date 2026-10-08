import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, Observable, take} from 'rxjs';
import {AppConfigService} from './app-config.service';
import {CsrfTokenStore} from './csrf-token.store';

export interface SessionInfo {
  email: string;
  csrfToken?: string;
  logoutUrl?: string;
}

const POST_LOGIN_PATH_KEY = 'postLoginPath';
const DEFAULT_POST_LOGIN_PATH = '/home';

export function isSafePostLoginPath(candidate: string | null | undefined): boolean {
  if (candidate == null || candidate.length === 0 || candidate.length > 512) {
    return false;
  }
  if (candidate.charAt(0) !== '/' || candidate.charAt(1) === '/' || candidate.charAt(1) === '\\') {
    return false;
  }
  for (let i = 0; i < candidate.length; i++) {
    const c = candidate.charAt(i);
    if (c === '\\' || c === ' ' || c === '\t' || c === '\n' || c === '\r') {
      return false;
    }
  }
  const lower = candidate.toLowerCase();
  if (lower.indexOf(':') !== -1 || lower.indexOf('//') !== -1) {
    return false;
  }
  return true;
}

@Injectable({providedIn: 'root'})
export class SkyAuthService {
  private sessionUrl: string;
  private loginUrl: string;
  private logoutUrl: string;

  private currentUser = new BehaviorSubject<string | null>(null);
  private endSessionUrl: string | null = null;
  readonly currentUser$: Observable<string | null> = this.currentUser.asObservable();

  constructor(
    private http: HttpClient,
    private csrfTokenStore: CsrfTokenStore,
    config: AppConfigService
  ) {
    const base = `${config.get().apiBaseUrl}`;
    this.sessionUrl = `${base}/api/session`;
    this.loginUrl = `${base}/oauth2/authorization/keycloak`;
    this.logoutUrl = `${base}/logout`;
    this.refreshSession();
  }

  refreshSession(): void {
    this.http.get<SessionInfo>(this.sessionUrl).pipe(take(1)).subscribe({
      next: (session) => {
        this.csrfTokenStore.setToken(session.csrfToken ?? null);
        this.endSessionUrl = session.logoutUrl ?? null;
        this.currentUser.next(session.email);
      },
      error: () => {
        this.csrfTokenStore.setToken(null);
        this.endSessionUrl = null;
        this.currentUser.next(null);
      },
    });
  }

  login(returnPath: string): void {
    const safePath = isSafePostLoginPath(returnPath) ? returnPath : DEFAULT_POST_LOGIN_PATH;
    sessionStorage.setItem(POST_LOGIN_PATH_KEY, safePath as string);
    // rd carries a relative in-app path encoded exactly once; the edge
    // callback decodes once and returns to frontendOrigin + rd.
    const rd = encodeURIComponent(window.location.origin + (safePath as string));
    window.location.assign(`${this.loginUrl}?rd=${rd}`);
  }

  logout(): void {
    this.currentUser.next(null);
    // Full-browser form POST through the edge /logout endpoint (POST-only
    // with CSRF enforcement). rd carries the Keycloak end-session URL with
    // the {id_token} placeholder; the edge substitutes the server-side ID
    // token as id_token_hint so Keycloak ends the SSO session at once with
    // no confirm page. The compose gateway ignores rd and uses its own OIDC
    // handler, which already appends the hint server side.
    const csrfToken = this.csrfTokenStore.getToken();
    this.csrfTokenStore.setToken(null);
    const endSession = this.endSessionUrl;
    this.endSessionUrl = null;
    const inner = endSession && endSession.length > 0
      ? (endSession.includes('id_token_hint=') ? endSession : `${endSession}&id_token_hint={id_token}`)
      : `${window.location.origin}/home`;
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = this.logoutUrl;
    const rdInput = document.createElement('input');
    rdInput.type = 'hidden';
    rdInput.name = 'rd';
    rdInput.value = inner;
    form.appendChild(rdInput);
    if (csrfToken) {
      const csrfInput = document.createElement('input');
      csrfInput.type = 'hidden';
      csrfInput.name = '_csrf';
      csrfInput.value = csrfToken;
      form.appendChild(csrfInput);
    }
    document.body.appendChild(form);
    form.submit();
  }

  consumePostLoginPath(): string | null {
    const path = sessionStorage.getItem(POST_LOGIN_PATH_KEY);
    sessionStorage.removeItem(POST_LOGIN_PATH_KEY);
    return isSafePostLoginPath(path) ? path : null;
  }
}
