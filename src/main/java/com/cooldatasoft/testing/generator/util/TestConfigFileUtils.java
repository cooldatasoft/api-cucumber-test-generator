package com.cooldatasoft.testing.generator.util;

import com.cooldatasoft.testing.generator.Constants;
import com.cooldatasoft.testing.generator.data.InputJson;
import com.cooldatasoft.testing.generator.data.Scenario;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;


@Slf4j
public class TestConfigFileUtils {

    private static final List<String> httpMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH");

    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public static void writeConfigToFile(InputJson activeConfig) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/resources/config/test-config.json"), activeConfig);
    }


    public static InputJson getTestConfig() throws IOException {
        String testConfigJsonStr = org.apache.commons.io.FileUtils.readFileToString(new File(Constants.INPUT_FILE), "UTF-8");

        InputJson inputJson = objectMapper.readValue(testConfigJsonStr, InputJson.class);

        //adapt the old json format to new format
        //to support multiple groups per scenario
        inputJson.getApiMap().forEach((k, v) -> {
            v.getScenarios().parallelStream().forEach(scenario -> {
                if (StringUtils.isNotBlank(scenario.getGroupName())) {
                    if (!scenario.getGroupNames().contains(scenario.getGroupName())) {
                        scenario.getGroupNames().add(scenario.getGroupName());
                        scenario.setGroupName(null);
                    }
                }

                Set<String> sortedAndSpaceRemovedGroups = scenario.getGroupNames().parallelStream()
                        .map(s -> s.replace(' ', '_'))
                        .sorted()
                        .collect(Collectors.toCollection(HashSet::new));
                scenario.setGroupNames(sortedAndSpaceRemovedGroups);
            });
        });

        //set scenarioNumber if empty
        inputJson.getApiMap().forEach((k, v) -> {
            //verify & set if any scenario is missing scenarioNumber
            AtomicInteger scenarioNumber = new AtomicInteger(1);
            v.getScenarios().parallelStream().forEach(scenario -> {
                if (scenario.getScenarioNumber() == 0) {
                    while (true) {
                        IntPredicate exists = num -> num == scenarioNumber.get();
                        boolean scenarioNumberExists = v.getScenarios().parallelStream().mapToInt(Scenario::getScenarioNumber).anyMatch(exists);

                        if (scenarioNumberExists) {
                            scenarioNumber.incrementAndGet();
                        } else {
                            scenario.setScenarioNumber(scenarioNumber.getAndIncrement());
                            break;
                        }
                    }
                }
            });
            //sort json scenario list first
            v.getScenarios().sort(Comparator.comparingInt(Scenario::getScenarioNumber));
        });

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        //validate
        inputJson.getApiMap().forEach((apiName, api) -> {
            if (!apiName.matches("[a-zA-Z0-9]*")) {
                errors.add("ApiName can only contain letters and number : " + apiName);
            }

            if (api.getEnvironments().size() < 1) {
                errors.add("At least 1 envrionemnt should be defined!");
            }

            api.getEnvironments().forEach(environment -> {
                if (StringUtils.isBlank(environment.getName())) {
                    errors.add("Environment name cannot be blank!");
                }

                if (StringUtils.isBlank(environment.getProtocol())) {
                    errors.add("Environment protocol cannot be blank!");
                }

                if (StringUtils.isBlank(environment.getHost())) {
                    errors.add("Environment host cannot be blank!");
                }

                if (environment.getPort() < 0) {
                    errors.add("Environment port should be a valid number! port : " + environment.getPort());
                }
            });

            if (api.getScenarios().size() == 0) {
                errors.add("At least 1 scenario should be defined!");
            }

            AtomicInteger atomicInteger = new AtomicInteger(1);
            api.getScenarios().forEach(scenario -> {
                if (scenario.getScenarioNumber() == 0) {
                    scenario.setScenarioNumber(atomicInteger.getAndIncrement());
                }

                if (StringUtils.isBlank(scenario.getRequestMethod())) {
                    errors.add("Request method cannot be blank! ScenarioNumber : " + scenario.getScenarioNumber());
                }

                if (!httpMethods.contains(scenario.getRequestMethod().toUpperCase())) {
                    errors.add("Request method must be valid! ScenarioNumber : " + scenario.getScenarioNumber());
                }

                if (StringUtils.isBlank(scenario.getContextPath())) {
                    errors.add("ContextPath cannot be blank! ScenarioNumber : " + scenario.getScenarioNumber());
                }
                if (!scenario.getContextPath().startsWith("/")) {
                    errors.add("ContextPath must start with '/' - ScenarioNumber : " + scenario.getScenarioNumber());
                }

                if (scenario.getHasResponseBody() && StringUtils.isBlank(scenario.getProduces())) {
                    errors.add("Scenario produces cannot be blank! ScenarioNumber : " + scenario.getScenarioNumber());
                }

                if (scenario.getHasRequestBody() && StringUtils.isBlank(scenario.getConsumes()) && !"GET".equalsIgnoreCase(scenario.getRequestMethod())) {
                    warnings.add("Scenario consumes should not be blank! ScenarioNumber : " + scenario.getScenarioNumber());
                }

                if (scenario.getResponseStatus() < 100 && scenario.getResponseStatus() >= 600) {
                    errors.add("Response code must be a valid response code! ScenarioNumber : " + scenario.getScenarioNumber());
                }

                if ("GET".equalsIgnoreCase(scenario.getRequestMethod()) && scenario.getHasRequestBody()) {
                    errors.add("GET request should not have a body! ScenarioNumber : " + scenario.getScenarioNumber());
                }

                if (scenario.getHasRequestBody()) {
                    scenario.setRequestFilePath("/config/request/"+apiName+scenario.getScenarioNumber()+getFileExtension(scenario.getConsumes()));
                }
                if (scenario.getHasResponseBody()) {
                    scenario.setResponseFilePath("/config/response/"+apiName+scenario.getScenarioNumber()+getFileExtension(scenario.getProduces()));
                }
            });
        });

        if (errors.size() > 0) {
            errors.forEach(log::error);
            throw new RuntimeException("Invalid input json file! Please correct above issues with the input json!");
        }

        warnings.forEach(log::warn);

        return inputJson;
    }


    public static String getFileExtension(String mimeType) {
        String extension="";
        if(mimeType!=null) {
            if (mimeType.contains("json")) {
                extension = ".json";
            } else if (mimeType.contains("xml")) {
                extension = ".xml";
            } else {
                extension = ".txt";
            }
        }
        return extension;
    }

}
