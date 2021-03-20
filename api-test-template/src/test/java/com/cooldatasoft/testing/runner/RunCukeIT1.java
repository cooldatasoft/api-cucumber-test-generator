package com.cooldatasoft.testing.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        plugin = {
                "pretty",
                "html:target/cucumber",
                "json:target/cucumber-report/cucumber1.json",
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter: target/report.html"
        }
        , glue = "com.cooldatasoft.testing.stepdefs.core.scenario1"
        , features = "src/test/resources/features/TestTemplate1.feature"
        , monochrome = true
)
public class RunCukeIT1 extends AbstractTestNGCucumberTests {

}

//first
//@RunWith(Cucumber.class)
//@CucumberOptions(
//        features =  {
//                "src/test/java/com/features/fpos/admin",
//        },
//        glue = {
//                "com.stepdefinitions"
//        },
//        monochrome = true,
//        tags =  {
//                "@fpos_smoke"
//        },
//        plugin = {"pretty",
//                "html:target/cucumber",
//                "json:target/cucumber-report/cucumber1.json",
//                "json:target/cucumber.json",
//                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter: target/report.html"}
//)


//second

//@RunWith(Cucumber.class)
//@CucumberOptions(
//        features =  "src/test/java/demo/features", //feature location
//        glue = { "demo.stepdefinitions", "com.stepdefinitions" }, //step definition , master hooks locations
//        monochrome = true,
//        tags =  {},
//        plugin = {"pretty",
//                "html:target/cucumber",
//                "json:target/cucumber-report/cucumber.json",
//                "json:target/cucumber-report/cucumber2.json",
//                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter: test-output/report.html"}
//)