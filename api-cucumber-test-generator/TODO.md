TODO

* scenarios.json might contain the same apiName multiple times. A validation is needed before generation.
* validate input.json before processing
* read paramaters from external properties
* print logs


* aggregate all runners report into 1
* common steps for group of scenarios
* common steps for api of scenarios



Sample input format

```json
{
  "CurrencyApi": {
    "description": "Conversion API",
    "environments": [
      {
        "name": "dev",
        "protocol": "http",
        "host": "data.fixer.io",
        "port": 80
      },
      {
        "name": "ci",
        "protocol": "http",
        "host": "data.fixer.io",
        "port": 80
      }
    ],
    "scenarios": [
      {
        "scenarioNumber": 1,
        "groupName": "application",
        "requestMethod": "GET",
        "contextPath": "/api/convert",
        "description": "testing adding customer",
        "consumes": "application/json",
        "produces": "application/json",
        "queryParams": {
          "access_key": "ACCESS_KEY",
          "from": "GBP",
          "to": "USD",
          "amount": "1"
        },
        "hasRequestBody": "true",
        "hasResponseBody": "true",
        "responseStatus": 200
      }
    ]
  }
}

```