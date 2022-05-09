curl --location --request POST 'localhost:8762/register' \
--header 'Content-Type: application/json' \
--data-raw '{
    "email": "admin@admin",
    "password": "admin"
}'

curl --location --request POST 'localhost:8762/register' \
--header 'Content-Type: application/json' \
--data-raw '{
    "email": "test@test",
    "password": "test"
}'