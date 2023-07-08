# Offer service for Sky

port: `5552`    
name: `sky-offer`  
api prefix: `/api`  
header user: `X-Forwarded-User`

| Endpoint             | HTTP method | Description      | Body                       | 
|----------------------|-------------|------------------|----------------------------|
| `/`                  | GET         | Home page        |                            | 
| `/home`              | GET         | Home page        |                            | 
| `/offers`            | GET         | Get all offers   |                            | 
| `/owner/offers`      | GET         | Get owned offers |                            | 
| `/owner/offers`      | POST        | Add offer        | Offer object               | 
| `/owner/offers`      | PUT         | Edit offer       | Json with fields to update | 
| `/owner/offers/{id}` | DELETE      | Delete offer     |                            | 
| `/search`            | POST        | Search for query | String with searched value | 

Offer object:

```json
{
  "hotelName": "",
  "description": "",
  "comment": "",
  "price": "",
  "roomCapacity": "",
  "city": "",
  "country": "",
  "photoPath": ""
}
```
