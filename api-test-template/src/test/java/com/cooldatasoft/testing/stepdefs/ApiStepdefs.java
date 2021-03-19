package com.cooldatasoft.testing.stepdefs;

import com.cooldatasoft.testing.base.BaseStepdefs;
import com.cooldatasoft.testing.config.Config;
import com.cooldatasoft.testing.data.Api;
import com.cooldatasoft.testing.data.Scenario;
import com.cooldatasoft.testing.data.TestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@Slf4j
public class ApiStepdefs extends BaseStepdefs {

//        Reporter.addScenarioLog("Scenario Log message goes here");
//        Reporter.addStepLog("Step Log message goes here");


    @Given("^\"([^\"]*)\" is up and running$")
    public void isUpAndRunning(String apiName) {
        init();

        String protocol = Config.getConfig(getEnv()).getProperty(apiName + ".protocol");
        String host = Config.getConfig(getEnv()).getProperty(apiName + ".host");
        String port = Config.getConfig(getEnv()).getProperty(apiName + ".port");

        setApiName(apiName);
        setProtocol(protocol);
        setHost(host);
        setPort(Integer.parseInt(port));
    }

    @Given("^\"([^\"]*)\" is up and running at \"([^\"]*)\" on port \"([^\"]*)\" over protocol \"([^\"]*)\"$")
    public void isUpAndRunningAtOnPortOverProtocol(String apiName, String host, int port, String protocol) {
        init();

        setApiName(apiName);
        setProtocol(protocol);
        setHost(host);
        setPort(port);
    }

    @And("^I prepare scenario number \"([^\"]*)\" in group \"([^\"]*)\"$")
    public void iPrepareScenarioNumberInGroup(int scenarioNumebr, String groupName) {
        setScenarioNumber(scenarioNumebr);
        setGroupName(groupName);
    }

    @And("^request has header with name \"([^\"]*)\" and value \"([^\"]*)\"$")
    public void requestHasHeaderWithNameAndValue(String headerName, String headerValue) {
        getHeaders().put(headerName, headerValue);
    }

    @And("^path param \"([^\"]*)\" has value \"([^\"]*)\"$")
    public void pathParamHasValue(String pathParamName, String pathParamValue) {
        getPathParams().put(pathParamName, pathParamValue);
    }

    @And("^has a query param with name \"([^\"]*)\" and value \"([^\"]*)\"$")
    public void hasAQueryParamWithNameAndValue(String queryParamName, String queryParamValue) {
        getQueryParams().put(queryParamName, queryParamValue);
    }

    @And("^endpoint consumes \"([^\"]*)\"$")
    public void endpointConsumes(String consumes) {
        setContentType(ContentType.fromContentType(consumes));
    }

    @And("^endpoint produces \"([^\"]*)\"$")
    public void endpointProduces(String produces) {
//        // Write code here that turns the phrase above into concrete actions
//        throw new PendingException();
    }

    @When("^I make a \"([^\"]*)\" request to path \"([^\"]*)\"$")
    public void iMakeARequestToPath(String requestMethod, String contextPath) throws Throwable {
        setContextPath(contextPath);
        setRequestMethod(Method.valueOf(requestMethod));

        ObjectMapper objectMapper = new ObjectMapper();

        String testCasesStr = IOUtils.resourceToString("/config/test-config.json", StandardCharsets.UTF_8);
        TestConfig apiTestConfig = objectMapper.readValue(testCasesStr, TestConfig.class);

        if (apiTestConfig.containsKey(getApiName())) {
            Api api = apiTestConfig.get(getApiName());

            Optional<Scenario> optionalScenario = api.getScenarios().stream()
                    .filter(scenario -> scenario.getScenarioNumber() == getScenarioNumber())
                    .findFirst();

            Scenario scenario = optionalScenario.orElseThrow(() -> new RuntimeException("Invalid Scenario Number : " + getApiName() + " " + getScenarioNumber()));
            setScenarioDescription(scenario.getDescription());

            setHasRequestBody(scenario.getHasRequestBody());
            setHasResponseBody(scenario.getHasResponseBody());

        } else {
            throw new RuntimeException("Invalid Api name : " + getApiName());
        }


        assertThat(getApiName(), is(notNullValue()));
        assertThat(getProtocol(), is(notNullValue()));
        assertThat(getHost(), is(notNullValue()));
        assertThat(getPort(), is(notNullValue()));
        assertThat(getContentType(), is(notNullValue()));

        RestAssured.baseURI = getProtocol() + "://" + getHost() + ":" + getPort();
        RestAssured.useRelaxedHTTPSValidation();
        setRequestSpecification(RestAssured.given());
        getHeaders().put(CONTENT_TYPE, getContentType().toString());

        getRequestSpecification().headers(getHeaders());
        getRequestSpecification().pathParams(getPathParams());
        getRequestSpecification().queryParams(getQueryParams());

        getRequestSpecification().log().all();

        log.info("******************Scenario " + getScenarioNumber() + "************************************");
        log.info("URL : " + getRequestMethod() + " " + getProtocol() + "://" + getHost() + ":" + getPort() + getContextPath());
        log.info("Request Body: " + getRequestBody());
        log.info("Path Params : " + getPathParams());
        log.info("Query Params : " + getQueryParams());
        log.info("Headers : " + getHeaders());
        log.info("*****************************************************************************************");

        switch (getRequestMethod()) {
            case GET:
                assertThat("GET request cannot have request body!", getRequestBody(), is(nullValue()));
                setResponse(getRequestSpecification().get(getContextPath()));
                break;
            case POST:
                if (getHasRequestBody()) {
                    getRequestSpecification().body(readAndSetRequestBodyFromFile());
                }
                setResponse(getRequestSpecification().post(getContextPath()));
                break;
            case DELETE:
                if (getHasRequestBody()) {
                    getRequestSpecification().body(readAndSetRequestBodyFromFile());
                }
                setResponse(getRequestSpecification().delete(getContextPath()));
                break;
            case PUT:
                if (getHasRequestBody()) {
                    getRequestSpecification().body(readAndSetRequestBodyFromFile());
                }
                setResponse(getRequestSpecification().put(getContextPath()));
                break;
            default:
                throw new RuntimeException("Method not implemented : " + getRequestMethod());
        }

        log.info("*****************************************************************************************");
        log.info("Response Status: " + getResponse().getStatusLine());
        log.info("Response : " + getResponse().asPrettyString());
        log.info("*****************************************************************************************");
    }

    @Then("^I should get a response with http status code \"([^\"]*)\"$")
    public void iShouldGetAResponseWithHttpStatusCode(int responseStatus) {
        thenVerifyResponseCode(responseStatus);
        thenVerifyResponseBody();
    }
}
