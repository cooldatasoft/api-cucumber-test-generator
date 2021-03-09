package com.cooldatasoft.testing.base;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@Data
@Slf4j
public abstract class BaseStepdefs {

    public static final String CONTENT_TYPE = "Content-Type";
    private final Map<String, String> pathParams = new HashMap<>();
    private final Map<String, String> queryParams = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private String env;
    private Response response;
    private RequestSpecification requestSpecification;
    private String apiName;
    private String groupName;
    private int scenarioNumber;
    private String scenarioDescription;
    private String protocol;
    private String host;
    private int port;
    private String contextPath = "";
    private Method requestMethod;
    private ContentType contentType;
    private String requestBody;
    private Boolean hasRequestBody;
    private Boolean hasResponseBody;
    private String sqlScript;

    protected final void init() {
        this.env = System.getProperty("env");
        if (this.env == null || this.env.trim().length() == 0) {
            throw new RuntimeException("Environment name not provided as paramater. eg. -Denv=dev");
        }
    }

    /**
     * Do not remove the final from method. This method should not be overriden in child classes to make sure the whole
     * framework works as expected.
     */
    protected final void whenRequestIsExecuted() {

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

    /**
     * Do not remove the final from method. This method should not be overriden in child classes to make sure the whole
     * framework works as expected.
     */
    protected final void thenVerifyResponseCode(int code) {
        assertThat(getResponse().getStatusCode(), is(code));
    }

    protected final void thenVerifyResponseContentType(String contentType) {
        assertThat(getResponse().getContentType(), is(contentType));
    }

    /**
     * Do not remove the final from method. This method should not be overriden in child classes to make sure the whole
     * framework works as expected.
     */
    protected final void thenVerifyResponseBody() {
        try {
            if (getHasResponseBody()) {
                String expectedResponse = IOUtils.resourceToString("/config/response/" + getApiName() + getScenarioNumber() + ".json", StandardCharsets.UTF_8);
                JSONAssert.assertEquals(expectedResponse, response.asString(), false);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String readAndSetRequestBodyFromFile() {
        if (getHasRequestBody()) {
            String requestBody = null;
            try {
                requestBody = IOUtils.resourceToString("/config/request/" + getApiName() + getScenarioNumber() + ".json", StandardCharsets.UTF_8);
                setRequestBody(requestBody);
                return requestBody;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    protected final void thenVerifyPartOfResponse(JSONObject jsonObject) {
        try {
            String expectedResponse = IOUtils.resourceToString("/config/response/" + getApiName() + getScenarioNumber() + ".json", StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expectedResponse, jsonObject.toString(), false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
