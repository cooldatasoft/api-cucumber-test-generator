package com.cooldatasoft.testing.runner;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.junit.TextReport;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
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
public class RunCukeUI {
}
