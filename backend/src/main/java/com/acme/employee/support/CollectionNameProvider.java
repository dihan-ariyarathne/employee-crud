package com.acme.employee.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("collectionNameProvider")
public class CollectionNameProvider {

    private final String collectionName;

    public CollectionNameProvider(@Value("${MONGODB_EMP_COLLECTION:employees}") String collectionName) {
        this.collectionName = collectionName;
    }

    public String collectionName() {
        return collectionName;
    }
}

