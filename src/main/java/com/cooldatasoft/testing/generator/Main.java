package com.cooldatasoft.testing.generator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.cooldatasoft.testing.data.testng.xsd.ClassType;
import com.cooldatasoft.testing.data.testng.xsd.ObjectFactory;
import com.cooldatasoft.testing.data.testng.xsd.SuiteType;
import com.cooldatasoft.testing.generator.data.InputJson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import javax.xml.bind.*;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

    private void start() throws IOException {

        InputJson activeConfig = getTestConfig();
        String basePackagePath = activeConfig.getConfig().getMavenGroupId().replaceAll("\\.", "/");
        createDirectories(activeConfig, basePackagePath);

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/resources/config/test-config.json"), activeConfig);


        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init();


        //pom.xml
        Map<String, Object> map = new HashMap<>();
        map.put("groupId", activeConfig.getConfig().getMavenGroupId());
        map.put("artifactId", activeConfig.getConfig().getMavenArtifactId());
        map.put("basePackage", activeConfig.getConfig().getMavenGroupId());
        final VelocityContext velocityContext = new VelocityContext();
        map.forEach(velocityContext::put);
        map.forEach((s, o) -> {
            System.out.println(s + "=" + o);
        });

        createFile(velocityEngine, velocityContext, activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/pom.xml", "src/main/resources/template/pom.xml.vm");
        createFile(velocityEngine, velocityContext, activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/.gitignore", "src/main/resources/template/.gitignore.vm");
        createFile(velocityEngine, velocityContext, activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/Jenkinsfile", "src/main/resources/template/Jenkinsfile.vm");


        createFile(velocityEngine, velocityContext, activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/resources/logback.xml",
                "src/main/resources/template/src/test/resources/logback.xml.vm");
        createFile(velocityEngine, velocityContext, activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/resources/extent-config.xml",
                "src/main/resources/template/src/test/resources/extent-config.xml.vm");
        createFile(velocityEngine, velocityContext, activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/resources/extent.properties",
                "src/main/resources/template/src/test/resources/extent.properties.vm");
        createFile(velocityEngine, velocityContext, activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/resources/cucumber.properties",
                "src/main/resources/template/src/test/resources/cucumber.properties.vm");



        //************** TESTNG.xml start **************
        List<String> runnerClassListFromInput = new ArrayList<>();
        activeConfig.getApiMap().forEach( (apiName, api) -> {
            api.getScenarios().forEach(scenario -> {
                String runnerName = WordUtils.capitalize(apiName) + scenario.getScenarioNumber();
                String runnerClass = activeConfig.getConfig().getMavenGroupId() + ".runner.RunCukeIT" + runnerName;
                runnerClassListFromInput.add(runnerClass);
            });
        });

        String testNgOutputFileStr = activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/testng.xml";
        if(new File(testNgOutputFileStr).exists()) {
            String xmlString = FileUtils.readFileToString(new File(testNgOutputFileStr), "UTF-8");
            xmlString = xmlString.replace("<!DOCTYPE suite SYSTEM \"http://testng.org/testng-1.0.dtd\" >", "");

            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
//                unmarshaller.setProperty(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, Boolean.FALSE);
                SuiteType suiteType = ((JAXBElement<SuiteType>) unmarshaller.unmarshal(new StringReader(xmlString))).getValue();
                System.out.println(suiteType);

                List<ClassType> existingClassList = suiteType.getTest().getClasses().getClazz();

                ObjectFactory objectFactory = new ObjectFactory();

                //add new runner classes to testng.xml
                runnerClassListFromInput.forEach( runnerClassFromInput -> {
                    boolean anyMatch = existingClassList.stream().anyMatch(s -> s.getName().equals(runnerClassFromInput));
                    if (!anyMatch) {
                        ClassType classType = objectFactory.createClassType();
                        classType.setName(runnerClassFromInput);
                        classType.setValue("");
                        existingClassList.add(classType);
                    }
                });

                //remove deleted ones
                List<ClassType> toBeRemoved = new ArrayList<>();
                existingClassList.forEach(classType -> {
                    String existingRunnerClass = classType.getName();
                    boolean anyMatch = runnerClassListFromInput.stream().anyMatch(s -> s.equals(existingRunnerClass));
                    if(!anyMatch){
                        toBeRemoved.add(classType);
                    }
                });
                existingClassList.removeAll(toBeRemoved);


//                StringWriter sw = new StringWriter();
//                Marshaller marshaller = jaxbContext.createMarshaller();
//                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
//                marshaller.marshal(objectFactory.createSuite(suiteType), sw);
//                FileUtils.writeStringToFile(new File(testNgOutputFileStr), sw.toString(), Charset.forName("UTF-8"));

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                StringWriter stringWriter = new StringWriter();
                transformer.transform(new JAXBSource(jaxbContext, objectFactory.createSuite(suiteType)), new StreamResult(stringWriter));
                String outputTestNgXml = stringWriter.toString().replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE suite SYSTEM \"http://testng.org/testng-1.0.dtd\" >");
                FileUtils.writeStringToFile(new File(testNgOutputFileStr), outputTestNgXml, Charset.forName("UTF-8"));

            } catch (JAXBException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }
        } else {
            //first time generation
            velocityContext.put("runners", runnerClassListFromInput);
            createFile(velocityEngine, velocityContext, activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/testng.xml",
                    "src/main/resources/template/testng.xml.vm");
        }
        //************** TESTNG.xml end **************


        activeConfig.getApiMap().forEach((apiName, api) -> {
            api.getEnvironments().forEach(environment -> {
                new File(activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/resources/env/config-" + environment.getName() + ".properties").delete();
            });
        });

        activeConfig.getApiMap().forEach((apiName, api) -> {
            velocityContext.put("apiName", apiName);
            velocityContext.put("capitalizedApiName", WordUtils.capitalize(apiName));
            velocityContext.put("apiNameLowercase", apiName.toLowerCase());
            velocityContext.put("api", api);

            try {
                //Do not override this file if exists
                if (!new File(activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/stepdefs/api/" + WordUtils.capitalize(apiName) + "Stepdefs.java").exists()) {
                    createFile(velocityEngine, velocityContext,
                            activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/stepdefs/api/" + WordUtils.capitalize(apiName) + "Stepdefs.java",
                            "src/main/resources/template/src/test/java/basePackage/stepdefs/ApiStepdefs.java.vm");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


            api.getScenarios().forEach(scenario -> {

                int scenarioNumber = scenario.getScenarioNumber();
                String consumes = scenario.getConsumes();
                String produces = scenario.getProduces();

                String requestFilePath = activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/resources/config/request/" + apiName + scenarioNumber;
                System.out.println("Created : "+requestFilePath);
                if(scenario.getHasRequestBody()) {
                    File requestFile = new File(requestFilePath);
                    if (!requestFile.exists()) {
                        String requestFileContent = "";
                        try {
                            Files.createDirectories(Paths.get(requestFilePath).getParent());
                            String extension = getFileExtension(consumes);

                            Files.write(Paths.get(requestFilePath + extension), requestFileContent.getBytes(), StandardOpenOption.CREATE);
                            if (StringUtils.isEmpty(requestFileContent)) {
                                System.err.println("Please fill in the request file for in src/test/resources/config/request/"+ apiName + scenarioNumber + extension);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                String responseFilePath = activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/resources/config/response/" + apiName + scenarioNumber;

                if(scenario.getHasResponseBody()) {
                    File responseFile = new File(requestFilePath);
                    if (!responseFile.exists()) {
                        String responseFileContent = "";
                        try {
                            Files.createDirectories(Paths.get(responseFilePath).getParent());
                            String extension = getFileExtension(produces);
                            Files.write(Paths.get(responseFilePath + extension), responseFileContent.getBytes(), StandardOpenOption.CREATE);

                            if (StringUtils.isEmpty(responseFileContent)) {
                                System.err.println("Please fill in the response file for " + apiName + scenarioNumber + " in src/test/resources/config/response");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    velocityContext.put("scenario", scenario);
                    createFile(velocityEngine, velocityContext,
                            activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/resources/features/" + apiName + scenarioNumber + ".feature",
                            "src/main/resources/template/src/test/resources/features/TestTemplate.feature.vm");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    velocityContext.put("scenarioNumber", scenarioNumber);
                    createFile(velocityEngine, velocityContext,
                            activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/stepdefs/core/" + apiName.toLowerCase() + scenarioNumber + "/_" + WordUtils.capitalize(apiName) + scenarioNumber + "Stepdefs.java",
                            "src/main/resources/template/src/test/java/basePackage/stepdefs/core/TopLevelApiStepdefs.java.vm");

                    //Do not override this file if exists
                    if (!new File(activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/stepdefs/" + WordUtils.capitalize(apiName) + scenarioNumber + "Stepdefs.java").exists()) {
                        createFile(velocityEngine, velocityContext,
                                activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/stepdefs/" + WordUtils.capitalize(apiName) + scenarioNumber + "Stepdefs.java",
                                "src/main/resources/template/src/test/java/basePackage/stepdefs/ScenarioStepdefs.java.vm");
                    }

                    createFile(velocityEngine, velocityContext,
                            activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/runner/RunCukeIT" + WordUtils.capitalize(apiName) + scenarioNumber + ".java",
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
                            activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/resources/env/config-" + environment.getName() + ".properties",
                            "src/main/resources/template/src/test/resources/env/config.properties.vm", true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        });



        createFile(velocityEngine, velocityContext,
                activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/base/BaseStepdefs.java",
                "src/main/resources/template/src/test/java/basePackage/base/BaseStepdefs.java.vm");

        createFile(velocityEngine, velocityContext, activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/config/Config.java",
                "src/main/resources/template/src/test/java/basePackage/config/Config.java.vm");

        createFile(velocityEngine, velocityContext,
                activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/data/Api.java",
                "src/main/resources/template/src/test/java/basePackage/data/Api.java.vm");

        createFile(velocityEngine, velocityContext,
                activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/data/ApiMap.java",
                "src/main/resources/template/src/test/java/basePackage/data/ApiMap.java.vm");

        createFile(velocityEngine, velocityContext,
                activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/data/Config.java",
                "src/main/resources/template/src/test/java/basePackage/data/Config.java.vm");

        createFile(velocityEngine, velocityContext,
                activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/data/Environment.java",
                "src/main/resources/template/src/test/java/basePackage/data/Environment.java.vm");

        createFile(velocityEngine, velocityContext,
                activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/data/Pair.java",
                "src/main/resources/template/src/test/java/basePackage/data/Pair.java.vm");

        createFile(velocityEngine, velocityContext,
                activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/data/Scenario.java",
                "src/main/resources/template/src/test/java/basePackage/data/Scenario.java.vm");

        createFile(velocityEngine, velocityContext,
                activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/data/InputJson.java",
                "src/main/resources/template/src/test/java/basePackage/data/InputJson.java.vm");

        createFile(velocityEngine, velocityContext,
                activeConfig.getConfig().getOutputPath() + activeConfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/util/ObjectStore.java",
                "src/main/resources/template/src/test/java/basePackage/util/ObjectStore.java.vm");

    }

    private String getFileExtension(String mimeType) {
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

    private void createDirectories(InputJson activeCOnfig, String basePackagePath) {
        new File(activeCOnfig.getConfig().getOutputPath() + activeCOnfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/base").mkdirs();
        new File(activeCOnfig.getConfig().getOutputPath() + activeCOnfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/config").mkdirs();
        new File(activeCOnfig.getConfig().getOutputPath() + activeCOnfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/data").mkdirs();
        new File(activeCOnfig.getConfig().getOutputPath() + activeCOnfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/runner").mkdirs();
        new File(activeCOnfig.getConfig().getOutputPath() + activeCOnfig.getConfig().getMavenArtifactId() + "/src/test/java/" + basePackagePath + "/stepdefs/core").mkdirs();

        new File(activeCOnfig.getConfig().getOutputPath() + activeCOnfig.getConfig().getMavenArtifactId() + "/src/test/resources/features/").mkdirs();
        new File(activeCOnfig.getConfig().getOutputPath() + activeCOnfig.getConfig().getMavenArtifactId() + "/src/test/resources/config/").mkdirs();
        System.out.println("Created directories...");
    }

    private InputJson getTestConfig() throws IOException {
        String testConfigJsonStr = FileUtils.readFileToString(new File(Constants.INPUT_FILE), "UTF-8");

        InputJson inputJson = objectMapper.readValue(testConfigJsonStr, InputJson.class);

        List<String> errors = new ArrayList<>();
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
                    errors.add("Scenario consumes cannot be blank! ScenarioNumber : " + scenario.getScenarioNumber());
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
            errors.forEach(System.err::println);
            throw new RuntimeException("Invalid input json file! Please correct above issues with the input json!");
        }

        return inputJson;
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
