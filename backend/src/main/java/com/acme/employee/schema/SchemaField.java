package com.acme.employee.schema;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SchemaField(
        String name,
        SchemaFieldType type,
        boolean required,
        boolean nullable,
        SchemaFieldType arrayItemType) {

    public SchemaField withType(SchemaFieldType newType) {
        return new SchemaField(name, newType, required, nullable, arrayItemType);
    }

    public Optional<SchemaFieldType> arrayItemTypeOptional() {
        return Optional.ofNullable(arrayItemType);
    }
}

