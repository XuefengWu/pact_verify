pact
---
test rest service by json self.

usage
---
> put json test files in pacts_dir
```
java -jar pact.jar pacts_dir webservice_url
```

Example directory:
project
-pacts
 -test_account
  -create.json
-report

Example json test file:
```
{
    "provider": {
        "name": "Activate Service"
    },
    "consumer": {
        "name": "Account App"
    },
    "interactions": [
        {
            "description": "Activate user with empty request body",
            "request": {
                "method": "post",
                "path": "/users/activate"
            },
            "response": {
                "status": 400,
                "headers": {
                    "Content-Type": "application/json; charset=UTF-8"
                }
        },
        {
            "description": "Activate user with validate request body",
            "request": {
                "method": "post",
                "path": "/users/activate",
                "headers": {
                    "Accept": "application/json"
                },
                "body": {
                    "activationToken": "activation-token"
                }
            },
            "response": {
                "status": 200,
                "headers": {
                    "Content-Type": "application/json; charset=UTF-8"
                }
            }
        }
    ]
}
```

Futures
---

Use response result as next request parameter by $placeholder$
-----
```
{
    "provider": {
        "name": "Activate Service"
    },
    "consumer": {
        "name": "Account App"
    },
    "interactions": [
        {
            "description": "create user",
            "request": {
                "method": "post",
                "path": "/users",
                "body": {"name":"Xuefeng Wu","email":"benewu(at)gmail.com"}
            },
            "response": {
                "status": 200
                "body":{"name":"Xuefeng Wu"}
            }
        },
        {
            "description": "get user",
            "request": {
                "method": "get",
                "path": "/users/$id$",
                "headers": {
                    "Accept": "application/json"
                }
            },
            "response": {
                "status": 200,
                "body":{"name":"Xuefeng Wu"}
            }
        }
    ]
}
```

Assert
----
> Status code
> body fields
