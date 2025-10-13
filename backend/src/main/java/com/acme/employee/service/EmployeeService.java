package com.acme.employee.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.acme.employee.dto.EmployeePatchRequest;
import com.acme.employee.dto.EmployeeQueryParams;
import com.acme.employee.dto.EmployeeResponse;
import com.acme.employee.dto.EmployeeUpsertRequest;
import com.acme.employee.dto.PageResponse;
import com.acme.employee.exception.ResourceNotFoundException;
import com.acme.employee.mapper.EmployeeMapper;
import com.acme.employee.model.EmployeeDocument;
import com.acme.employee.repository.EmployeeRepository;
import com.acme.employee.schema.SchemaField;
import com.acme.employee.schema.SchemaFieldType;
import com.acme.employee.schema.SchemaResult;
import com.acme.employee.support.CollectionNameProvider;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EmployeeService {

    private static final int MAX_PAGE_SIZE = 100;
    private final EmployeeRepository repository;
    private final ReactiveMongoTemplate template;
    private final EmployeeMapper mapper;
    private final SchemaDiscoveryService schemaDiscoveryService;
    private final CollectionNameProvider collectionNameProvider;

    public EmployeeService(EmployeeRepository repository,
                           ReactiveMongoTemplate template,
                           EmployeeMapper mapper,
                           SchemaDiscoveryService schemaDiscoveryService,
                           CollectionNameProvider collectionNameProvider) {
        this.repository = repository;
        this.template = template;
        this.mapper = mapper;
        this.schemaDiscoveryService = schemaDiscoveryService;
        this.collectionNameProvider = collectionNameProvider;
    }

    public Mono<PageResponse<EmployeeResponse>> list(EmployeeQueryParams params) {
        int page = Math.max(params.pageOrDefault(), 0);
        int size = Math.min(Math.max(params.sizeOrDefault(), 1), MAX_PAGE_SIZE);

        return schemaDiscoveryService.discover(collectionNameProvider.collectionName(), 0)
                .flatMap(schema -> executePagedQuery(params, page, size, schema));
    }

    private Mono<PageResponse<EmployeeResponse>> executePagedQuery(EmployeeQueryParams params,
                                                                   int page,
                                                                   int size,
                                                                   SchemaResult schema) {
        Query query = buildQuery(params, schema);
        PageRequest pageRequest = buildPageRequest(params, page, size);
        Query pagedQuery = query.with(pageRequest);

        return template.count(query, EmployeeDocument.class, collectionNameProvider.collectionName())
                .flatMap(total -> template.find(pagedQuery, EmployeeDocument.class, collectionNameProvider.collectionName())
                        .map(mapper::toResponse)
                        .collectList()
                        .map(content -> toPageResponse(content, page, size, total)));
    }

    private Query buildQuery(EmployeeQueryParams params, SchemaResult schema) {
        List<Criteria> andCriteria = new ArrayList<>();
        andCriteria.add(new Criteria().orOperator(
                Criteria.where("deleted").is(false),
                Criteria.where("deleted").exists(false)
        ));

        if (StringUtils.hasText(params.searchTerm())) {
            andCriteria.add(buildSearchCriteria(params.searchTerm(), schema));
        }

        params.filters().forEach((key, value) -> buildFilterCriteria(key, value, schema).ifPresent(andCriteria::add));

        Criteria root = new Criteria();
        if (!andCriteria.isEmpty()) {
            root.andOperator(andCriteria.toArray(Criteria[]::new));
        }
        return new Query(root);
    }

    private PageRequest buildPageRequest(EmployeeQueryParams params, int page, int size) {
        Optional<Sort> sort = params.sortFieldOptional()
                .map(field -> Sort.by(resolveSortDirection(params.sortDirectionOptional()), resolveSortField(field)));
        return sort.map(value -> PageRequest.of(page, size, value))
                .orElseGet(() -> PageRequest.of(page, size));
    }

    private Sort.Direction resolveSortDirection(Optional<String> direction) {
        return direction.map(String::toUpperCase)
                .map(Sort.Direction::fromString)
                .orElse(Sort.Direction.ASC);
    }

    private String resolveSortField(String field) {
        return switch (field) {
            case "createdAt", "updatedAt", "deleted" -> field;
            default -> "attributes." + field;
        };
    }

    private Criteria buildSearchCriteria(String searchTerm, SchemaResult schema) {
        String regex = ".*" + Pattern.quote(searchTerm.trim()) + ".*";
        List<Criteria> orCriteria = schema.fields().values().stream()
                .filter(field -> field.type() == SchemaFieldType.STRING || field.type() == SchemaFieldType.UNKNOWN)
                .map(SchemaField::name)
                .map(fieldName -> Criteria.where("attributes." + fieldName).regex(regex, "i"))
                .collect(Collectors.toList());
        if (orCriteria.isEmpty()) {
            return Criteria.where("attributes").exists(true);
        }
        return new Criteria().orOperator(orCriteria);
    }

    private Optional<Criteria> buildFilterCriteria(String key, String rawValue, SchemaResult schema) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(rawValue)) {
            return Optional.empty();
        }
        String operator = "eq";
        String value = rawValue;
        int colonIndex = rawValue.indexOf(':');
        if (colonIndex > 0) {
            operator = rawValue.substring(0, colonIndex).toLowerCase(Locale.ROOT);
            value = rawValue.substring(colonIndex + 1);
        }

        SchemaFieldType fieldType = schema.fields().getOrDefault(key, new SchemaField(key, SchemaFieldType.UNKNOWN, false, true, null)).type();
        Object typedValue = convertValue(value, fieldType);

        return switch (operator) {
            case "eq" -> Optional.of(Criteria.where("attributes." + key).is(typedValue));
            case "contains" -> Optional.of(Criteria.where("attributes." + key)
                    .regex(".*" + Pattern.quote(value) + ".*", "i"));
            case "gt" -> Optional.of(Criteria.where("attributes." + key).gt(typedValue));
            case "lt" -> Optional.of(Criteria.where("attributes." + key).lt(typedValue));
            default -> Optional.empty();
        };
    }

    private Object convertValue(String value, SchemaFieldType fieldType) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        try {
            return switch (fieldType) {
                case NUMBER -> Double.valueOf(value);
                case BOOLEAN -> Boolean.valueOf(value);
                case DATE -> Instant.parse(value);
                default -> value;
            };
        } catch (Exception ex) {
            return value;
        }
    }

    private PageResponse<EmployeeResponse> toPageResponse(List<EmployeeResponse> content,
                                                          int page,
                                                          int size,
                                                          long total) {
        int totalPages = (int) Math.ceil((double) total / size);
        boolean hasNext = page + 1 < totalPages;
        boolean hasPrevious = page > 0;
        return new PageResponse<>(content, total, page, size, totalPages, hasNext, hasPrevious);
    }

    public Mono<EmployeeResponse> get(String id) {
        return repository.findById(id)
                .filter(employee -> !employee.isDeleted())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Employee", id)))
                .map(mapper::toResponse);
    }

    @Transactional
    public Mono<EmployeeResponse> create(EmployeeUpsertRequest request) {
        EmployeeDocument document = mapper.newDocument(request.attributes());
        return repository.save(document)
                .map(mapper::toResponse);
    }

    @Transactional
    public Mono<EmployeeResponse> replace(String id, EmployeeUpsertRequest request) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Employee", id)))
                .flatMap(document -> {
                    mapper.overwriteAttributes(document, request.attributes());
                    document.setDeleted(false);
                    return repository.save(document);
                })
                .map(mapper::toResponse);
    }

    @Transactional
    public Mono<EmployeeResponse> patch(String id, EmployeePatchRequest request) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Employee", id)))
                .flatMap(document -> {
                    mapper.mergeAttributes(document, request.attributes());
                    document.setDeleted(false);
                    return repository.save(document);
                })
                .map(mapper::toResponse);
    }

    @Transactional
    public Mono<Void> delete(String id, boolean softDelete) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Employee", id)))
                .flatMap(document -> {
                    if (softDelete) {
                        document.setDeleted(true);
                        document.setUpdatedAt(Instant.now());
                        return repository.save(document).then();
                    }
                    return repository.delete(document);
                });
    }

    @Transactional
    public Mono<Void> deleteAll() {
        return repository.deleteAll();
    }
}
