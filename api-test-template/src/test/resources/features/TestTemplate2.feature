@CustomerApi
Feature: some feature description 2

#  @groupName @customerApi_1 @groupName_1
  Scenario: testing adding customer - some description 2
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



#    ********************************

#Feature: $apiSescription
#
##set( $underscore = "_" )
#
##foreach($scenario in $scenarios)
#
##set ($headersMap = $scenario.headers)
##set ($pathParamsMap = $scenario.pathParams)
##set ($queryParamsMap = $scenario.queryParams)
#
#  @$apiName
#  @$scenario.groupName
#  @$apiName$underscore$scenario.scenarioNumber
#  Scenario: $scenario.description
#    Given "$apiName" is up and running
#    And I prepare scenario number "$scenario.scenarioNumber" in group "$scenario.groupName"
#  #foreach ($mapEntry in $headersMap.entrySet())
#    And request has header with name "$mapEntry.key" and value "$mapEntry.value"
#  #end
#  #foreach ($mapEntry in $pathParamsMap.entrySet())
#    And path param "$mapEntry.key" has value "$mapEntry.value"
#  #end
#  #foreach ($mapEntry in $queryParamsMap.entrySet())
#    And has a query param with name "$mapEntry.key" and value "$mapEntry.value"
#  #end
#    And endpoint consumes "$scenario.consumes"
#    And endpoint produces "$scenario.produces"
#    When I make a "$scenario.requestMethod" request to path "$scenario.contextPath"
#    Then I should get a response with http status code "$scenario.responseStatus"
##end

