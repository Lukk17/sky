# Cluster deployment

How the sky platform gets from a local checkout onto a Kubernetes cluster on GCP, at [https://skycloud.luksarna.com](https://skycloud.luksarna.com).

This document owns the deployment path: images, cluster creation, sealed secrets, the deployment scripts, logs, and teardown. What each chart contains and which values it reads is in [config/k8s/helm/helm_README.md](../helm/helm_README.md). Operating a cluster that is already running is in [config/k8s/k8s_README.md](../k8s_README.md).

Run every command from the repository root.

---

### Deployment scripts

Six scripts, three operations, in a Linux shell flavour and a Windows batch flavour. They are the intended way to deploy, because they run the charts in dependency order and wait for each dependency before continuing.

| Script | What it does |
|---|---|
| [helm/linux/helm-app-deploy.sh](helm/linux/helm-app-deploy.sh) | First install. Creates the `sealed-secrets` namespace and TLS key, installs the Sealed Secrets controller, applies the three sealed secrets, then installs keycloak, oauth2-proxy, the database PVC, PostgreSQL, floci, Kafka, and the four service charts. Waits for the Keycloak PostgreSQL, Keycloak, the app PostgreSQL, and floci before moving on. |
| [helm/win/helm-app-deploy.bat](helm/win/helm-app-deploy.bat) | The same first install, as a Windows batch file. |
| [helm/linux/helm-app-upgrade.sh](helm/linux/helm-app-upgrade.sh) | `helm upgrade` for every release already installed. Skips the namespace, the TLS key, and the sealed secrets, since those are install-time steps. |
| [helm/win/helm-app-upgrade.bat](helm/win/helm-app-upgrade.bat) | The same upgrade, as a Windows batch file. |
| [helm/linux/helm-app-remove.sh](helm/linux/helm-app-remove.sh) | `helm uninstall` for every release, deletes the three sealed secrets, the `sealed-secrets` namespace, and the Kafka PVC. |
| [helm/win/helm-app-remove.bat](helm/win/helm-app-remove.bat) | The same removal, as a Windows batch file. |

There is no PowerShell script. The `.bat` files are plain `cmd.exe` batch and run from PowerShell as well.

Every script reads one variable, `ENV`, which defaults to `prod`. It selects the `values-<env>.yaml` overlay for the charts that ship one: keycloak, oauth2-proxy, kafka, and the four services. The kafka overlays are intentionally empty of overrides, see [config/k8s/helm/helm_README.md](../helm/helm_README.md); they exist so the `-f values-<env>.yaml` argument resolves uniformly for every chart.

Unix shell:

```bash
./config/k8s/_deployment-scripts/helm/linux/helm-app-deploy.sh
```

Windows:

```bat
.\config\k8s\_deployment-scripts\helm\win\helm-app-deploy.bat
```

Two things they do not do:

- They do not install an ingress controller. Do that first, see step 2 below.
- They always install the Sealed Secrets controller and apply the sealed secrets, including under `ENV=local`. A local k3d cluster has no key material for that, so follow [config/k8s/local_README.md](../local_README.md) instead of running these scripts against it.

One thing to know before running the deploy script: it installs the Kafka chart with its default values, and that chart pins an image tag Docker Hub no longer serves. See the Kafka section of [config/k8s/helm/helm_README.md](../helm/helm_README.md) for the overlay to pass by hand.

---

### 1. Build and publish the images

The release pipeline at [.github/workflows/release.yaml](../../../.github/workflows/release.yaml) does this for you. It is manually triggered and takes the version as an input. It validates that version first, then runs the composite build from [.github/workflows/ci.yaml](../../../.github/workflows/ci.yaml) as a reusable workflow on the same commit, so a failing test stops the release before anything reaches Docker Hub. Only then does it build all five images and push each as `lukk17/<service>:v<version>`, read every tag back from the registry, and pause at the approval gate, a GitHub environment protection rule on the `release` environment that only pauses once that environment carries a required reviewer. After the gate it pins the four service charts to the released version, commits that pin, cuts the GitHub release at that commit, and moves `lukk17/<service>:latest` onto the released version as its last step, with `docker buildx imagetools create` copying the manifest inside the registry instead of building a second time. `:latest` therefore never advances past the gate: rejecting a release leaves it on the previous version. Dispatch it from a branch, never from a tag, because the pin has to commit somewhere. `sky-gateway` is built and pushed with the rest even though no chart deploys it, so the image is available for a Compose stack on any host, and the pin step skips it for the same reason.

To build and push by hand instead, run each build from the repository root because the Dockerfile copies `settings.gradle.kts`, `buildSrc`, and `sky-common` alongside the service. Apply both tags from the one build, the release version and `latest`, which is the pair the pipeline publishes. A published version tag carries a leading `v`, which is what the existing `v1.0.0` and `v1.0.1` images on Docker Hub use, and the next release is `v2.0.0`:

```shell
docker build . -f sky-booking/docker/Dockerfile -t lukk17/sky-booking:v2.0.0 -t lukk17/sky-booking:latest
```

```shell
docker build . -f sky-offer/docker/Dockerfile -t lukk17/sky-offer:v2.0.0 -t lukk17/sky-offer:latest
```

```shell
docker build . -f sky-message/docker/Dockerfile -t lukk17/sky-message:v2.0.0 -t lukk17/sky-message:latest
```

```shell
docker build . -f sky-notify/docker/Dockerfile -t lukk17/sky-notify:v2.0.0 -t lukk17/sky-notify:latest
```

```shell
docker build . -f sky-gateway/docker/Dockerfile -t lukk17/sky-gateway:v2.0.0 -t lukk17/sky-gateway:latest
```

Push the version tag first, check the deployment on it, and only then move `latest`. The pipeline holds that order deliberately, because `:latest` advances only after the approval gate and a rejected release has to leave it on the previous version. Repeat both pushes for `sky-offer`, `sky-message`, `sky-notify`, and `sky-gateway`:

```shell
docker push lukk17/sky-booking:v2.0.0
```

```shell
docker push lukk17/sky-booking:latest
```

The service charts pin an explicit image tag in `deployment.image.tag`, so a chart install pulls that tag rather than `latest`. Bump it by hand only when you built by hand, and carry the same leading `v` the published tag has, because a pin without it names a tag that was never pushed. The deployment template wraps both `deployment.image.repository` and `deployment.image.tag` in Helm's `required`, so a missing or empty value fails the render and names the value, the file, the line and the column, rather than falling back to something the cluster cannot pull. A release through the pipeline moves that pin itself: it rewrites `deployment.image.tag` and the `Chart.yaml` `appVersion` in the four service charts to the released version, commits that, and tags the release at the commit holding the pin. So `helm upgrade` from a checkout of a release tag deploys the image that release built, with no chart editing in between.

---

### 2. Create the GCP cluster

1. Install the [gcloud CLI](https://cloud.google.com/sdk/gcloud) and the GKE auth plugin:

    ```shell
    sudo apt-get install google-cloud-sdk-gke-gcloud-auth-plugin
    ```

2. Log in:

    ```shell
    gcloud auth login
    ```

3. Select the project:

    ```shell
    gcloud config set project sky-app-17
    ```

4. Create the cluster, if it does not exist:

    ```shell
    gcloud container clusters create-auto sky-cluster --location=europe-central2
    ```

5. Fetch credentials, which is what makes `kubectl` and Lens able to talk to it:

    ```shell
    gcloud container clusters get-credentials sky-cluster --location=europe-central2
    ```

6. Install nginx-ingress:

    ```shell
    helm upgrade --install ingress-nginx ingress-nginx --repo https://kubernetes.github.io/ingress-nginx --namespace ingress-nginx --create-namespace
    ```

Sealed Secrets needs to create service accounts, which requires the Kubernetes Engine Admin role on the account doing the deploy. Grant it in the Google Cloud Console under IAM and Admin, IAM, Grant access: enter the account email, pick Kubernetes Engine, then Kubernetes Engine Admin, and save.

---

### 3. Sealed secrets

Sealed Secrets lets the encrypted form of a Kubernetes Secret live in the repository. The controller in the cluster holds the private key and decrypts it into a real Secret at apply time. The private key is never committed: [config/.gitignore](../../.gitignore) excludes `secret/sealed-private.key`, `secret/secrets.yaml`, and `secret/docker-cred.yaml`, and nothing else under `config/`.

#### Install kubeseal

Linux:

```shell
wget https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.22.0/kubeseal-linux-amd64 -O kubeseal
```

```shell
sudo install -m 755 kubeseal /usr/local/bin/kubeseal
```

Windows: download `kubeseal-0.22.0-windows-amd64.tar.gz` or newer from the [sealed-secrets releases page](https://github.com/bitnami-labs/sealed-secrets/releases), extract it, and put the directory holding `kubeseal.exe` on your `PATH`.

#### Create the key pair

Run on Linux or WSL. Keep both files out of the repository and store them in a password manager or a secrets vault.

```shell
openssl req -x509 -days 3650 -nodes -newkey rsa:4096 -keyout sealed-private.key -out sealed-public.crt -subj "/CN=sealed-secret/O=sealed-secret"
```

Run it without `sudo`. Running it as root leaves the private key owned by root with no read permission for your account, and the `kubectl create secret tls` below then fails to open it.

#### Deploy the controller

The controller comes from the vendored chart at [config/k8s/helm/api-gateway/sealed-secrets-controller/](../helm/api-gateway/sealed-secrets-controller/), which is what the deploy script installs. The chart parameter reference is in [config/k8s/helm/helm_README.md](../helm/helm_README.md). Create the namespace first, because GKE does not allow the upstream default of `kube-system`:

```shell
kubectl create namespace sealed-secrets
```

```shell
kubectl create secret tls sealed-secrets-key --cert=./config/k8s/secret/sealed-public.crt --key=./config/k8s/secret/sealed-private.key -n sealed-secrets
```

```shell
helm install sealed-secrets-controller ./config/k8s/helm/api-gateway/sealed-secrets-controller/ -n sealed-secrets --set generatePrivateKey=false --set fullnameOverride=sealed-secrets-controller
```

#### Create new sealed secrets

1. Write the plain Secret to `config/k8s/secret/secrets.yaml`, which is gitignored. The key inventory, and what reads each key, is in [config/k8s/helm/helm_README.md](../helm/helm_README.md).

    ```yaml
    ---
    apiVersion: v1
    kind: Secret
    metadata:
      name: sky-secrets
      namespace: default
    type: Opaque
    stringData:
      postgres-user: "<postgres-user>"
      postgres-password: "<postgres-password>"
      s3-access-key: "<s3-access-key>"
      s3-secret-key: "<s3-secret-key>"
      keycloak-admin: "<keycloak-admin>"
      keycloak-admin-password: "<keycloak-admin-password>"
      keycloak-db-user: "<keycloak-db-user>"
      keycloak-db-password: "<keycloak-db-password>"
      keycloak-client-id: "sky-backend"
      keycloak-client-secret: "<client-secret>"
      keycloak-client-cookie-secret: "<32-byte-random-base64>"
    ```

2. Write the registry credential to `config/k8s/secret/docker-cred.yaml`, also gitignored:

    ```yaml
    ---
    apiVersion: v1
    kind: Secret
    metadata:
      name: docker-cred
    type: kubernetes.io/dockerconfigjson
    stringData:
      docker-server: "<your-registry-server>"
      docker-username: "<your-name>"
      docker-password: "<your-password>"
    ```

3. Seal both. Unix shell:

    ```shell
    kubeseal --format=yaml --cert=config/k8s/secret/sealed-public.crt < config/k8s/secret/secrets.yaml > config/k8s/secret/sealed/sealed-secrets.yaml
    ```

    ```shell
    kubeseal --format=yaml --cert=config/k8s/secret/sealed-public.crt < config/k8s/secret/docker-cred.yaml > config/k8s/secret/sealed/sealed-docker-cred.yaml
    ```

    PowerShell, where `<` is not a redirection operator:

    ```powershell
    Get-Content config\k8s\secret\secrets.yaml | kubeseal --format=yaml --cert=config\k8s\secret\sealed-public.crt | Set-Content config\k8s\secret\sealed\sealed-secrets.yaml
    ```

    ```powershell
    Get-Content config\k8s\secret\docker-cred.yaml | kubeseal --format=yaml --cert=config\k8s\secret\sealed-public.crt | Set-Content config\k8s\secret\sealed\sealed-docker-cred.yaml
    ```

4. Apply the sealed forms, which are the ones committed:

    ```shell
    kubectl apply -f config/k8s/secret/sealed/sealed-secrets.yaml
    ```

    ```shell
    kubectl apply -f config/k8s/secret/sealed/sealed-docker-cred.yaml
    ```

#### Seal the TLS certificate

The development certificate and key at [config/k8s/secret/ssl/](../secret/ssl/) are committed, together with a ready-made Secret manifest and its sealed form. Regenerate the sealed form only if you replace the certificate.

1. Turn the certificate and key into a Secret manifest:

    ```shell
    kubectl create secret tls dev-ssl-cert --cert=config/k8s/secret/ssl/dev-ssl-cert.crt --key=config/k8s/secret/ssl/dev-ssl-cert.key --dry-run=client -o yaml > config/k8s/secret/ssl/dev-ssl-cert.yaml
    ```

2. Seal it. Unix shell:

    ```shell
    kubeseal --format=yaml --cert=config/k8s/secret/sealed-public.crt < config/k8s/secret/ssl/dev-ssl-cert.yaml > config/k8s/secret/sealed/sealed-dev-ssl-cert.yaml
    ```

    PowerShell:

    ```powershell
    Get-Content config\k8s\secret\ssl\dev-ssl-cert.yaml | kubeseal --format=yaml --cert=config\k8s\secret\sealed-public.crt | Set-Content config\k8s\secret\sealed\sealed-dev-ssl-cert.yaml
    ```

3. Apply it:

    ```shell
    kubectl apply -f config/k8s/secret/sealed/sealed-dev-ssl-cert.yaml
    ```

The `tls` block that consumes it is templated by each chart from `ingress.tls.secretName`, which the production overlays point at `sky-tls-cert` and the local overlays at `dev-ssl-cert`. No chart default names either one: the value is empty behind `required`, so a chart rendered with no overlay fails and names it rather than picking an environment's certificate for you.

#### Generate a self-signed certificate

1. Private key:

    ```shell
    openssl genrsa -out config/k8s/secret/ssl/dev-ssl-cert.key 2048
    ```

2. Certificate signing request. Every host the certificate has to serve goes in `subjectAltName`, and the Common Name goes in `-subj` so the command does not stop to prompt. A certificate with no subject alternative name is rejected outright by every current browser and by Go clients, which includes nginx-ingress and oauth2-proxy, so the name list is the part to get right. `CN` is ignored for hostname matching and only the alternative names are read. The list below is the committed development certificate, covering every host a local Ingress serves. Securing a different host means changing both the `CN` and the `subjectAltName` list, not just the `CN`:

    ```shell
    openssl req -new -key config/k8s/secret/ssl/dev-ssl-cert.key -out config/k8s/secret/ssl/dev-ssl-cert.csr -subj "/C=PL/ST=WAW/O=Lukk/CN=localhost" -addext "subjectAltName=DNS:localhost,DNS:keycloak.127.0.0.1.nip.io,IP:127.0.0.1,IP:::1"
    ```

    In Git Bash on Windows, prefix that command with `MSYS_NO_PATHCONV=1`, or MSYS rewrites the leading slash of `-subj` into a Windows path and openssl fails with `subject name is expected to be in the format /type0=value0/...` against something like `C:/Program Files/Git/C=PL/ST=WAW/...`.

3. Self-sign it. `-copy_extensions copyall` is not optional: `openssl x509 -req -signkey` discards the extensions the request asked for unless it is given, so without it this step silently produces a certificate with no subject alternative name even though step 2 put one in the request. That is how the previous certificate ended up unusable:

    ```shell
    openssl x509 -req -days 3655 -copy_extensions copyall -in config/k8s/secret/ssl/dev-ssl-cert.csr -signkey config/k8s/secret/ssl/dev-ssl-cert.key -out config/k8s/secret/ssl/dev-ssl-cert.crt
    ```

4. Confirm the name list survived, because the failure above is silent:

    ```shell
    openssl x509 -in config/k8s/secret/ssl/dev-ssl-cert.crt -noout -subject -dates -ext subjectAltName
    ```

---

### 4. Deploy the stack

Run the deploy script from the top of this document. It covers everything from the Sealed Secrets controller to the four service charts.

To install one chart at a time instead, follow the numbered sequence in [config/k8s/helm/helm_README.md](../helm/helm_README.md).

---

### 5. Logs

Cluster logs land in [Google Cloud Logging](https://console.cloud.google.com/logs). The query that narrows them to this cluster:

```text
resource.type="k8s_container"
resource.labels.project_id="sky-app-17"
resource.labels.location="europe-central2"
resource.labels.cluster_name="sky-cluster"
resource.labels.namespace_name="default"
severity>=DEFAULT
```

For a single pod, `kubectl logs` is faster. See [config/k8s/k8s_README.md](../k8s_README.md).

---

### Changing the application address

When moving the platform from one hostname to another:

1. Update the chart values and the oauth2-proxy redirect URL. The exact keys are listed in [config/k8s/helm/helm_README.md](../helm/helm_README.md).
2. Add the new callback URL to the `sky-backend` client in Keycloak, both in [config/k8s/helm/infra/keycloak/files/sky-realm.json](../helm/infra/keycloak/files/sky-realm.json) and in the running realm through the admin console.
3. Issue a certificate for the new hostname and re-seal it.
4. Update the Bruno `prod` environment at [docs/api/request/environments/prod.yml](../../../docs/api/request/environments/prod.yml).

---

### Tear down

The remove script listed at the top of this document is the whole teardown. To do it by hand, follow the uninstall section of [config/k8s/helm/helm_README.md](../helm/helm_README.md), then remove what lives outside Helm:

```shell
kubectl delete -f config/k8s/secret/sealed --recursive
```

The controller itself is a Helm release, so it goes the same way as the others:

```shell
helm uninstall sealed-secrets-controller -n sealed-secrets
```

---

### Troubleshooting

Fetching the controller's public certificate. You should be sealing with the `sealed-public.crt` you generated, but if you need to pull the certificate the controller is actually using:

```shell
kubeseal --fetch-cert --controller-name=sealed-secrets-controller --controller-namespace=sealed-secrets > config/k8s/secret/sky-sealed-secrets.pem
```

Pods stuck in `CreateContainerConfigError` after a controller reinstall. A reinstalled controller generates a new key unless `generatePrivateKey=false` and the TLS secret are both in place, and every SealedSecret then has to be re-encrypted against the new key.

---

### Docs map

| Document | What it covers |
|---|---|
| [README.md](../../../README.md) | Platform overview, modules, build, ports |
| [config/k8s/helm/helm_README.md](../helm/helm_README.md) | Chart-by-chart reference, secret key inventory, upgrades |
| [config/k8s/k8s_README.md](../k8s_README.md) | Operating a running cluster with kubectl |
| [config/k8s/local_README.md](../local_README.md) | Local Kubernetes cluster on k3d: create, deploy the charts, verify, tear down |
| [config/local-dev/local_README.md](../../local-dev/local_README.md) | Running locally without Kubernetes: Gradle and Docker Compose |
| [config/keycloak/SETUP.md](../../keycloak/SETUP.md) | Keycloak realm, import, certificate trust, users, tokens |
