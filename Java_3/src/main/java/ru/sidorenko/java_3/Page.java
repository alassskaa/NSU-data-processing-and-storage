package ru.sidorenko.java_3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Page(String message, List<String> successors) {

    public Page {
        if (successors == null) {
            successors = List.of();
        }
    }
}
