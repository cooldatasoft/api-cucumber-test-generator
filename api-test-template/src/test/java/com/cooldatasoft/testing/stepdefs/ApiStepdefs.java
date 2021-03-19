package com.cooldatasoft.testing.stepdefs;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;

/**
 * DO NOT CHANGE THIS FILE or IT WILL GET OVERRIDDEN
 * Create one class per API
 */
@Slf4j
public class ApiStepdefs extends TemplateStepdefs {

    @Given("^\"([^\"]*)\" is up and running$")
    public void _isUpAndRunning(String apiName) {
        beforeInit();
        init();
        afterInit();

        beforeIsUpAndRunning();
        isUpAndRunning(apiName);
        afterIsUpAndRunning();
    }

    @Given("^\"([^\"]*)\" is up and running at \"([^\"]*)\" on port \"([^\"]*)\" over protocol \"([^\"]*)\"$")
    public void _isUpAndRunningAtOnPortOverProtocol(String apiName, String host, int port, String protocol) {
        beforeInit();
        init();
        afterInit();

        beforeIsUpAndRunning();
        isUpAndRunningAtOnPortOverProtocol(apiName, host, port, protocol);
        afterIsUpAndRunning();
    }

    @And("^I prepare scenario number \"([^\"]*)\" in group \"([^\"]*)\"$")
    public void _iPrepareScenarioNumberInGroup(int scenarioNumebr, String groupName) {
        beforeIPrepareScenarioNumberInGroup();
        iPrepareScenarioNumberInGroup(scenarioNumebr, groupName);
        afterIPrepareScenarioNumberInGroup();
    }

    @And("^request has header with name \"([^\"]*)\" and value \"([^\"]*)\"$")
    public void _requestHasHeaderWithNameAndValue(String headerName, String headerValue) {
        beforeRequestHasHeaderWithNameAndValue();
        requestHasHeaderWithNameAndValue(headerName, headerValue);
        afterRequestHasHeaderWithNameAndValue();
    }

    @And("^path param \"([^\"]*)\" has value \"([^\"]*)\"$")
    public void _pathParamHasValue(String pathParamName, String pathParamValue) {
        beforePathParamHasValue();
        pathParamHasValue(pathParamName, pathParamValue);
        afterPathParamHasValue();
    }

    @And("^has a query param with name \"([^\"]*)\" and value \"([^\"]*)\"$")
    public void _hasAQueryParamWithNameAndValue(String queryParamName, String queryParamValue) {
        beforeHasAQueryParamWithNameAndValue();
        hasAQueryParamWithNameAndValue(queryParamName, queryParamValue);
        afterHasAQueryParamWithNameAndValue();
    }

    @And("^endpoint consumes \"([^\"]*)\"$")
    public void _endpointConsumes(String consumes) {
        beforeEndpointConsumes();
        endpointConsumes(consumes);
        afterEndpointConsumes();
    }

    @And("^endpoint produces \"([^\"]*)\"$")
    public void _endpointProduces(String produces) {
        beforeEndpointProduces();
        endpointProduces(produces);
        afterEndpointProduces();
    }

    @When("^I make a \"([^\"]*)\" request to path \"([^\"]*)\"$")
    public void _iMakeARequestToPath(String requestMethod, String contextPath) throws Throwable {
        beforeIMakeARequestToPath();
        iMakeARequestToPath(requestMethod, contextPath);
        afterIMakeARequestToPath();
    }

    @Then("^I should get a response with http status code \"([^\"]*)\"$")
    public void _iShouldGetAResponseWithHttpStatusCode(int responseStatus) {
        beforeIShouldGetAResponseWithHttpStatusCode();
        iShouldGetAResponseWithHttpStatusCode(responseStatus);
        afterIShouldGetAResponseWithHttpStatusCode();
    }
}
