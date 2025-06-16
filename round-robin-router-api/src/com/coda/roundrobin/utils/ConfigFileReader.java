package com.coda.roundrobin.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ConfigFileReader {

    public static List<String> loadBackends(String filePath) throws IOException {
        String configFileDir = System.getProperty("user.dir")+"/src/resources/";
        Properties props = new Properties();
        String backends = "";
        try (FileInputStream fis = new FileInputStream(configFileDir+filePath)) {
            props.load(fis);
            backends = props.getProperty("backends");
        }
        if (backends == null || backends.isBlank()) {
            throw new IllegalArgumentException("Missing 'backends' in config file");
        }
        return Arrays.stream(backends.split(","))
                .map(String::trim)
                .toList();
    }
}
