#!/bin/sh
set -eu
CONFIG_FILE=/usr/share/nginx/html/assets/config/app-config.json
mkdir -p "$(dirname "$CONFIG_FILE")"
API_BASE_URL="${API_BASE_URL:-http://localhost:5777}"
NOTIFY_SOCKET_PATH="${NOTIFY_SOCKET_PATH:-/notifyWebsocket}"
cat > "$CONFIG_FILE" <<EOF
{
  "apiBaseUrl": "${API_BASE_URL}",
  "allOfferPath": "/api/v1/offers",
  "ownedOffersPath": "/api/v1/owner/offers",
  "addOfferPath": "/api/v1/owner/offers",
  "editOfferPath": "/api/v1/owner/offers",
  "deleteOfferPath": "/api/v1/owner/offers/",
  "searchOfferPath": "/api/v1/search",
  "bookings": "/api/v1/user/bookings",
  "addBooking": "/api/v1/bookings",
  "deleteBooking": "/api/v1/bookings/",
  "receivedMessages": "/api/v1/messages/received",
  "sentMessages": "/api/v1/messages/sent",
  "sendMessage": "/api/v1/messages",
  "deleteMessage": "/api/v1/messages/",
  "notifySocketPath": "${NOTIFY_SOCKET_PATH}"
}
EOF
exec nginx -g 'daemon off;'
