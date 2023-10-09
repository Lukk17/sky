# Offer service for Sky

port: `5552`    
name: `sky-offer`  
api prefix: `/api`  
header user: `x-auth-request-email`

| Endpoint                 | HTTP method | Description      | Body                       | 
|--------------------------|-------------|------------------|----------------------------|
| `/`                      | GET         | Home page        |                            | 
| `/home`                  | GET         | Home page        |                            | 
| `/offers`                | GET         | Get all offers   |                            | 
| `/owner/offers`          | GET         | Get owned offers |                            | 
| `/owner/offer`           | POST        | Add offer        | Offer object               | 
| `/owner/offer`           | PUT         | Edit offer       | Json with fields to update | 
| `/owner/offer/{offerId}` | DELETE      | Delete offer     |                            | 
| `/search`                | POST        | Search for query | String with searched value | 

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
