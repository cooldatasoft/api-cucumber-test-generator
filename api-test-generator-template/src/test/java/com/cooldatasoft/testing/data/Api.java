package com.cooldatasoft.testing.data;

import lombok.Data;

import java.util.List;

@Data
public class Api {

    private String description;
    private List<Environment> environments;
    private List<Scenario> scenarios;
}