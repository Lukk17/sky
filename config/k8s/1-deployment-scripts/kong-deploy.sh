# INSTALLING FROM KONG DOCUMENTATION (WITH POSTGRES DB)
# https://docs.konghq.com/gateway/latest/install/kubernetes/helm-quickstart/

kubectl create namespace kong
helm repo add jetstack https://charts.jetstack.io
helm repo add kong https://charts.konghq.com
helm repo update

kubectl create secret generic kong-config-secret -n kong \
    --from-literal=portal_session_conf='{"storage":"kong","secret":"super_secret_salt_string","cookie_name":"portal_session","cookie_samesite":"off","cookie_secure":false}' \
    --from-literal=admin_gui_session_conf='{"storage":"kong","secret":"super_secret_salt_string","cookie_name":"admin_session","cookie_samesite":"off","cookie_secure":false}' \
    --from-literal=pg_host="postgres-service" \
    --from-literal=kong_admin_password=kong \
    --from-literal=password=kong

kubectl create secret generic kong-enterprise-license --from-literal=license="'{}'" -n kong --dry-run=client -o yaml | kubectl apply -f -


helm upgrade --install cert-manager jetstack/cert-manager \
    --set installCRDs=true --namespace cert-manager --create-namespace

bash -c "cat <<EOF | kubectl apply -n kong -f -
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: quickstart-kong-selfsigned-issuer-root
spec:
  selfSigned: {}
---
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: quickstart-kong-selfsigned-issuer-ca
spec:
  commonName: quickstart-kong-selfsigned-issuer-ca
  duration: 2160h0m0s
  isCA: true
  issuerRef:
    group: cert-manager.io
    kind: Issuer
    name: quickstart-kong-selfsigned-issuer-root
  privateKey:
    algorithm: ECDSA
    size: 256
  renewBefore: 360h0m0s
  secretName: quickstart-kong-selfsigned-issuer-ca
---
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: quickstart-kong-selfsigned-issuer
spec:
  ca:
    secretName: quickstart-kong-selfsigned-issuer-ca
EOF"


# helm install quickstart kong/kong --namespace kong --values https://bit.ly/KongGatewayHelmValuesAIO
helm install quickstart kong/kong -n kong -f ../api-gateway/kong/kong-classic/kong-values.yaml
helm upgrade quickstart -n kong kong/kong -f ../api-gateway/kong/kong-classic/custom-values.yaml

# --------------------------------------------------------------------------------------------------------
# CUSTOM RESOURCE DEFINITIONS:

#kubectl apply -f ../api-gateway/kong/kong-customized/kong-custom-resource-definitions.yaml

# --------------------------------------------------------------------------------------------------------

#helm charts:
# https://github.com/Kong/kubernetes-ingress-controller
# https://github.com/Kong/charts/tree/main/charts/kong

# --------------------------------------------------------------------------------------------------------

# bitnami:
# https://bitnami.com/stack/kong/helm
#helm repo add bitnami https://charts.bitnami.com/bitnami
#helm repo update
#helm install kong bitnami/kong
#helm uninstall kong

# --------------------------------------------------------------------------------------------------------

# another option to install
#helm install kong/kong --generate-name --set ingressController.installCRDs=false
#




