# Message service for Sky

port: 5553  
name: sky-message  
api prefix: `/api`  
header user: `x-auth-request-email`

| Endpoint               | HTTP method | Description           | Body                 | 
|------------------------|-------------|-----------------------|----------------------|
| `/`                    | GET         | Home page             |                      | 
| `/home`                | GET         | Home page             |                      | 
| `/messages/sent`       | GET         | Get sent messages     |                      | 
| `/messages/received`   | GET         | Get received messages |                      | 
| `/message`             | POST        | Send message          | Message object       | 
| `/message/{messageId}` | DELETE      | Delete message        | Message ID to delete | 
| ``                     |             |                       |                      | 

Message object:

```json
{
  "text": "",
  "receiverEmail": ""
}
```


