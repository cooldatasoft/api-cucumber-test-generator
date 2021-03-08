package com.cooldatasoft.testing.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = {
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",
                "summary"
        }
        , glue = "com.cooldatasoft.testing"
        , features = "classpath:features/"
        , publish = false
//		,tags = "@all"
)
public class RunCukeIT {

}
