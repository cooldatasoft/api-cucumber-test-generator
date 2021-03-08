package com.hsbc.generator;

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

import static com.hsbc.generator.Constants.MAVEN_ARTIFACT_ID;
import static com.hsbc.generator.Constants.OUTPUT_PATH;

public class Main {


    public static void main(String[] args) throws IOException, URISyntaxException {
        new Main().start();
    }

    private void start() throws IOException, URISyntaxException {

        String currentDir = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();


        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/features/").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/env").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/scenario/request").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/scenario/response").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/base").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/config").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/runners").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/stepDefinitions/base").mkdirs();
        new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/stepDefinitions/data").mkdirs();


        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init();

        //pom.xml
        Map<String, Object> map = new HashMap<>();
        map.put("groupId", MAVEN_ARTIFACT_ID);
        map.put("artifactId", MAVEN_ARTIFACT_ID);


        VelocityContext context = new VelocityContext();
        map.forEach(context::put);

        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/pom.xml", "src/main/resources/template/pom.xml.vm");

        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/testNG.xml", "src/main/resources/template/testNG.xml.vm");

        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/.gitignore", "src/main/resources/template/.gitignore.vm");

        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/Jenkinsfile", "src/main/resources/template/Jenkinsfile.vm");


        ObjectMapper objectMapper = new ObjectMapper();
        String testCasesStr = IOUtils.resourceToString("/scenarios.json", StandardCharsets.UTF_8);
        //FIXME copy scenarios.json to generated project as well.
        TestCases testCases = objectMapper.readValue(testCasesStr, TestCases.class);


        testCases.forEach((apiName, api) -> {
            VelocityContext velocityContext = new VelocityContext();
            velocityContext.put("scenarios", api.getScenarios());
            velocityContext.put("apiName", apiName);
            velocityContext.put("protocol", api.getProtocol());
            velocityContext.put("host", api.getHost());
            velocityContext.put("port", api.getPort());
            //FIXME use groupName as file name
            try {
                createFile(velocityEngine, velocityContext, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/features/" + apiName + ".feature",
                        "src/main/resources/template/src/test/resources/features/TestTemplate.feature.vm");
            } catch (IOException e) {
                e.printStackTrace();
            }

        });


        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/log4j.xml",
                "src/main/resources/template/src/test/resources/config/log4j.xml.vm");

        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/extent-config.xml",
                "src/main/resources/template/src/test/resources/config/extent-config.xml.vm");

        FileUtils.copyFile(new File("src/main/resources/scenarios.json"), new File(OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/scenario/scanarios.json"));


        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/resources/config/env/config-dev.properties",
                "src/main/resources/template/src/test/resources/config/env/config-dev.properties.vm");




        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/base/BaseUtil.java",
                "src/main/resources/template/src/test/java/com/hsbc/base/BaseUtil.java.vm");

        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/config/Config.java",
                "src/main/resources/template/src/test/java/com/hsbc/config/Config.java.vm");

        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/runners/TestRunner.java",
                "src/main/resources/template/src/test/java/com/hsbc/runners/TestRunner.java.vm");

        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/stepDefinitions/Hooks.java",
                "src/main/resources/template/src/test/java/com/hsbc/stepDefinitions/Hooks.java.vm");


//        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/stepDefinitions/base/ApiBaseStepdefs.java",
//                "src/main/resources/template/src/test/java/com/hsbc/stepDefinitions/base/ApiBaseStepdefs.java.vm");

        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/stepDefinitions/base/ApiStepdefs.java",
                "src/main/resources/template/src/test/java/com/hsbc/stepDefinitions/base/ApiStepdefs.java.vm");

        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/stepDefinitions/base/BaseStepdefs.java.vm",
                "src/main/resources/template/src/test/java/com/hsbc/stepDefinitions/base/BaseStepdefs.java.vm.vm");


        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/stepDefinitions/data/Api.java",
                "src/main/resources/template/src/test/java/com/hsbc/stepDefinitions/data/Api.java.vm");
        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/stepDefinitions/data/Pair.java",
                "src/main/resources/template/src/test/java/com/hsbc/stepDefinitions/data/Pair.java.vm");
        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/stepDefinitions/data/Scenario.java",
                "src/main/resources/template/src/test/java/com/hsbc/stepDefinitions/data/Scenario.java.vm");
        createFile(velocityEngine, context, OUTPUT_PATH + MAVEN_ARTIFACT_ID + "/src/test/java/com/hsbc/stepDefinitions/data/TestCases.java",
                "src/main/resources/template/src/test/java/com/hsbc/stepDefinitions/data/TestCases.java.vm");

    }

    public void createFile(VelocityEngine velocityEngine, VelocityContext context, String outputFile, String template) throws IOException {

        Template t = velocityEngine.getTemplate(template);

        Writer writer = new FileWriter(new File(outputFile));
        t.merge(context, writer);
        writer.flush();
        writer.close();
    }
}
