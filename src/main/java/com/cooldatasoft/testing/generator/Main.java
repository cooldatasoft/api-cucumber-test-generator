package com.cooldatasoft.testing.generator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.cooldatasoft.testing.generator.data.TestConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

//@Slf4j
public class Main {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<String> httpMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH");

    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }


    public static void main(String[] args) throws IOException {
        new Main().start();
    }

    public String getCreateTimestamp() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        return currentDateTime.format(formatter);
    }

    private void start() throws IOException {

        String basePackagePath = Constants.MAVEN_GROUP_ID.replaceAll("\\.", "/");

        createDirectories(basePackagePath);


        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init();


        //pom.xml
        Map<String, Object> map = new HashMap<>();
        map.put("groupId", Constants.MAVEN_GROUP_ID);
        map.put("artifactId", Constants.MAVEN_ARTIFACT_ID);
        map.put("basePackage", Constants.MAVEN_GROUP_ID);
        map.put("createTimestamp", getCreateTimestamp());
        final VelocityContext velocityContext = new VelocityContext();
        map.forEach(velocityContext::put);
        map.forEach((s, o) -> {
            System.out.println(s + "=" + o);
        });

        createFile(velocityEngine, velocityContext, Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/pom.xml", "src/main/resources/template/pom.xml.vm");
        createFile(velocityEngine, velocityContext, Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/.gitignore", "src/main/resources/template/.gitignore.vm");


        createFile(velocityEngine, velocityContext, Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/logback.xml",
                "src/main/resources/template/src/test/resources/logback.xml.vm");
        createFile(velocityEngine, velocityContext, Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/klov.properties",
                "src/main/resources/template/src/test/resources/klov.properties.vm");
        createFile(velocityEngine, velocityContext, Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/extent-config.xml",
                "src/main/resources/template/src/test/resources/extent-config.xml.vm");
        createFile(velocityEngine, velocityContext, Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/extent.properties",
                "src/main/resources/template/src/test/resources/extent.properties.vm");
        createFile(velocityEngine, velocityContext, Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/cucumber.properties",
                "src/main/resources/template/src/test/resources/cucumber.properties.vm");


        TestConfig testConfig = getTestConfig();


        //        String currentDir = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();


        objectMapper.writeValue(new File(Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/config/test-config.json"), testConfig);

        List<String> runners = new ArrayList<>();
        testConfig.forEach((apiName, api) -> {
            api.getScenarios().forEach(scenario -> {
                String runnerName = WordUtils.capitalize(apiName) + scenario.getScenarioNumber();
                runners.add(runnerName);
            });
        });
        velocityContext.put("runners", runners);
        createFile(velocityEngine, velocityContext, Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/testng.xml",
                "src/main/resources/template/testng.xml.vm");

        testConfig.forEach((apiName, api) -> {
            api.getEnvironments().forEach(environment -> {
                new File(Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/config/env/config-" + environment.getName() + ".properties").delete();
            });
        });

        testConfig.forEach((apiName, api) -> {
            velocityContext.put("apiName", apiName);
            velocityContext.put("capitalizedApiName", WordUtils.capitalize(apiName));
            velocityContext.put("apiNameLowercase", apiName.toLowerCase());
            velocityContext.put("api", api);

            try {
                //Do not override this file if exists
                if (!new File(Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/stepdefs/api/" + WordUtils.capitalize(apiName) + "Stepdefs.java").exists()) {
                    createFile(velocityEngine, velocityContext,
                            Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/stepdefs/api/" + WordUtils.capitalize(apiName) + "Stepdefs.java",
                            "src/main/resources/template/src/test/java/basePackage/stepdefs/ApiStepdefs.java.vm");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


            api.getScenarios().forEach(scenario -> {

                int scenarioNumber = scenario.getScenarioNumber();
                String consumes = scenario.getConsumes();
                String produces = scenario.getProduces();

                String requestFile = Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/config/request/" + apiName + scenarioNumber;

                if(StringUtils.isNotBlank(scenario.getRequestBody())) {
                    try {
                        if (consumes.contains("json")) {
                            Files.write(Paths.get(requestFile + ".json"), scenario.getRequestBody().getBytes());
                        } else if (consumes.contains("xml")) {
                            Files.write(Paths.get(requestFile + ".xml"), scenario.getRequestBody().getBytes());
                        } else {
                            Files.write(Paths.get(requestFile + ".txt"), scenario.getRequestBody().getBytes());
                        }
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }

                String responseFile = Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/config/response/" + apiName + scenarioNumber;

                if(StringUtils.isNotBlank(scenario.getResponseBody())) {
                    try {
                        if (produces.contains("json")) {
                            Files.write(Paths.get(responseFile + ".json"), scenario.getResponseBody().getBytes());
                        } else if (produces.contains("xml")) {
                            Files.write(Paths.get(responseFile + ".xml"), scenario.getResponseBody().getBytes());
                        } else {
                            Files.write(Paths.get(responseFile + ".txt"), scenario.getResponseBody().getBytes());
                        }
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    velocityContext.put("scenario", scenario);
                    createFile(velocityEngine, velocityContext,
                            Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/features/" + apiName + scenarioNumber + ".feature",
                            "src/main/resources/template/src/test/resources/features/TestTemplate.feature.vm");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    velocityContext.put("scenarioNumber", scenarioNumber);
                    createFile(velocityEngine, velocityContext,
                            Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/stepdefs/core/" + apiName.toLowerCase() + scenarioNumber + "/_" + WordUtils.capitalize(apiName) + scenarioNumber + "Stepdefs.java",
                            "src/main/resources/template/src/test/java/basePackage/stepdefs/core/TopLevelApiStepdefs.java.vm");

                    //Do not override this file if exists
                    if (!new File(Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/stepdefs/" + WordUtils.capitalize(apiName) + scenarioNumber + "Stepdefs.java").exists()) {
                        createFile(velocityEngine, velocityContext,
                                Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/stepdefs/" + WordUtils.capitalize(apiName) + scenarioNumber + "Stepdefs.java",
                                "src/main/resources/template/src/test/java/basePackage/stepdefs/ScenarioStepdefs.java.vm");
                    }

                    createFile(velocityEngine, velocityContext,
                            Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/runner/RunCukeIT" + WordUtils.capitalize(apiName) + scenarioNumber + ".java",
                            "src/main/resources/template/src/test/java/basePackage/runner/RunCukeIT.java.vm");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });


            api.getEnvironments().forEach(environment -> {
                try {
                    VelocityContext contextForEnv = new VelocityContext();
                    contextForEnv.put("apiName", apiName);
                    contextForEnv.put("environment", environment);
                    contextForEnv.put("createTimestamp", getCreateTimestamp());

                    createFile(velocityEngine, contextForEnv,
                            Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/config/env/config-" + environment.getName() + ".properties",
                            "src/main/resources/template/src/test/resources/config/env/config.properties.vm", true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        });



        createFile(velocityEngine, velocityContext,
                Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/base/BaseStepdefs.java",
                "src/main/resources/template/src/test/java/basePackage/base/BaseStepdefs.java.vm");

        createFile(velocityEngine, velocityContext, Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/config/Config.java",
                "src/main/resources/template/src/test/java/basePackage/config/Config.java.vm");

        createFile(velocityEngine, velocityContext,
                Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/data/Api.java",
                "src/main/resources/template/src/test/java/basePackage/data/Api.java.vm");

        createFile(velocityEngine, velocityContext,
                Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/data/Environment.java",
                "src/main/resources/template/src/test/java/basePackage/data/Environment.java.vm");

        createFile(velocityEngine, velocityContext,
                Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/data/Pair.java",
                "src/main/resources/template/src/test/java/basePackage/data/Pair.java.vm");

        createFile(velocityEngine, velocityContext,
                Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/data/Scenario.java",
                "src/main/resources/template/src/test/java/basePackage/data/Scenario.java.vm");

        createFile(velocityEngine, velocityContext,
                Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/data/TestConfig.java",
                "src/main/resources/template/src/test/java/basePackage/data/TestConfig.java.vm");

        createFile(velocityEngine, velocityContext,
                Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/util/ObjectStore.java",
                "src/main/resources/template/src/test/java/basePackage/util/ObjectStore.java.vm");

    }

    private void createDirectories(String basePackagePath) {
        new File(Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/base").mkdirs();
        new File(Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/config").mkdirs();
        new File(Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/data").mkdirs();
        new File(Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/runner").mkdirs();
        new File(Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackagePath + "/stepdefs/core").mkdirs();

        new File(Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/features/").mkdirs();
        new File(Constants.OUTPUT_PATH + Constants.MAVEN_ARTIFACT_ID + "/src/test/resources/config/env").mkdirs();
        System.out.println("Created directories...");
    }

    private TestConfig getTestConfig() throws IOException {
        String testConfigJsonStr = FileUtils.readFileToString(new File(Constants.INPUT_TESTS_FILE), "UTF-8");

        TestConfig testConfig = objectMapper.readValue(testConfigJsonStr, TestConfig.class);

        List<String> errors = new ArrayList<>();
        //validate
        testConfig.forEach((apiName, api) -> {
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

                if (StringUtils.isBlank(scenario.getProduces())) {
                    errors.add("Scenario produces cannot be blank! ScenarioNumber : " + scenario.getScenarioNumber());
                }

                if (StringUtils.isBlank(scenario.getConsumes())) {
                    errors.add("Scenario consumes cannot be blank! ScenarioNumber : " + scenario.getScenarioNumber());
                }

                if (scenario.getResponseStatus() < 100 && scenario.getResponseStatus() >= 600) {
                    errors.add("Response code must be a valid response code! ScenarioNumber : " + scenario.getScenarioNumber());
                }

                if (scenario.getRequestMethod().equalsIgnoreCase("GET") && StringUtils.isNotBlank(scenario.getRequestBody())) {
                    errors.add("GET request should not have a body! ScenarioNumber : " + scenario.getScenarioNumber());
                }

                //read request/response file content into request/response body fields
                if (StringUtils.isNotBlank(scenario.getRequestFilePath()) && StringUtils.isNotBlank(scenario.getRequestBody())) {
                    errors.add("Either requestBody or requestFilePath should be provided, not both! ScenarioNumber : " + scenario.getScenarioNumber());
                }

                if (StringUtils.isNotBlank(scenario.getResponseFilePath()) && StringUtils.isNotBlank(scenario.getResponseBody())) {
                    errors.add("Either requestBody or requestFilePath should be provided, not both! ScenarioNumber : " + scenario.getScenarioNumber());
                }

                if (StringUtils.isNotBlank(scenario.getRequestFilePath()) && StringUtils.isBlank(scenario.getRequestBody())) {
                    try {
                        File requestFile = new File(scenario.getRequestFilePath());
                        if (requestFile.exists()) {
                            String requestFileContent = FileUtils.readFileToString(requestFile, Charset.forName("UTF-8"));
                            scenario.setRequestBody(requestFileContent);
                            scenario.setRequestFilePath(null);
                        } else {
                            errors.add("Could not find request file: " + scenario.getRequestFilePath() + "\nScenarioNumber : " + scenario.getScenarioNumber());
                        }
                    } catch (IOException e) {
                        errors.add("Failed to read request file content! ScenarioNumber : " + scenario.getScenarioNumber());
                        e.printStackTrace();
                    }
                }

                if (StringUtils.isNotBlank(scenario.getResponseFilePath()) && StringUtils.isBlank(scenario.getResponseBody())) {
                    try {
                        File responseFile = new File(scenario.getResponseFilePath());
                        if (responseFile.exists()) {
                            String responseFileContent = FileUtils.readFileToString(responseFile, Charset.forName("UTF-8"));
                            scenario.setResponseBody(responseFileContent);
                            scenario.setResponseFilePath(null);
                        } else {
                            errors.add("Could not find response file: " + scenario.getResponseFilePath() + "\nScenarioNumber : " + scenario.getScenarioNumber());
                        }
                    } catch (IOException e) {
                        errors.add("Failed to read response file content! ScenarioNumber : " + scenario.getScenarioNumber());
                        e.printStackTrace();
                    }
                }
            });

        });

        if (errors.size() > 0) {
            errors.forEach(System.err::println);
            throw new RuntimeException("Invalid input json file! Please correct above issues with the input json!");
        }

        return testConfig;
    }

    public void createFile(VelocityEngine velocityEngine, VelocityContext context, String outputFile, String template) throws IOException {
        createFile(velocityEngine, context, outputFile, template, false);
    }

    public void createFile(VelocityEngine velocityEngine, VelocityContext context, String outputFile, String template, boolean append) throws IOException {

        File file = new File(outputFile);
        if (!file.exists()) {
            file = file.getParentFile();
            Files.createDirectories(Paths.get(file.getAbsolutePath()));
        }


        Template t = velocityEngine.getTemplate(template);
        Writer writer = new FileWriter(outputFile, append);
        t.merge(context, writer);
        writer.flush();
        writer.close();
        System.out.println("Created : " + outputFile);
    }
}
