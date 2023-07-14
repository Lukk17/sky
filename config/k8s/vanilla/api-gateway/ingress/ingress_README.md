# Ingress

----------

## Host
When adding host - then in headers you need add it in every request,
without host all traffic (not only from sky.192.168.59.103.nip.io) will be routed to this service,
depending on rule below

```shell
spec:
  ingressClassName: nginx
  tls:
    - hosts:
      - sky.<minikubeIP>.nip.io
  rules:
    - host: sky.<minikubeIP>.nip.io
```

----------

## Class name
use:
```shell
  annotations:
    kubernetes.io/ingress.class: "nginx"
```
instead of:
```shell
spec:
  ingressClassName: nginx
```

----------

## Stripping path

Without rewriting 404 because of wrong path (need to strip first part for each service)
```shell
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /$2
```

----------
