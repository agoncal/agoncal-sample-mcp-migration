package org.agoncal.sample.mcp.maven.pomxml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.List;

public class JsonTest {

    public static void main(String[] args) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Properties
        List list = List.of(new PropertyRecord("name", "value"),
            new PropertyRecord("anotherName", "anotherValue"),
            new PropertyRecord("yetAnotherName", "yetAnotherValue"),
            new PropertyRecord("finalName", "finalValue"));

        System.out.println(list);
        System.out.println(objectMapper.writeValueAsString(list));

        // Dependencies
        list = List.of(
            new DependencyRecord("org.example", "example-artifact", "1.0.0", null, "test"),
            new DependencyRecord("com.example", "another-artifact", "2.0.0", "pom", null),
            new DependencyRecord("net.example", "yet-another-artifact", "3.0.0", null, "compile")
        );
        System.out.println(objectMapper.writeValueAsString(list));
    }
}
