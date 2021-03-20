package com.cooldatasoft.testing.generator;

import com.cooldatasoft.testing.generator.data.TestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.cooldatasoft.testing.generator.Constants.*;

public class Main {

    public static void main(String[] args) throws IOException, URISyntaxException {
        new Main().start();
    }

    private void start() throws IOException {

        String basePackage = MAVEN_GROUP_ID.replace("\\.","/");

        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/base").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/config").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/data").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/runner").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/stepdefs/core").mkdirs();

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

        final VelocityContext velocityContext = new VelocityContext();
        map.forEach(velocityContext::put);

        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/pom.xml", "src/main/resources/template/pom.xml.vm");
        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/.gitignore", "src/main/resources/template/.gitignore.vm");


        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/pdf-config1.yaml",
                "src/main/resources/template/src/test/resources/pdf-config1.yaml.vm");
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


        testConfig.forEach((apiName, api) -> {
            velocityContext.put("apiName", apiName);
            velocityContext.put("api", api);
            try {
                createFile(velocityEngine, velocityContext,
                        OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/features/" + apiName + ".feature",
                        "src/main/resources/template/src/test/resources/features/TestTemplate.feature.vm");


                createFile(velocityEngine, velocityContext,
                        OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/stepdefs/core/_"+WordUtils.capitalize(apiName)+"Stepdefs.java",
                        "src/main/resources/template/src/test/java/basePackage/stepdefs/core/ApiStepdefs.java.vm");

                createFile(velocityEngine, velocityContext,
                        OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/stepdefs/"+ WordUtils.capitalize(apiName)+"Stepdefs.java",
                        "src/main/resources/template/src/test/java/basePackage/stepdefs/TemplateStepdefs.java.vm");

            } catch (IOException e) {
                e.printStackTrace();
            }

            api.getScenarios().forEach(scenario -> {
                try {
                    if (scenario.getConsumes().contains("json")) {
                        Files.write(Paths.get(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/request/" + apiName + scenario.getScenarioNumber() + ".json"),
                                "Place your request body here".getBytes());
                    } else if (scenario.getConsumes().contains("xml")) {
                        Files.write(Paths.get(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/request/" + apiName + scenario.getScenarioNumber() + ".xml"),
                                "Place your request body here".getBytes());
                    } else {
                        Files.write(Paths.get(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/request/" + apiName + scenario.getScenarioNumber() + ".txt"),
                                "Place your request body here".getBytes());
                    }


                    if (scenario.getProduces().contains("json")) {
                        Files.write(Paths.get(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/response/" + apiName + scenario.getScenarioNumber() + ".json"),
                                "Place your response body here".getBytes());
                    } else if (scenario.getProduces().contains("xml")) {
                        Files.write(Paths.get(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/response/" + apiName + scenario.getScenarioNumber() + ".xml"),
                                "Place your response body here".getBytes());
                    } else {
                        Files.write(Paths.get(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/response/" + apiName + scenario.getScenarioNumber() + ".txt"),
                                "Place your response body here".getBytes());
                    }
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
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/base/BaseStepdefs.java",
                "src/main/resources/template/src/test/java/basePackage/base/BaseStepdefs.java.vm");

        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/config/Config.java",
                "src/main/resources/template/src/test/java/basePackage/config/Config.java.vm");

        createFile(velocityEngine, velocityContext,
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/data/Api.java",
                "src/main/resources/template/src/test/java/basePackage/data/Api.java.vm");

        createFile(velocityEngine, velocityContext,
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/data/Environment.java",
                "src/main/resources/template/src/test/java/basePackage/data/Environment.java.vm");

        createFile(velocityEngine, velocityContext,
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/data/Pair.java",
                "src/main/resources/template/src/test/java/basePackage/data/Pair.java.vm");

        createFile(velocityEngine, velocityContext,
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/data/Scenario.java",
                "src/main/resources/template/src/test/java/basePackage/data/Scenario.java.vm");

        createFile(velocityEngine, velocityContext,
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/data/TestConfig.java",
                "src/main/resources/template/src/test/java/basePackage/data/TestConfig.java.vm");


        createFile(velocityEngine, velocityContext,
                OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/"+basePackage+"/runner/RunCukeIT.java",
                "src/main/resources/template/src/test/java/basePackage/runner/RunCukeIT.java.vm");
    }

    public void createFile(VelocityEngine velocityEngine, VelocityContext context, String outputFile, String template) throws IOException {
        createFile(velocityEngine, context, outputFile, template, false);
    }

    public void createFile(VelocityEngine velocityEngine, VelocityContext context, String outputFile, String template, boolean append) throws IOException {
        Template t = velocityEngine.getTemplate(template);
        Writer writer = new FileWriter(outputFile, append);
        t.merge(context, writer);
        writer.flush();
        writer.close();
    }
}
