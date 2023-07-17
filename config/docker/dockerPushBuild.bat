
docker build -t lukk17/sky-booking -f ./sky-booking/docker/Dockerfile .
docker build -t lukk17/sky-notify -f ./sky-notify/docker/Dockerfile .
docker build -t lukk17/sky-offer -f ./sky-offer/docker/Dockerfile .
docker build -t lukk17/sky-message -f ./sky-message/docker/Dockerfile .

docker tag lukk17/sky-booking lukk17/sky-booking:latest
docker tag lukk17/sky-offer lukk17/sky-offer:latest
docker tag lukk17/sky-notify lukk17/sky-notify:latest
docker tag lukk17/sky-message lukk17/sky-message:latest

docker push lukk17/sky-booking:latest
docker push lukk17/sky-notify:latest
docker push lukk17/sky-offer:latest
docker push lukk17/sky-message:latest

docker tag lukk17/sky-booking lukk17/sky-booking:x03
docker tag lukk17/sky-offer lukk17/sky-offer:x00
docker tag lukk17/sky-notify lukk17/sky-notify:x00
docker tag lukk17/sky-message lukk17/sky-message:x00

docker push lukk17/sky-booking:x03
docker push lukk17/sky-notify:x00
docker push lukk17/sky-offer:x00
docker push lukk17/sky-message:x00
