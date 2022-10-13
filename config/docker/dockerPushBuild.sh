# Run it manually from terminal - not using IDE
# IMPORTANT: in terminal be in same folder as this file to execute:
# "./dockerPushBuild.sh" - that's why there is cd ../..

# Dockerfiles need to be build from top project (not module) folder
cd ../..

docker build -t lukk17/sky-auth:latest -f ./auth-service/docker/Dockerfile .
docker build -t lukk17/sky-offer:latest -f ./sky-offer/docker/Dockerfile .
docker build -t lukk17/sky-message:latest -f ./sky-message/docker/Dockerfile .
docker build -t lukk17/sky-zuul:latest -f ./zuul-service/docker/Dockerfile .

docker push lukk17/sky-auth:latest
docker push lukk17/sky-offer:latest
docker push lukk17/sky-message:latest
docker push lukk17/sky-zuul:latest

docker scan lukk17/sky-auth
docker scan lukk17/sky-offer
docker scan lukk17/sky-message
docker scan lukk17/sky-zuul