# config/seed

Local demo dataset: 4 users, 12 offers, 21 bookings, 24 messages.

All seed users share password `local`.

Run (compose or k3d must be running first):

```powershell
$env:TLS_INSECURE = "1"; node config/seed/seed.mjs
```

```bash
TLS_INSECURE=1 node config/seed/seed.mjs
```
