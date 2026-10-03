export function maskDateInput(value: string): string {
  const digits = (value ?? '').replace(/\D/g, '').slice(0, 8);
  let out = '';
  for (let i = 0; i < digits.length; i++) {
    if (i === 2 || i === 4) out += '/';
    out += digits[i];
  }
  return out;
}

export function toIsoDate(raw: string): string | null {
  const v = (raw ?? '').trim();
  const m = v.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
  if (m) {
    const dd = Number(m[1]);
    const mm = Number(m[2]);
    if (dd < 1 || dd > 31 || mm < 1 || mm > 12) return null;
    return `${m[3]}-${m[2]}-${m[1]}`;
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(v)) return v;
  return null;
}
