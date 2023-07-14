
docker build -t lukk17/sky-booking:latest -f ./sky-booking/docker/Dockerfile .
docker build -t lukk17/sky-notify:latest -f ./sky-notify/docker/Dockerfile .
docker build -t lukk17/sky-offer:latest -f ./sky-offer/docker/Dockerfile .
docker build -t lukk17/sky-message:latest -f ./sky-message/docker/Dockerfile .

docker tag sky-booking:latest lukk17/sky-booking:latest
docker tag sky-offer:latest lukk17/sky-offer:latest
docker tag sky-notify:latest lukk17/sky-notify:latest
docker tag sky-message:latest lukk17/sky-message:latest

docker push lukk17/sky-booking:latest
docker push lukk17/sky-notify:latest
docker push lukk17/sky-offer:latest
docker push lukk17/sky-message:latest

docker scan lukk17/sky-booking
docker scan lukk17/sky-notify
docker scan lukk17/sky-offer
docker scan lukk17/sky-message
