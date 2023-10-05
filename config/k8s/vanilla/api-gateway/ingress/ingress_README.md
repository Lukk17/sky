# Ingress

----------

## Host
When adding host - then in headers, you need to add it in every request.
Without host-all traffic (not only from sky.192.168.59.103.nip.io) will be routed to this service,
depending on the rule below.

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

## TLS setup

In ingress to use certificate:
```shell
tls:
- hosts:
    - skycloud.luksarna.com
  secretName: dev-ssl-cert
```
for no tls use:
```shell
tls: []
```

----------

## Class name
use (introduced in 1.18):
```shell
spec:
  ingressClassName: nginx
```
instead of (deprecated from Kubernetes v1.22+):
```shell
annotations:
    kubernetes.io/ingress.class: "nginx"
```

----------

## Stripping path

Without rewriting 404 because of a wrong path (need to strip the first part for each service)
```shell
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /$2
```

----------
