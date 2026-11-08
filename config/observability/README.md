# Sky local observability stack

Local-only Prometheus plus Grafana plus Loki plus Alloy. No Tempo, no Pyroscope.
Logs stay plain text in every service; Alloy parses the correlation id.

## Services

| Service | URL | Purpose |
|---|---|---|
| Prometheus | http://localhost:9090 | scrapes `/actuator/prometheus` on all five JVM services every 15s with OAuth2 client_credentials |
| Grafana | http://localhost:3000 (admin/admin) | Sky Overview dashboard, Explore for logs and metrics |
| Loki | http://localhost:3100 | log store, fed by Alloy only |
| Alloy | http://localhost:12345 | tails Docker stdout, parses `correlation_id`, pushes to Loki |

Prometheus scrapes via the compose network (`sky-booking:5555`, `sky-offer:5552`,
`sky-message:5553`, `sky-notify:5554`, `sky-gateway:5777`) on
`/actuator/prometheus` with an OAuth2 client_credentials token for the existing
confidential client `sky-backend` (token URL
`http://keycloak:8080/auth/realms/sky/protocol/openid-connect/token`, secret is
the local-dev default `dev-only-change-in-prod`). Anonymous stays closed by
design: 401 on the four services, 302 to Keycloak login on the gateway, pinned
by `SkySecurityDefaultsTest`, `SecurityPathsTest`,
`NotifySecurityIntegrationTest` and `SecurityConfigOidcProfileTest`. No Java
change, no separate management port, no opened endpoint. The `SkyServiceDown`
and `SkyHighErrorRate` alerts are in `prometheus/alerts/sky-alerts.yaml`.

## Run

```sh
docker compose -f config/docker/docker-compose.yaml up -d prometheus loki alloy grafana
docker compose -f config/docker/docker-compose.yaml ps
```

Grafana provisions Prometheus and Loki plus the Sky Overview dashboard on first
start. No manual datasource setup.

## Verify

Prometheus targets UP:

```sh
curl -s http://localhost:9090/api/v1/targets | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['activeTargets'])"
```

Actuator stays secured on the main port (anonymous fails by design):

```sh
docker exec sky-prometheus wget -S -O- http://sky-booking:5555/actuator/prometheus 2>&1 | head -5
# HTTP/1.1 401
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:5777/actuator/prometheus
# 302
```

Authenticated scrape (each answers OpenMetrics text with `jvm_memory_used_bytes`):

```sh
TOKEN=$(curl -s -X POST http://localhost:5777/auth/realms/sky/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=sky-backend \
  -d client_secret=dev-only-change-in-prod | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")
docker exec -e TOKEN=$TOKEN sky-prometheus sh -c 'wget -qO- --header="Authorization: Bearer $TOKEN" http://sky-booking:5555/actuator/prometheus' | grep -m1 jvm_memory_used_bytes
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:5777/actuator/prometheus | grep -m1 jvm_memory_used_bytes
```

Grafana Explore log query (Loki):

```
{service="sky-booking"} | json | correlation_id != ""
```

Simpler text form, matching what Alloy stores:

```
{service=~"sky-.*"} |= "correlation"
```

Error rate query (Prometheus):

```promql
sum by (application) (rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
sum by (application) (rate(http_server_requests_seconds_count[5m]))
```

Join logs on one request: read `X-Correlation-Id` from any response header,
then filter Loki by that value. The gateway mints the header when absent and
forwards it downstream; the three servlet services read or mint it in
`CorrelationIdFilter` and render it as `[correlationId]` in every line.

## Files

- `prometheus/prometheus.yaml`: scrape jobs for the five JVM services.
- `prometheus/alerts/sky-alerts.yaml`: service-down plus 5xx-rate alerts.
- `loki/loki-config.yaml`: single-tenant filesystem store on port 3100.
- `alloy/alloy-config.alloy`: Docker discovery, correlation-id parse, Loki push.
- `grafana/provisioning/datasources/datasources.yaml`: Prometheus plus Loki.
- `grafana/provisioning/dashboards/dashboards.yaml`: file provider for Sky folder.
- `grafana/dashboards/sky-overview.json`: error rate, request rate, JVM memory, logs.
