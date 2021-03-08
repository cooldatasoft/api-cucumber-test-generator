@CustomerApi
Feature: some description

#  @groupName @customerApi_1 @groupName_1
  Scenario: testing adding customer - some description
    Given "customerApi" is up and running
    And I prepare scenario number "1" in group "application"
    And request has header with name "headerName1" and value "headerValue1"
    And request has header with name "headerName2" and value "headerValue2"
    And path param "path1" has value "value1"
    And path param "path2" has value "value2"
    And has a query param with name "name1" and value "value1"
    And has a query param with name "name2" and value "value2"
    And endpoint consumes "application/json"
    And endpoint produces "application/json"
    When I make a "POST" request to path "/test/abc/{path1}/{path2}"
    Then I should get a response with http status code "200"

