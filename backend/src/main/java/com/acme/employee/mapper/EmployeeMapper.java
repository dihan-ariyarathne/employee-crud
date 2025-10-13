package com.acme.employee.mapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.acme.employee.dto.EmployeeResponse;
import com.acme.employee.model.EmployeeDocument;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(EmployeeDocument document) {
        return new EmployeeResponse(
                document.getId(),
                new HashMap<>(document.getAttributes()),
                document.isDeleted(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    public EmployeeDocument newDocument(Map<String, Object> attributes) {
        EmployeeDocument document = new EmployeeDocument();
        document.setAttributes(cleanAttributes(attributes));
        document.setDeleted(false);
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        return document;
    }

    public void overwriteAttributes(EmployeeDocument document, Map<String, Object> attributes) {
        document.setAttributes(cleanAttributes(attributes));
        document.setUpdatedAt(Instant.now());
    }

    public void mergeAttributes(EmployeeDocument document, Map<String, Object> attributes) {
        document.mergeAttributes(cleanAttributes(attributes));
        document.setUpdatedAt(Instant.now());
    }

    private Map<String, Object> cleanAttributes(Map<String, Object> attributes) {
        Map<String, Object> safe = new HashMap<>();
        attributes.forEach((key, value) -> {
            if (key == null) {
                return;
            }
            String trimmedKey = key.trim();
            if (trimmedKey.isEmpty() || trimmedKey.startsWith("_")) {
                return;
            }
            safe.put(trimmedKey, value);
        });
        return safe;
    }
}

