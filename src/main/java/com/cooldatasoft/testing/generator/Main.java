package com.cooldatasoft.testing.generator;

import com.cooldatasoft.testing.generator.data.TestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.cooldatasoft.testing.generator.Constants.*;

public class Main {

    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException {
        new Main().start();
    }

    public String getCreateTimestamp(){
        LocalDateTime currentDateTime = LocalDateTime.now();
        return currentDateTime.format(formatter);
    }
    private void start() throws IOException {

        String basePackage = MAVEN_GROUP_ID.replace("\\.", "/");

        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/base").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/config").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/data").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/runner").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/stepdefs/core").mkdirs();

        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/features/").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/env").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/request").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/response").mkdirs();

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init();



        //pom.xml
        Map<String, Object> map = new HashMap<>();
        map.put("groupId", MAVEN_GROUP_ID);
        map.put("artifactId", MAVEN_ARTIFACT_ID);
        map.put("basePackage", MAVEN_GROUP_ID);
        map.put("createTimestamp", getCreateTimestamp());
        final VelocityContext velocityContext = new VelocityContext();
        map.forEach(velocityContext::put);

        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/pom.xml", "src/main/resources/template/pom.xml.vm");
        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/.gitignore", "src/main/resources/template/.gitignore.vm");


        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/logback.xml",
                "src/main/resources/template/src/test/resources/logback.xml.vm");
        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/klov.properties",
                "src/main/resources/template/src/test/resources/klov.properties.vm");
        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/extent-config.xml",
                "src/main/resources/template/src/test/resources/extent-config.xml.vm");
        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/extent.properties",
                "src/main/resources/template/src/test/resources/extent.properties.vm");


        String testConfigJsonStr = FileUtils.readFileToString(new File(INPUT_TESTS_FILE), "UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        TestConfig testConfig = objectMapper.readValue(testConfigJsonStr, TestConfig.class);


        List<String> runners = new ArrayList<>();
        testConfig.forEach((apiName, api) -> {
            api.getScenarios().forEach(scenario -> {
                String runnerName = WordUtils.capitalize(apiName) + scenario.getScenarioNumber();
                runners.add(runnerName);
            });
        });
        velocityContext.put("runners", runners);
        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/testng.xml",
                "src/main/resources/template/testng.xml.vm");


        testConfig.forEach((apiName, api) -> {
            velocityContext.put("apiName", apiName);
            velocityContext.put("capitalizedApiName", WordUtils.capitalize(apiName));
            velocityContext.put("apiNameLowercase", apiName.toLowerCase());
            velocityContext.put("api", api);


            try {
                //Do not override this file if exists
                if (!new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/stepdefs/api/" + WordUtils.capitalize(apiName) + "Stepdefs.java").exists()) {
                    createFile(velocityEngine, velocityContext,
                            OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/stepdefs/api/" + WordUtils.capitalize(apiName) + "Stepdefs.java",
                            "src/main/resources/template/src/test/java/basePackage/stepdefs/ApiStepdefs.java.vm");
                }
            } catch(Exception e) {
                e.printStackTrace();
            }

            api.getScenarios().forEach(scenario -> {

                int scenarioNumber = scenario.getScenarioNumber();
                String consumes = scenario.getConsumes();
                String produces = scenario.getProduces();

                try {
                    velocityContext.put("scenario", scenario);
                    createFile(velocityEngine, velocityContext,
                            OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/features/" + apiName + scenarioNumber + ".feature",
                            "src/main/resources/template/src/test/resources/features/TestTemplate.feature.vm");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    String requestFile = OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/request/" + apiName + scenarioNumber;
                    if (consumes.contains("json") && scenario.getHasRequestBody()) {
                        if (!new File(requestFile + ".json").exists()) {
                            Files.write(Paths.get(requestFile + ".json"),
                                    "{\n\t\"message\":\"Place your request body here\"\n}".getBytes());
                        }
                    } else if (consumes.contains("xml") && scenario.getHasRequestBody()) {
                        if (!new File(requestFile + ".xml").exists()) {
                            Files.write(Paths.get(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/request/" + apiName + scenarioNumber + ".xml"),
                                    "<xml>\n\t<value>Place your request body here</value>\n</xml>".getBytes());
                        }
                    } else if (!new File(requestFile + ".txt").exists() && scenario.getHasRequestBody()) {
                        Files.write(Paths.get(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/request/" + apiName + scenarioNumber + ".txt"),
                                "Place your request body here".getBytes());
                    } else if (scenario.getHasRequestBody()) {
                        System.err.println("Unknown consumes : " + consumes);
                    }


                    String responseFile = OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/response/" + apiName + scenarioNumber;
                    if (produces.contains("json") && scenario.getHasResponseBody()) {
                        if (!new File(responseFile + ".json").exists()) {
                            Files.write(Paths.get(responseFile + ".json"),
                                    "{\n\t\"message\":\"Place your response body here\"\n}".getBytes());
                        }
                    } else if (produces.contains("xml") && scenario.getHasResponseBody()) {
                        if (!new File(responseFile + ".xml").exists()) {
                            Files.write(Paths.get(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/response/" + apiName + scenarioNumber + ".xml"),
                                    "<xml>\n\t<value>Place your response body here</value>\n</xml>".getBytes());
                        }
                    } else if (!new File(responseFile + ".txt").exists() && scenario.getHasResponseBody()) {
                        Files.write(Paths.get(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/response/" + apiName + scenarioNumber + ".txt"),
                                "Place your response body here".getBytes());
                    } else if (scenario.getHasResponseBody()) {
                        System.err.println("Unknown produces : " + produces);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    velocityContext.put("scenarioNumber", scenarioNumber);
                    createFile(velocityEngine, velocityContext,
                            OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/stepdefs/core/" + apiName.toLowerCase() + scenarioNumber + "/_" + WordUtils.capitalize(apiName) + scenarioNumber + "Stepdefs.java",
                            "src/main/resources/template/src/test/java/basePackage/stepdefs/core/TopLevelApiStepdefs.java.vm");

                    //Do not override this file if exists
                    if (!new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/stepdefs/" + WordUtils.capitalize(apiName) + scenarioNumber + "Stepdefs.java").exists()) {
                        createFile(velocityEngine, velocityContext,
                                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/stepdefs/" + WordUtils.capitalize(apiName) + scenarioNumber + "Stepdefs.java",
                                "src/main/resources/template/src/test/java/basePackage/stepdefs/ScenarioStepdefs.java.vm");
                    }

                    createFile(velocityEngine, velocityContext,
                            OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/runner/RunCukeIT" + WordUtils.capitalize(apiName) + scenarioNumber + ".java",
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

                    createFile(velocityEngine, contextForEnv,
                            OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/env/config-" + environment.getName() + ".properties",
                            "src/main/resources/template/src/test/resources/config/env/config.properties.vm", true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
//        String currentDir = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        FileUtils.copyFile(new File(INPUT_TESTS_FILE), new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/test-config.json"));


        createFile(velocityEngine, velocityContext,
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/base/BaseStepdefs.java",
                "src/main/resources/template/src/test/java/basePackage/base/BaseStepdefs.java.vm");

        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/config/Config.java",
                "src/main/resources/template/src/test/java/basePackage/config/Config.java.vm");

        createFile(velocityEngine, velocityContext,
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/data/Api.java",
                "src/main/resources/template/src/test/java/basePackage/data/Api.java.vm");

        createFile(velocityEngine, velocityContext,
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/data/Environment.java",
                "src/main/resources/template/src/test/java/basePackage/data/Environment.java.vm");

        createFile(velocityEngine, velocityContext,
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/data/Pair.java",
                "src/main/resources/template/src/test/java/basePackage/data/Pair.java.vm");

        createFile(velocityEngine, velocityContext,
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/data/Scenario.java",
                "src/main/resources/template/src/test/java/basePackage/data/Scenario.java.vm");

        createFile(velocityEngine, velocityContext,
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/" + basePackage + "/data/TestConfig.java",
                "src/main/resources/template/src/test/java/basePackage/data/TestConfig.java.vm");

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
    }
}
