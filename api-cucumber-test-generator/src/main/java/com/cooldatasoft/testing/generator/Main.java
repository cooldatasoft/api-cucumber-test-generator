package com.cooldatasoft.testing.generator;

import com.cooldatasoft.testing.generator.data.TestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.cooldatasoft.testing.generator.Constants.*;

public class Main {


    public static void main(String[] args) throws IOException, URISyntaxException {
        new Main().start();
    }

    private void start() throws IOException, URISyntaxException {


        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/base").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/config").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/data").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/runner").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/stepdefs").mkdirs();

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


        ObjectMapper objectMapper = new ObjectMapper();
        String testConfigJsonStr = IOUtils.resourceToString("/input.json", StandardCharsets.UTF_8);
        TestConfig testConfig = objectMapper.readValue(testConfigJsonStr, TestConfig.class);


        testConfig.forEach((apiName, api) -> {
            velocityContext.put("apiName", apiName);
            velocityContext.put("api", api);
            try {
                createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/features/" + apiName + ".feature",
                        "src/main/resources/template/src/test/resources/features/TestTemplate.feature.vm");
            } catch (IOException e) {
                e.printStackTrace();
            }

            api.getScenarios().forEach(scenario -> {
                try {
                    new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/request/" + apiName +scenario.getScenarioNumber() +".json").createNewFile();
                    new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/response/" + apiName +scenario.getScenarioNumber() +".json").createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            api.getEnvironments().forEach( environment -> {
                try {
                    VelocityContext contextForEnv = new VelocityContext();
                    contextForEnv.put("apiName", apiName);
                    contextForEnv.put("environment", environment);

                    createFile(velocityEngine, contextForEnv, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/env/config-"+environment.getName()+".properties",
                            "src/main/resources/template/src/test/resources/config/env/config.properties.vm", true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
//        String currentDir = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        FileUtils.copyFile(new File("src/main/resources/input.json"), new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/test-config.json"));


        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/base/BaseStepdefs.java",
                "src/main/resources/template/src/test/java/com/cooldatasoft/testing/base/BaseStepdefs.java.vm");
        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/config/Config.java",
                "src/main/resources/template/src/test/java/com/cooldatasoft/testing/config/Config.java.vm");


        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/data/Api.java",
                "src/main/resources/template/src/test/java/com/cooldatasoft/testing/data/Api.java.vm");
        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/data/Environment.java",
                "src/main/resources/template/src/test/java/com/cooldatasoft/testing/data/Environment.java.vm");
        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/data/Pair.java",
                "src/main/resources/template/src/test/java/com/cooldatasoft/testing/data/Pair.java.vm");
        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/data/Scenario.java",
                "src/main/resources/template/src/test/java/com/cooldatasoft/testing/data/Scenario.java.vm");
        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/data/TestConfig.java",
                "src/main/resources/template/src/test/java/com/cooldatasoft/testing/data/TestConfig.java.vm");


        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/runner/RunCukeIT.java",
                "src/main/resources/template/src/test/java/com/cooldatasoft/testing/runner/RunCukeIT.java.vm");
        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/cooldatasoft/testing/stepdefs/ApiStepdefs.java",
                "src/main/resources/template/src/test/java/com/cooldatasoft/testing/stepdefs/ApiStepdefs.java.vm");


//        createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/env/config-dev.properties",
//                "src/main/resources/template/src/test/resources/config/env/config-dev.properties.vm");
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
