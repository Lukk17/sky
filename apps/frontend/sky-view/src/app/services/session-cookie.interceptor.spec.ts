import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';
import {HttpClient, provideHttpClient, withInterceptors} from '@angular/common/http';
import {Router} from '@angular/router';
import {environment} from '../../environments/environment';
import {CsrfTokenStore} from './csrf-token.store';
import {sessionCookieInterceptor} from './session-cookie.interceptor';

describe('sessionCookieInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let router: { navigate: jasmine.Spy };
  let csrfTokenStore: CsrfTokenStore;

  beforeEach(() => {
    router = {navigate: jasmine.createSpy('navigate')};
    csrfTokenStore = new CsrfTokenStore();
    csrfTokenStore.setToken('test-xsrf-token');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([sessionCookieInterceptor])),
        provideHttpClientTesting(),
        {provide: Router, useValue: router},
        {provide: CsrfTokenStore, useValue: csrfTokenStore},
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('sends credentials with api requests', () => {
    http.get(`${environment.apiBaseUrl}/api/v1/offers`).subscribe();

    const request = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/offers`);
    expect(request.request.withCredentials).toBeTrue();
    request.flush([]);
  });

  it('attaches the xsrf token on mutating api requests', () => {
    http.post(`${environment.apiBaseUrl}/api/v1/bookings`, {}).subscribe();

    const request = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/bookings`);
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('test-xsrf-token');
    request.flush({});
  });

  it('leaves non api requests untouched', () => {
    http.get('https://example.com/other').subscribe();

    const request = httpMock.expectOne('https://example.com/other');
    expect(request.request.withCredentials).toBeFalse();
    expect(request.request.headers.has('X-XSRF-TOKEN')).toBeFalse();
    request.flush({});
  });

  it('navigates to auth on 401 from api calls', () => {
    http.get(`${environment.apiBaseUrl}/api/v1/user/bookings`).subscribe({error: () => undefined});

    const request = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/user/bookings`);
    request.flush({}, {status: 401, statusText: 'Unauthorized'});

    expect(router.navigate).toHaveBeenCalledWith(['/auth']);
  });
});
