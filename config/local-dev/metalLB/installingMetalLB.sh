# use yaml files in project to create metalLB
# other option is using url to install it (or with newer version):
# https://raw.githubusercontent.com/metallb/metallb/v0.12.1/manifests/metallb.yaml
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.13.6/config/manifests/metallb-native.yaml

# secret from openssl random function
# openssl rand -base64 20
# openssl is intalled with git C:\Program Files\Git\usr\bin\openssl.exe
kubectl create secret generic -n metallb-system memberlist --from-literal=secretkey=ALosuRKeNTkWg+4kduJy


# installation with helm:
helm repo add metallb https://metallb.github.io/metallb
# newest version no need to configure
helm install metallb metallb/metallb
# version 0.12.1
helm install metallb metallb/metallb --version 0.12.1
# need configMap