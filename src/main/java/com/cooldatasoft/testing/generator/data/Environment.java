package com.cooldatasoft.testing.generator.data;

import lombok.Data;

import java.util.Map;

@Data
public class Environment {

    private String name;

    private String protocol;
    private String host;
    private int port;
    private Map<String, String> props;
}
