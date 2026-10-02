import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {environment} from '../../environments/environment';
import {CsrfTokenStore} from './csrf-token.store';

const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export const sessionCookieInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiBaseUrl)) {
    return next(req);
  }

  let outgoing = req.clone({withCredentials: true});
  const xsrfToken = inject(CsrfTokenStore).getToken();
  if (xsrfToken && MUTATING_METHODS.has(req.method.toUpperCase()) && !outgoing.headers.has('X-XSRF-TOKEN')) {
    outgoing = outgoing.clone({setHeaders: {'X-XSRF-TOKEN': xsrfToken}});
  }

  return next(outgoing);
};
