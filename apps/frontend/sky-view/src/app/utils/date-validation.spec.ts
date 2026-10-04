import { toIsoDate, isPastDateIso } from './date-input.util';

describe('date validation', () => {
  it('rejects impossible calendar dates', () => {
    expect(toIsoDate('31/02/2026')).toBeNull();
    expect(toIsoDate('30/02/2024')).toBeNull();
    expect(toIsoDate('2026-02-31')).toBeNull();
  });
  it('rejects past dates at caller', () => {
    expect(isPastDateIso('2000-01-01')).toBeTrue();
    expect(isPastDateIso('2999-01-01')).toBeFalse();
  });
});
