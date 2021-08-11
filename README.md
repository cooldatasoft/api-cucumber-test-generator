# api-cucumber-test-generator




Sample input format

```json
{
  "config":{
    "outputPath": "c:/workspace/",
    "mavenGroupId": "com.cooldatasoft.testing",
    "mavenArtifactId": "some-api-tests"
  },
  "apiMap": {
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
            "pathParams": {
              "pathParam1":"pathParamValue1",
              "pathParam2":"pathParamValue2"
            },
            "headers": {
              "headerName1":"headerValue1",
              "headerName2":"headerValue2"
            },
            "hasRequestBody": true,
            "hasResponseBody": true,
            "responseStatus": 200,
             "ignore": false
          }
        ]
      }
  }
}

```