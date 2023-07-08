kubectl apply -f ../secret.yaml

kubectl apply -f ../mysql/

kubectl apply -f ../service/sky-booking/
kubectl apply -f ../service/sky-notify/
kubectl apply -f ../service/sky-offer/
kubectl apply -f ../service/sky-message/

kubectl apply -f ../api-gateway/ingress/ingress.yaml

# generate auth file with secrets (must be generated in same folder):
# htpasswd -c auth <username>
kubectl create secret generic basic-auth --from-file=../api-gateway/ingress/auth

kubectl apply -f ../api-gateway/oauth2-proxy/oauth2-proxy-deployment.yaml
kubectl apply -f ../api-gateway/oauth2-proxy/oauth2-proxy-service.yaml

