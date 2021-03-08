package com.hsbc.generator;

import lombok.Data;

import java.util.List;

@Data
public class Api {

    private String protocol;
    private String host;
    private int port;
    private List<Scenario> scenarios;
}