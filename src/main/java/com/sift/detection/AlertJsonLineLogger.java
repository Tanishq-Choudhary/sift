package com.sift.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sift.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class AlertJsonLineLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertJsonLineLogger.class);

    private final Path logFile;
    private final ObjectMapper objectMapper;

    public AlertJsonLineLogger(Path logFile) {
        this.logFile = logFile;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void append(Alert alert) {
        try {
            String line = objectMapper.writeValueAsString(alert) + System.lineSeparator();
            Files.writeString(logFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.warn("Failed to append alert to {}: {}", logFile, e.getMessage());
        }
    }
}