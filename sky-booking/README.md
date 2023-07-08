# Offer booking service for Sky

port: 5555  
name: sky-offer-booking  
api prefix: `/api`  
header user: `X-Forwarded-User`

| Endpoint        | HTTP method | Description  | Body           | 
|-----------------|-------------|--------------|----------------|
| `/`             | GET         | Home page    |                | 
| `/home`         | GET         | Home page    |                | 
| `user/bookings` | GET         | Get bookings |                | 
| `bookings`      | POST        | Book offer   | Booking object | 

Booking object:
```json
{
	"offerId": "",
	"dateToBook": ""
}
```
date in format: `YYYY-MM-DD` example: `2023-09-09`
