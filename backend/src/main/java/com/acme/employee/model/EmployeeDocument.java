package com.acme.employee.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.acme.employee.support.CollectionNameProvider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "#{@collectionNameProvider.collectionName()}")
public class EmployeeDocument {

    @Id
    private String id;

    @Field("attributes")
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    @Builder.Default
    private boolean deleted = false;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // Removed optimistic locking version to simplify updates with dynamically shaped documents

    public void mergeAttributes(Map<String, Object> updates) {
        attributes.putAll(updates);
    }
}
