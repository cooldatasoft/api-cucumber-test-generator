package com.cooldatasoft.testing.data;

import lombok.Data;

@Data
public class Environment {

    private String name;

    private String protocol;
    private String host;
    private int port;
}