package com.acme.employee.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import com.acme.employee.support.CollectionNameProvider;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class EmployeeIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(EmployeeIndexInitializer.class);

    private final ReactiveMongoTemplate template;
    private final CollectionNameProvider collectionNameProvider;

    public EmployeeIndexInitializer(ReactiveMongoTemplate template,
                                    CollectionNameProvider collectionNameProvider) {
        this.template = template;
        this.collectionNameProvider = collectionNameProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        Index idIndex = new Index().on("_id", Sort.Direction.ASC).named("employee_id_idx");
        Index emailIndex = new Index().on("attributes.email", Sort.Direction.ASC).named("employee_email_idx");
        Index lastNameIndex = new Index().on("attributes.lastName", Sort.Direction.ASC).named("employee_lastName_idx");

        Mono.when(
                template.indexOps(collectionNameProvider.collectionName()).ensureIndex(idIndex),
                template.indexOps(collectionNameProvider.collectionName()).ensureIndex(emailIndex),
                template.indexOps(collectionNameProvider.collectionName()).ensureIndex(lastNameIndex)
        )
                .doOnSuccess(unused -> log.info("Indexes ensured for collection {}", collectionNameProvider.collectionName()))
                .doOnError(error -> log.warn("Failed to create indexes: {}", error.getMessage(), error))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}
