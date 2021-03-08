package com.cooldatasoft.testing.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Config {

    public static final String CONFIG_PATH = "config/env/config-%s.properties";
    private static final Map<String, Properties> CONFIG_MAP = new ConcurrentHashMap<>();

    private Config() {
    }

    public static Properties getConfig() {
        return getConfig("default");
    }

    public static Properties getConfig(String environment) {
        if (!CONFIG_MAP.containsKey(environment)) {
            try {
                String configFileName = String.format(CONFIG_PATH, environment);
                InputStream input = Config.class.getClassLoader().getResourceAsStream(configFileName);
                if (input == null) {
                    throw new RuntimeException(configFileName + " not found!");
                }
                Properties properties = new Properties();
                properties.load(input);
                CONFIG_MAP.put(environment, properties);
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
        return CONFIG_MAP.get(environment);
    }

}
