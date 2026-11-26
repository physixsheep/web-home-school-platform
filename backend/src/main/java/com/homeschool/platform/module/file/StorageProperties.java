package com.homeschool.platform.module.file;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String root = "./storage";
    private long maxSizeBytes = 20 * 1024 * 1024;
    private List<String> allowedExtensions = new ArrayList<>();
}

