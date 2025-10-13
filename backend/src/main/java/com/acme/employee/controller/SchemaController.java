package com.acme.employee.controller;

import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.acme.employee.schema.SchemaResult;
import com.acme.employee.service.SchemaDiscoveryService;
import com.acme.employee.support.CollectionNameProvider;

import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    private final SchemaDiscoveryService schemaDiscoveryService;
    private final CollectionNameProvider collectionNameProvider;

    public SchemaController(SchemaDiscoveryService schemaDiscoveryService,
                            CollectionNameProvider collectionNameProvider) {
        this.schemaDiscoveryService = schemaDiscoveryService;
        this.collectionNameProvider = collectionNameProvider;
    }

    @GetMapping
    public Mono<SchemaResult> schema(@RequestParam(name = "collection", required = false) String collection,
                                     @RequestParam(name = "sampleSize", defaultValue = "0") @Min(0) int sampleSize) {
        String targetCollection = collection != null ? collection : collectionNameProvider.collectionName();
        return schemaDiscoveryService.discover(targetCollection, sampleSize);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> refresh(@RequestParam(name = "collection", required = false) String collection,
                              @RequestParam(name = "sampleSize", defaultValue = "0") @Min(0) int sampleSize) {
        if (collection == null && sampleSize <= 0) {
            return schemaDiscoveryService.refreshAll();
        }
        String targetCollection = collection != null ? collection : collectionNameProvider.collectionName();
        return schemaDiscoveryService.refresh(targetCollection, sampleSize);
    }
}
