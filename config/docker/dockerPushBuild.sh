# Run it manually from terminal - not using IDE
# IMPORTANT: in terminal be in same folder as this file to execute:
# "./dockerPushBuild.sh" - that's why there is cd ../..

# Dockerfiles need to be build from top project (not module) folder
cd ../..

docker build -t lukk17/sky-booking:latest -f ./sky-booking/docker/Dockerfile .
docker build -t lukk17/sky-notify:latest -f ./sky-notify/docker/Dockerfile .
docker build -t lukk17/sky-offer:latest -f ./sky-offer/docker/Dockerfile .
docker build -t lukk17/sky-message:latest -f ./sky-message/docker/Dockerfile .

docker push lukk17/sky-booking:latest
docker push lukk17/sky-notify:latest
docker push lukk17/sky-offer:latest
docker push lukk17/sky-message:latest

docker scan lukk17/sky-booking
docker scan lukk17/sky-notify
docker scan lukk17/sky-offer
docker scan lukk17/sky-message
