import { EMPTY } from 'rxjs';

describe('EMPTY error path message', () => {
  it('EMPTY completes without a value', (done) => {
    let seen = false;
    EMPTY.subscribe({ next: () => (seen = true), complete: () => {
      expect(seen).toBeFalse();
      done();
    } });
  });
});
