package com.cooldatasoft.testing.base;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Data
@Slf4j
public abstract class BaseStepdefs {

    private String env;
    private String protocol;
    private String host;
    private int port;

    protected final void init() {
        this.env = System.getProperty("env");
        if (this.env == null || this.env.trim().length() == 0) {
            throw new RuntimeException("Environment name not provided as paramater. eg. -Denv=dev");
        }
    }

    /**
     * Do not remove the final from method. This method should not be overriden in child classes to make sure the whole
     * framework works as expected.
     */
    protected final void whenRequestIsExecuted() {

        assertThat(getProtocol(), is(notNullValue()));
        assertThat(getHost(), is(notNullValue()));
        assertThat(getPort(), is(notNullValue()));

        log.info("*****************************************************************************************");
        log.info("Environment : " + getEnv());
        log.info("Protocol : " + getProtocol());
        log.info("Host : " + getHost());
        log.info("Port : " + getPort());
        log.info("*****************************************************************************************");
    }

}
