import { isSafePostLoginPath } from '../services/sky-auth.service';

describe('auth flow', () => {
  it('accepts safe paths and rejects open redirects', () => {
    expect(isSafePostLoginPath('/home')).toBeTrue();
    expect(isSafePostLoginPath('//evil')).toBeFalse();
    expect(isSafePostLoginPath('https://evil')).toBeFalse();
    expect(isSafePostLoginPath(null)).toBeFalse();
  });
});
