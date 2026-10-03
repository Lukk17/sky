import {maskDateInput, toIsoDate} from './date-input.util';

describe('date-input util', () => {
  it('masks digits to dd/mm/yyyy', () => {
    expect(maskDateInput('01022026')).toBe('01/02/2026');
  });
  it('converts to ISO or null', () => {
    expect(toIsoDate('01/02/2026')).toBe('2026-02-01');
    expect(toIsoDate('99/99/9999')).toBeNull();
  });
});
