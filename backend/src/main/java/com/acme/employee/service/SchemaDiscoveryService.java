package com.acme.employee.service;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.acme.employee.config.AppProperties;
import com.acme.employee.schema.SchemaField;
import com.acme.employee.schema.SchemaFieldType;
import com.acme.employee.schema.SchemaResult;

import reactor.core.publisher.Mono;

@Service
public class SchemaDiscoveryService {

    private final ReactiveMongoTemplate template;
    private final AppProperties appProperties;

    public SchemaDiscoveryService(ReactiveMongoTemplate template, AppProperties appProperties) {
        this.template = template;
        this.appProperties = appProperties;
    }

    @Cacheable(cacheNames = "schema", key = "#collection + ':' + #sampleSize")
    public Mono<SchemaResult> discover(String collection, int sampleSize) {
        int effectiveSample = sampleSize > 0 ? sampleSize : appProperties.schema().sampleSize();

        Query query = new Query().limit(effectiveSample);
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("deleted").is(false),
                Criteria.where("deleted").exists(false)
        ));

        return template.find(query, Document.class, collection)
                .collectList()
                .map(documents -> buildSchema(collection, effectiveSample, documents));
    }

    @CacheEvict(cacheNames = "schema", allEntries = true)
    public Mono<Void> refreshAll() {
        return Mono.empty();
    }

    @CacheEvict(cacheNames = "schema", key = "#collection + ':' + #sampleSize")
    public Mono<Void> refresh(String collection, int sampleSize) {
        return Mono.empty();
    }

    private SchemaResult buildSchema(String collection, int sampleSize, List<Document> documents) {
        Map<String, FieldAccumulator> accumulators = new LinkedHashMap<>();
        int documentCount = documents.size();

        for (Document document : documents) {
            Document attributesDoc = document.get("attributes", Document.class);
            if (attributesDoc == null) {
                continue;
            }

            Set<String> keys = attributesDoc.keySet();
            for (String key : keys) {
                Object value = attributesDoc.get(key);
                accumulators
                        .computeIfAbsent(key, FieldAccumulator::new)
                        .observe(value);
            }
            accumulators.values().forEach(acc -> acc.markDocumentProcessed(keys));
        }

        Map<String, SchemaField> fields = new LinkedHashMap<>();
        accumulators.forEach((name, acc) -> fields.put(name, acc.toSchemaField(documentCount)));

        return new SchemaResult(collection, sampleSize, Instant.now(), fields);
    }

    private SchemaFieldType detectType(Object value) {
        if (value == null) {
            return SchemaFieldType.NULL;
        }
        if (value instanceof String || value instanceof ObjectId) {
            return SchemaFieldType.STRING;
        }
        if (value instanceof Number) {
            return SchemaFieldType.NUMBER;
        }
        if (value instanceof Boolean) {
            return SchemaFieldType.BOOLEAN;
        }
        if (value instanceof java.util.Date
                || value instanceof java.time.temporal.TemporalAccessor) {
            return SchemaFieldType.DATE;
        }
        if (value.getClass().isArray() || value instanceof Collection<?>) {
            return SchemaFieldType.ARRAY;
        }
        if (value instanceof Map<?, ?> || value instanceof Document) {
            return SchemaFieldType.OBJECT;
        }
        return SchemaFieldType.UNKNOWN;
    }

    private SchemaFieldType detectArrayItemType(Object value) {
        if (value == null) {
            return null;
        }
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            if (array.length == 0) {
                return SchemaFieldType.UNKNOWN;
            }
            return detectType(array[0]);
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(this::detectType)
                    .orElse(SchemaFieldType.UNKNOWN);
        }
        return null;
    }

    private final class FieldAccumulator {
        private final String name;
        private int occurrences;
        private boolean sawNullValue;
        private SchemaFieldType arrayItemType;
        private final EnumSet<SchemaFieldType> types = EnumSet.noneOf(SchemaFieldType.class);

        private FieldAccumulator(String name) {
            this.name = name;
        }

        private void observe(Object value) {
            occurrences++;
            SchemaFieldType detected = detectType(value);
            if (detected == SchemaFieldType.NULL) {
                sawNullValue = true;
                return;
            }
            types.add(detected);
            if (detected == SchemaFieldType.ARRAY) {
                SchemaFieldType current = detectArrayItemType(value);
                if (arrayItemType == null) {
                    arrayItemType = current;
                } else if (current != null && arrayItemType != current) {
                    arrayItemType = SchemaFieldType.UNKNOWN;
                }
            }
        }

        private void markDocumentProcessed(Set<String> keysInDocument) {
            if (!keysInDocument.contains(name)) {
                sawNullValue = true;
            }
        }

        private SchemaField toSchemaField(int totalDocuments) {
            SchemaFieldType fieldType;
            if (types.isEmpty()) {
                fieldType = SchemaFieldType.NULL;
            } else if (types.size() == 1) {
                fieldType = types.iterator().next();
            } else {
                fieldType = SchemaFieldType.UNKNOWN;
            }

            boolean required = occurrences == totalDocuments && !sawNullValue;
            boolean nullable = sawNullValue || occurrences < totalDocuments;
            return new SchemaField(name, fieldType, required, nullable, arrayItemType);
        }
    }
}
