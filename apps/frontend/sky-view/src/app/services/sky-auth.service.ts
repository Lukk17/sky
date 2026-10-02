import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';
import {BehaviorSubject, Observable} from 'rxjs';
import {environment} from '../../environments/environment';
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
  private sessionUrl = `${environment.apiBaseUrl}/api/session`;
  private loginUrl = `${environment.apiBaseUrl}/oauth2/authorization/keycloak`;
  private logoutUrl = `${environment.apiBaseUrl}/logout`;

  private currentUser = new BehaviorSubject<string | null>(null);
  private endSessionUrl: string | null = null;
  readonly currentUser$: Observable<string | null> = this.currentUser.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
    private csrfTokenStore: CsrfTokenStore
  ) {
    this.refreshSession();
  }

  refreshSession(): void {
    this.http.get<SessionInfo>(this.sessionUrl).subscribe({
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
    // Clear the edge session first, then route the full browser through the
    // Keycloak end-session endpoint so the SSO session dies too. An XHR POST
    // alone leaves the Keycloak cookie alive and the next login is silent.
    this.http.post(this.logoutUrl, {}).subscribe({
      next: () => this.afterLogout(),
      error: () => this.afterLogout(),
    });
  }

  consumePostLoginPath(): string | null {
    const path = sessionStorage.getItem(POST_LOGIN_PATH_KEY);
    sessionStorage.removeItem(POST_LOGIN_PATH_KEY);
    return isSafePostLoginPath(path) ? path : null;
  }

  private afterLogout(): void {
    this.csrfTokenStore.setToken(null);
    const endSessionUrl = this.endSessionUrl;
    this.endSessionUrl = null;
    if (endSessionUrl) {
      window.location.assign(endSessionUrl);
      return;
    }
    this.router.navigate(['/home']).then();
  }
}
