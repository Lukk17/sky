# seed

Local demo dataset: 4 users, 9 offers, 15 bookings, 12 messages.

All seed users share password `local`.

Run (compose or k3d must be running first):

```powershell
$env:TLS_INSECURE = "1"; node seed/seed.mjs
```

```bash
TLS_INSECURE=1 node seed/seed.mjs
```
