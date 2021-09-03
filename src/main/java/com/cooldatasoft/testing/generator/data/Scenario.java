package com.cooldatasoft.testing.generator.data;

import lombok.Data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
public class Scenario {


    /**
     * Do not use this field as this will be removed in a future version. Instead use List<String> groupNames
     */
    @Deprecated
    private String groupName;


    private int scenarioNumber;
    private Set<String> groupNames= new HashSet<>();
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