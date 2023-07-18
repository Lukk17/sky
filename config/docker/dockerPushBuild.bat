:: Will work only if script is run from project main directory with:
:: .\config\docker\dockerPushBuild.bat

docker build -t lukk17/sky-booking -f ./sky-booking/docker/Dockerfile .  --no-cache
docker build -t lukk17/sky-message -f ./sky-message/docker/Dockerfile .  --no-cache
docker build -t lukk17/sky-notify -f ./sky-notify/docker/Dockerfile .  --no-cache
docker build -t lukk17/sky-offer -f ./sky-offer/docker/Dockerfile .  --no-cache

docker tag lukk17/sky-booking lukk17/sky-booking:latest
docker tag lukk17/sky-message lukk17/sky-message:latest
docker tag lukk17/sky-notify lukk17/sky-notify:latest
docker tag lukk17/sky-offer lukk17/sky-offer:latest

docker push lukk17/sky-booking:latest
docker push lukk17/sky-message:latest
docker push lukk17/sky-notify:latest
docker push lukk17/sky-offer:latest

docker tag lukk17/sky-booking lukk17/sky-booking:v1.0.0
docker tag lukk17/sky-message lukk17/sky-message:v1.0.0
docker tag lukk17/sky-notify lukk17/sky-notify:v1.0.0
docker tag lukk17/sky-offer lukk17/sky-offer:v1.0.0

docker push lukk17/sky-booking:v1.0.0
docker push lukk17/sky-message:v1.0.0
docker push lukk17/sky-notify:v1.0.0
docker push lukk17/sky-offer:v1.0.0
