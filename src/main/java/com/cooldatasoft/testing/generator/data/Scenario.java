package com.cooldatasoft.testing.generator.data;

import lombok.Data;

import java.util.Map;

@Data
public class Scenario {

    private int scenarioNumber;
    private String groupName;
    private String requestMethod;
    private String contextPath;
    private String description;
    private String consumes;
    private String produces;

    private Boolean hasRequestBody;
    private Boolean hasResponseBody;

    private String requestFilePath;
    private String responseFilePath;

    private int responseStatus;
    private boolean ignore;

    private Map<String, String> headers;
    private Map<String, String> pathParams;
    private Map<String, String> queryParams;
}