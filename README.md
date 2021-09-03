# api-cucumber-test-generator


* If you generate a project and later generate again but changing the API name, you will need to delete the previously generated files manually. Generator will not delete those files.
* If you have multiple APIs in your config and if you remove 1 or more of them and generate again, we will not delete the files that belong to the deleted API. You need to delete them yourself manually.

* You should generate on to the same directory location everytime you re-generate a project. 

Sample input format

```json
{
  "config":{
    "outputPath": "/home/fmucar/workspace/",
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
            "groupNames": ["application", "group1", "group5"],
            "requestMethod": "GET",
            "contextPath": "/api/convert/{pathParamValue1}/{pathParamValue2}",
            "description": "Convert from one currency to another",
            "consumes": "application/json",
            "produces": "application/json",
            "queryParams": {
              "access_key": "ACCESS_KEY",
              "amount": "100"
            },
            "pathParams": {
              "pathParam1":"GBP",
              "pathParam2":"TRY"
            },
            "headers": {
              "headerName1":"headerValue1",
              "headerName2":"headerValue2"
            },
            "hasRequestBody": false,
            "hasResponseBody": true,
            "responseStatus": 200,
            "ignore": false
          }
        ]
      }
  }
}

```


Property | Mandatory | Description 
--- | --- | --- 
config.outputPath| Yes| Destination location path for the generated project.
config.mavenGroupId| Yes| maven groupId for the generated project
config.mavenArtifactId | Yes | maven artifactId ofr the generated project. Also will be used for the root directory name
scenarioNumber | No | Unique integer within the API to order the scenarios. If it is not provided generator will automatically number the scenarios starting from 1.




 

#How to Run

You can run the whoel test suite from command via maven like below

    mvn verify -Dcucumber.options="--tags '@tag1'"
    mvn verify -Dcucumber.options="--tags '@tag1 and @tag2'"
    
    